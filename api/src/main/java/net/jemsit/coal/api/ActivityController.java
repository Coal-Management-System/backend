package net.jemsit.coal.api;

import jakarta.validation.Valid;
import net.jemsit.coal.api.dto.ActivityDtos.ActivityDto;
import net.jemsit.coal.api.dto.ActivityDtos.VisitRequest;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.api.service.ActivityLog;
import net.jemsit.coal.shared.domain.ActivityKind;
import net.jemsit.coal.shared.domain.UserActivity;
import net.jemsit.coal.shared.repo.UserActivityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * User activity journal. Reading it is for admins only (enforced in SecurityConfig);
 * reporting a page visit is open to every logged-in user.
 */
@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final UserActivityRepository repo;
    private final ActivityLog activityLog;
    /** Date filters are calendar days in the plant's time zone, like party numbers. */
    private final ZoneId zone;

    public ActivityController(UserActivityRepository repo, ActivityLog activityLog,
                              @Value("${coal.numbering.zone:Asia/Tashkent}") String zone) {
        this.repo = repo;
        this.activityLog = activityLog;
        this.zone = ZoneId.of(zone);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<ActivityDto> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) ActivityKind kind,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        var result = repo.search(
                username == null || username.isBlank() ? null : username,
                kind,
                from == null ? null : from.atStartOfDay(zone).toInstant(),
                // "to" is inclusive for the user, so the bound is the start of the next day.
                to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant(),
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 200)));
        return PageResponse.of(result, ActivityController::toDto);
    }

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<String> users() {
        return repo.usernames();
    }

    @PostMapping("/visit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void visit(@Valid @RequestBody VisitRequest req) {
        activityLog.visit(req.path(), req.title());
    }

    private static ActivityDto toDto(UserActivity a) {
        return new ActivityDto(a.getId(), a.getCreatedAt(), a.getUsername(), a.getFullName(),
                a.getKind(), a.getEntityType(), a.getEntityLabel(), a.getSummary());
    }
}
