package net.jemsit.coal.api;

import jakarta.validation.Valid;
import net.jemsit.coal.api.dto.PileDtos.PileCreateRequest;
import net.jemsit.coal.api.dto.PileDtos.PileDto;
import net.jemsit.coal.api.dto.PileDtos.PileMergeRequest;
import net.jemsit.coal.api.dto.PileDtos.PileSplitRequest;
import net.jemsit.coal.api.dto.PileDtos.PileTransferRequest;
import net.jemsit.coal.api.dto.MovementDtos.MovementDto;
import net.jemsit.coal.api.dto.OptionDtos.OptionDto;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.api.service.PileService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/piles")
public class PileController {

    private final PileService service;

    public PileController(PileService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<PileDto> list(
            @PageableDefault(size = 10, sort = {"pileNumber", "id"}) Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/options")
    public List<OptionDto> options() {
        return service.options();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PileDto create(@Valid @RequestBody PileCreateRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public PileDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/history")
    public List<MovementDto> history(@PathVariable UUID id) {
        return service.history(id);
    }

    @PostMapping("/{id}/split")
    @ResponseStatus(HttpStatus.CREATED)
    public PileDto split(@PathVariable UUID id, @Valid @RequestBody PileSplitRequest req) {
        return service.split(id, req);
    }

    @PostMapping("/merge")
    public PileDto merge(@Valid @RequestBody PileMergeRequest req) {
        return service.merge(req);
    }

    @PostMapping("/{id}/transfer")
    public PileDto transfer(@PathVariable UUID id, @Valid @RequestBody PileTransferRequest req) {
        return service.transfer(id, req);
    }
}
