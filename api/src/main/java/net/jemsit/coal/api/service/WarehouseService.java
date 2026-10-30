package net.jemsit.coal.api.service;

import net.jemsit.coal.api.dto.OptionDtos.OptionDto;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.api.dto.PileDtos.PileDto;
import net.jemsit.coal.api.dto.WarehouseDtos.WarehouseCreateRequest;
import net.jemsit.coal.api.dto.WarehouseDtos.WarehouseDto;
import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.domain.CoalPile;
import net.jemsit.coal.shared.domain.Warehouse;
import net.jemsit.coal.shared.domain.WarehouseStatus;
import net.jemsit.coal.shared.repo.CoalPileRepository;
import net.jemsit.coal.shared.repo.WarehouseRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouses;
    private final CoalPileRepository piles;
    private final PileService pileService;
    private final AdminKeyGuard guard;

    public WarehouseService(WarehouseRepository warehouses, CoalPileRepository piles,
                            PileService pileService, AdminKeyGuard guard) {
        this.warehouses = warehouses;
        this.piles = piles;
        this.pileService = pileService;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public PageResponse<WarehouseDto> list(Pageable pageable) {
        return PageResponse.of(warehouses.findAll(pageable), w -> new WarehouseDto(
                w.getId(),
                w.getNumber(),
                w.getLocation(),
                w.getStatus(),
                piles.countByWarehouseId(w.getId()),
                w.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public List<OptionDto> options() {
        return warehouses.findAll(Sort.by("number", "id")).stream()
                .map(w -> new OptionDto(w.getId(), w.getNumber()))
                .toList();
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
    public List<PileDto> piles(UUID warehouseId) {
        return piles.findByWarehouseIdOrderByPileNumberAsc(warehouseId).stream()
                .map(pileService::toDto).toList();
    }

    /**
     * Permanently deletes a warehouse. Requires the admin key and the warehouse
     * number typed back. Its piles are unlinked (kept, {@code warehouse_id} set
     * null) rather than deleted.
     */
    @Transactional
    public void delete(UUID id, String adminKey, String confirmNumber) {
        guard.check(adminKey);
        Warehouse w = warehouses.findById(id)
                .orElseThrow(() -> ApiException.notFound("Склад не найден: " + id));
        if (!w.getNumber().equals(confirmNumber)) {
            throw ApiException.badRequest("Номер склада для подтверждения не совпадает");
        }
        for (CoalPile p : piles.findByWarehouseIdOrderByPileNumberAsc(id)) {
            p.setWarehouse(null);
        }
        warehouses.delete(w);
    }
}
