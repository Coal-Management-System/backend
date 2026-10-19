package net.jemsit.coal.api;

import jakarta.validation.Valid;
import net.jemsit.coal.api.dto.OptionDtos.OptionDto;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.api.dto.PileDtos.PileDto;
import net.jemsit.coal.api.dto.WarehouseDtos.WarehouseCreateRequest;
import net.jemsit.coal.api.dto.WarehouseDtos.WarehouseDto;
import net.jemsit.coal.api.service.WarehouseService;
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
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService service;

    public WarehouseController(WarehouseService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<WarehouseDto> list(
            @PageableDefault(size = 10, sort = {"number", "id"}) Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/options")
    public List<OptionDto> options() {
        return service.options();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseDto create(@Valid @RequestBody WarehouseCreateRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}/piles")
    public List<PileDto> piles(@PathVariable UUID id) {
        return service.piles(id);
    }
}
