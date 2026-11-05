package net.jemsit.coal.api;

import jakarta.validation.Valid;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.api.dto.QrActionDtos.QrActionCreateRequest;
import net.jemsit.coal.api.dto.QrActionDtos.QrActionDto;
import net.jemsit.coal.api.dto.QrActionDtos.QrActionExecuteRequest;
import net.jemsit.coal.api.dto.QrActionDtos.QrActionScanDto;
import net.jemsit.coal.api.service.QrActionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Deliberately outside the public /api/qr/** path: these change volumes, so they need a login. */
@RestController
@RequestMapping("/api/qr-actions")
public class QrActionController {

    private final QrActionService service;

    public QrActionController(QrActionService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<QrActionDto> list(
            @PageableDefault(size = 10, sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QrActionDto create(@Valid @RequestBody QrActionCreateRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public QrActionScanDto scan(@PathVariable UUID id) {
        return service.scan(id);
    }

    @PostMapping("/{id}/execute")
    public QrActionDto execute(@PathVariable UUID id, @Valid @RequestBody QrActionExecuteRequest req) {
        return service.execute(id, req);
    }

    @PostMapping("/{id}/cancel")
    public QrActionDto cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }
}
