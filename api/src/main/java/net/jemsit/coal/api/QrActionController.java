package net.jemsit.coal.api;

import jakarta.validation.Valid;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.api.dto.QrActionDtos.QrActionCreateRequest;
import net.jemsit.coal.api.dto.QrActionDtos.QrActionDto;
import net.jemsit.coal.api.dto.QrActionDtos.QrActionExecuteRequest;
import net.jemsit.coal.api.dto.QrActionDtos.QrActionScanDto;
import net.jemsit.coal.api.service.QrActionService;
import net.jemsit.coal.api.service.ActivityLog;
import net.jemsit.coal.shared.domain.ActivityKind;
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
    private final ActivityLog activity;

    public QrActionController(QrActionService service, ActivityLog activity) {
        this.service = service;
        this.activity = activity;
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
        QrActionDto a = service.create(req);
        activity.record(ActivityKind.CREATE, "QR_ACTION", a.batchNumber(),
                "Создал QR-задание: партия " + a.batchNumber() + " → склад " + a.warehouseNumber());
        return a;
    }

    @GetMapping("/{id}")
    public QrActionScanDto scan(@PathVariable UUID id) {
        return service.scan(id);
    }

    @PostMapping("/{id}/execute")
    public QrActionDto execute(@PathVariable UUID id, @Valid @RequestBody QrActionExecuteRequest req) {
        QrActionDto a = service.execute(id, req);
        activity.record(ActivityKind.ACTION, "QR_ACTION", a.batchNumber(),
                "Принял по QR партию " + a.batchNumber() + " в штабель " + activity.pileNumber(req.pileId())
                        + " (" + ActivityLog.tons(req.volume()) + ")");
        return a;
    }

    @PostMapping("/{id}/cancel")
    public QrActionDto cancel(@PathVariable UUID id) {
        QrActionDto a = service.cancel(id);
        activity.record(ActivityKind.ACTION, "QR_ACTION", a.batchNumber(),
                "Отменил QR-задание: партия " + a.batchNumber() + " → склад " + a.warehouseNumber());
        return a;
    }
}
