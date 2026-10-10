package net.jemsit.coal.api;

import jakarta.validation.Valid;
import net.jemsit.coal.api.dto.HeapDtos.HeapCreateRequest;
import net.jemsit.coal.api.dto.HeapDtos.HeapDto;
import net.jemsit.coal.api.dto.HeapDtos.HeapMergeRequest;
import net.jemsit.coal.api.dto.HeapDtos.HeapSplitRequest;
import net.jemsit.coal.api.dto.HeapDtos.HeapTransferRequest;
import net.jemsit.coal.api.dto.MovementDtos.MovementDto;
import net.jemsit.coal.api.service.HeapService;
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
@RequestMapping("/api/heaps")
public class HeapController {

    private final HeapService service;

    public HeapController(HeapService service) {
        this.service = service;
    }

    @GetMapping
    public List<HeapDto> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HeapDto create(@Valid @RequestBody HeapCreateRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public HeapDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/history")
    public List<MovementDto> history(@PathVariable UUID id) {
        return service.history(id);
    }

    @PostMapping("/{id}/split")
    @ResponseStatus(HttpStatus.CREATED)
    public HeapDto split(@PathVariable UUID id, @Valid @RequestBody HeapSplitRequest req) {
        return service.split(id, req);
    }

    @PostMapping("/merge")
    public HeapDto merge(@Valid @RequestBody HeapMergeRequest req) {
        return service.merge(req);
    }

    @PostMapping("/{id}/transfer")
    public HeapDto transfer(@PathVariable UUID id, @Valid @RequestBody HeapTransferRequest req) {
        return service.transfer(id, req);
    }
}
