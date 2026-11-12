package net.jemsit.coal.api;

import jakarta.validation.Valid;
import net.jemsit.coal.api.dto.MovementDtos.MovementCreateRequest;
import net.jemsit.coal.api.dto.MovementDtos.MovementDto;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.api.service.MovementService;
import net.jemsit.coal.api.service.ActivityLog;
import net.jemsit.coal.shared.domain.ActivityKind;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/movements")
public class MovementController {

    private final MovementService service;
    private final ActivityLog activity;

    public MovementController(MovementService service, ActivityLog activity) {
        this.service = service;
        this.activity = activity;
    }

    @GetMapping
    public PageResponse<MovementDto> list(
            @PageableDefault(size = 10, sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    public MovementDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,
                       @RequestHeader(name = "X-Admin-Key", required = false) String adminKey,
                       @RequestParam(required = false) String confirmOperation) {
        MovementDto m = service.get(id);
        service.delete(id, adminKey, confirmOperation);
        activity.record(ActivityKind.DELETE, "MOVEMENT", describe(m), "Удалил движение: " + describe(m));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovementDto create(@Valid @RequestBody MovementCreateRequest req) {
        MovementDto m = service.create(req);
        activity.record(ActivityKind.CREATE, "MOVEMENT", describe(m), "Создал движение: " + describe(m));
        return m;
    }

    /** e.g. "SHIPMENT · партия 051126-001 · штабель П-002 · 40 т". */
    private static String describe(MovementDto m) {
        StringBuilder sb = new StringBuilder(m.operation().name());
        if (m.batchNumber() != null) sb.append(" · партия ").append(m.batchNumber());
        if (m.pileNumber() != null) sb.append(" · штабель ").append(m.pileNumber());
        return sb.append(" · ").append(ActivityLog.tons(m.volume())).toString();
    }
}
