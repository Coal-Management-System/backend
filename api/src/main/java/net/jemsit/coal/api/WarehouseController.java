package net.jemsit.coal.api;

import jakarta.validation.Valid;
import net.jemsit.coal.api.dto.HeapDtos.HeapDto;
import net.jemsit.coal.api.dto.WarehouseDtos.WarehouseCreateRequest;
import net.jemsit.coal.api.dto.WarehouseDtos.WarehouseDto;
import net.jemsit.coal.api.service.WarehouseService;
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
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService service;

    public WarehouseController(WarehouseService service) {
        this.service = service;
    }

    @GetMapping
    public List<WarehouseDto> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseDto create(@Valid @RequestBody WarehouseCreateRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}/heaps")
    public List<HeapDto> heaps(@PathVariable UUID id) {
        return service.heaps(id);
    }
}
