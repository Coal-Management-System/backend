package net.jemsit.coal.api.service;

import net.jemsit.coal.api.dto.PileDtos.PileDto;
import net.jemsit.coal.api.dto.WarehouseDtos.WarehouseCreateRequest;
import net.jemsit.coal.api.dto.WarehouseDtos.WarehouseDto;
import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.domain.Warehouse;
import net.jemsit.coal.shared.domain.WarehouseStatus;
import net.jemsit.coal.shared.repo.CoalPileRepository;
import net.jemsit.coal.shared.repo.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouses;
    private final CoalPileRepository piles;
    private final PileService pileService;

    public WarehouseService(WarehouseRepository warehouses, CoalPileRepository piles, PileService pileService) {
        this.warehouses = warehouses;
        this.piles = piles;
        this.pileService = pileService;
    }

    @Transactional(readOnly = true)
    public List<WarehouseDto> list() {
        return warehouses.findAllByOrderByNumberAsc().stream().map(w -> new WarehouseDto(
                w.getId(),
                w.getNumber(),
                w.getLocation(),
                w.getStatus(),
                piles.findByWarehouseIdOrderByPileNumberAsc(w.getId()).size(),
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
    public List<PileDto> piles(UUID warehouseId) {
        return piles.findByWarehouseIdOrderByPileNumberAsc(warehouseId).stream()
                .map(pileService::toDto).toList();
    }
}
