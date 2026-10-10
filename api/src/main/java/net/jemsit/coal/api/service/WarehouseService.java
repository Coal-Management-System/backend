package net.jemsit.coal.api.service;

import net.jemsit.coal.api.dto.HeapDtos.HeapDto;
import net.jemsit.coal.api.dto.WarehouseDtos.WarehouseCreateRequest;
import net.jemsit.coal.api.dto.WarehouseDtos.WarehouseDto;
import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.domain.Warehouse;
import net.jemsit.coal.shared.domain.WarehouseStatus;
import net.jemsit.coal.shared.repo.CoalHeapRepository;
import net.jemsit.coal.shared.repo.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouses;
    private final CoalHeapRepository heaps;
    private final HeapService heapService;

    public WarehouseService(WarehouseRepository warehouses, CoalHeapRepository heaps, HeapService heapService) {
        this.warehouses = warehouses;
        this.heaps = heaps;
        this.heapService = heapService;
    }

    @Transactional(readOnly = true)
    public List<WarehouseDto> list() {
        return warehouses.findAllByOrderByNumberAsc().stream().map(w -> new WarehouseDto(
                w.getId(),
                w.getNumber(),
                w.getLocation(),
                w.getStatus(),
                heaps.findByWarehouseIdOrderByHeapNumberAsc(w.getId()).size(),
                w.getCreatedAt())).toList();
    }

    @Transactional
    public WarehouseDto create(WarehouseCreateRequest req) {
        if (warehouses.existsByNumber(req.number())) {
            throw ApiException.conflict("Склад с номером " + req.number() + " уже существует");
        }
        Warehouse w = new Warehouse();
        w.setNumber(req.number());
        w.setLocation(req.location());
        w.setStatus(req.status() == null ? WarehouseStatus.ACTIVE : req.status());
        Warehouse saved = warehouses.save(w);
        return new WarehouseDto(saved.getId(), saved.getNumber(), saved.getLocation(),
                saved.getStatus(), 0, saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<HeapDto> heaps(UUID warehouseId) {
        return heaps.findByWarehouseIdOrderByHeapNumberAsc(warehouseId).stream()
                .map(heapService::toDto).toList();
    }
}
