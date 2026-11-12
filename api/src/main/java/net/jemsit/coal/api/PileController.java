package net.jemsit.coal.api;

import jakarta.validation.Valid;
import net.jemsit.coal.api.dto.PileDtos.PileAcceptBatchRequest;
import net.jemsit.coal.api.dto.PileDtos.PileCreateRequest;
import net.jemsit.coal.api.dto.PileDtos.PileDto;
import net.jemsit.coal.api.dto.PileDtos.PileLineageDto;
import net.jemsit.coal.api.dto.PileDtos.PileMergeRequest;
import net.jemsit.coal.api.dto.PileDtos.PileSplitRequest;
import net.jemsit.coal.api.dto.PileDtos.PileTransferRequest;
import net.jemsit.coal.api.dto.MovementDtos.MovementDto;
import net.jemsit.coal.api.dto.OptionDtos.OptionDto;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.api.service.PileService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/piles")
public class PileController {

    private final PileService service;
    private final ActivityLog activity;

    public PileController(PileService service, ActivityLog activity) {
        this.service = service;
        this.activity = activity;
    }

    @GetMapping
    public PageResponse<PileDto> list(
            @PageableDefault(size = 10, sort = {"pileNumber", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/options")
    public List<OptionDto> options() {
        return service.options();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PileDto create(@Valid @RequestBody PileCreateRequest req) {
        PileDto pile = service.create(req);
        activity.record(ActivityKind.CREATE, "PILE", pile.pileNumber(),
                "Создал штабель " + pile.pileNumber() + " (" + ActivityLog.tons(pile.volume())
                        + ", склад " + orDash(pile.warehouseNumber()) + ")");
        return pile;
    }

    private static String orDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    @GetMapping("/{id}")
    public PileDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,
                       @RequestHeader(name = "X-Admin-Key", required = false) String adminKey,
                       @RequestParam(required = false) String confirmNumber) {
        service.delete(id, adminKey, confirmNumber);
        activity.record(ActivityKind.DELETE, "PILE", confirmNumber, "Удалил штабель " + confirmNumber);
    }

    @GetMapping("/{id}/history")
    public List<MovementDto> history(@PathVariable UUID id) {
        return service.history(id);
    }

    @GetMapping("/{id}/lineage")
    public PileLineageDto lineage(@PathVariable UUID id) {
        return service.lineage(id);
    }

    @PostMapping("/{id}/split")
    @ResponseStatus(HttpStatus.CREATED)
    public PileDto split(@PathVariable UUID id, @Valid @RequestBody PileSplitRequest req) {
        String source = activity.pileNumber(id);
        PileDto created = service.split(id, req);
        activity.record(ActivityKind.ACTION, "PILE", source,
                "Разделил штабель " + source + ": " + ActivityLog.tons(req.volume())
                        + " в новый штабель " + created.pileNumber());
        return created;
    }

    @PostMapping("/merge")
    public PileDto merge(@Valid @RequestBody PileMergeRequest req) {
        String sources = String.join(", ", req.sourcePileIds().stream().map(activity::pileNumber).toList());
        PileDto target = service.merge(req);
        activity.record(ActivityKind.ACTION, "PILE", target.pileNumber(),
                "Объединил штабели " + sources + " в штабель " + target.pileNumber());
        return target;
    }

    @PostMapping("/{id}/accept-batch")
    public PileDto acceptBatch(@PathVariable UUID id,
                               @Valid @RequestBody PileAcceptBatchRequest req) {
        PileDto pile = service.acceptBatch(id, req);
        String batch = activity.batchNumber(req.batchId());
        activity.record(ActivityKind.ACTION, "PILE", pile.pileNumber(),
                "Принял партию " + batch + " в штабель " + pile.pileNumber() + " (" + ActivityLog.tons(req.volume()) + ")");
        return pile;
    }

    @PostMapping("/{id}/transfer")
    public PileDto transfer(@PathVariable UUID id, @Valid @RequestBody PileTransferRequest req) {
        String from = activity.pileNumber(id);
        PileDto pile = service.transfer(id, req);
        activity.record(ActivityKind.ACTION, "PILE", pile.pileNumber(),
                "Перенес штабель " + from + " на склад " + activity.warehouseNumber(req.toWarehouseId()));
        return pile;
    }
}
