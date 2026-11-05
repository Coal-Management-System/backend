package net.jemsit.coal.api.service;

import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.api.dto.PileDtos.PileAcceptBatchRequest;
import net.jemsit.coal.api.dto.PileDtos.PileDto;
import net.jemsit.coal.api.dto.QrActionDtos.QrActionCreateRequest;
import net.jemsit.coal.api.dto.QrActionDtos.QrActionDto;
import net.jemsit.coal.api.dto.QrActionDtos.QrActionExecuteRequest;
import net.jemsit.coal.api.dto.QrActionDtos.QrActionScanDto;
import net.jemsit.coal.api.dto.QrActionDtos.QrActionStatus;
import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.domain.CoalBatch;
import net.jemsit.coal.shared.domain.CoalPile;
import net.jemsit.coal.shared.domain.PileStatus;
import net.jemsit.coal.shared.domain.QrAction;
import net.jemsit.coal.shared.domain.Warehouse;
import net.jemsit.coal.shared.repo.CoalBatchRepository;
import net.jemsit.coal.shared.repo.CoalPileRepository;
import net.jemsit.coal.shared.repo.MovementRepository;
import net.jemsit.coal.shared.repo.QrActionRepository;
import net.jemsit.coal.shared.repo.WarehouseRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class QrActionService {

    private final QrActionRepository actions;
    private final CoalBatchRepository batches;
    private final WarehouseRepository warehouses;
    private final CoalPileRepository piles;
    private final MovementRepository movements;
    private final PileService pileService;

    public QrActionService(QrActionRepository actions,
                           CoalBatchRepository batches,
                           WarehouseRepository warehouses,
                           CoalPileRepository piles,
                           MovementRepository movements,
                           PileService pileService) {
        this.actions = actions;
        this.batches = batches;
        this.warehouses = warehouses;
        this.piles = piles;
        this.movements = movements;
        this.pileService = pileService;
    }

    @Transactional(readOnly = true)
    public PageResponse<QrActionDto> list(Pageable pageable) {
        return PageResponse.of(actions.findAll(pageable), this::toDto);
    }

    /** Asking twice for the same party and warehouse returns the code already printed. */
    @Transactional
    public QrActionDto create(QrActionCreateRequest req) {
        CoalBatch batch = batches.findById(req.batchId())
                .orElseThrow(() -> ApiException.notFound("Партия не найдена: " + req.batchId()));
        Warehouse warehouse = warehouses.findById(req.warehouseId())
                .orElseThrow(() -> ApiException.notFound("Склад не найден: " + req.warehouseId()));
        if (movements.unplacedVolume(batch).signum() <= 0) {
            throw ApiException.badRequest("У партии " + batch.getBatchNumber() + " нет объема для приёма в штабель");
        }
        QrAction action = actions
                .findFirstByBatchIdAndWarehouseIdAndCancelledAtIsNull(batch.getId(), warehouse.getId())
                .orElseGet(() -> {
                    QrAction created = new QrAction();
                    created.setAction(req.action());
                    created.setBatch(batch);
                    created.setWarehouse(warehouse);
                    return actions.save(created);
                });
        return toDto(action);
    }

    @Transactional(readOnly = true)
    public QrActionScanDto scan(UUID id) {
        QrAction action = load(id);
        List<PileDto> available = piles.findByWarehouseIdOrderByPileNumberAsc(action.getWarehouse().getId())
                .stream()
                .filter(p -> p.getStatus() != PileStatus.MERGED)
                .map(pileService::toDto)
                .toList();
        return new QrActionScanDto(toDto(action), available);
    }

    @Transactional
    public QrActionDto execute(UUID id, QrActionExecuteRequest req) {
        QrAction action = actions.findForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("QR-задание не найдено"));
        if (action.getCancelledAt() != null) {
            throw ApiException.badRequest("QR-задание отменено");
        }
        CoalPile pile = pileService.load(req.pileId());
        if (pile.getWarehouse() == null || !pile.getWarehouse().getId().equals(action.getWarehouse().getId())) {
            throw ApiException.badRequest("Штабель " + pile.getPileNumber() + " находится не на складе "
                    + action.getWarehouse().getNumber());
        }
        if (pile.getStatus() == PileStatus.MERGED) {
            throw ApiException.badRequest("Штабель " + pile.getPileNumber() + " уже объединен с другим");
        }
        pileService.acceptBatch(pile.getId(), new PileAcceptBatchRequest(action.getBatch().getId(), req.volume()));
        return toDto(action);
    }

    @Transactional
    public QrActionDto cancel(UUID id) {
        QrAction action = load(id);
        if (action.getCancelledAt() == null) {
            action.setCancelledAt(Instant.now());
        }
        return toDto(action);
    }

    private QrAction load(UUID id) {
        return actions.findById(id).orElseThrow(() -> ApiException.notFound("QR-задание не найдено"));
    }

    private QrActionDto toDto(QrAction a) {
        BigDecimal remaining = movements.unplacedVolume(a.getBatch());
        QrActionStatus status = a.getCancelledAt() != null ? QrActionStatus.CANCELLED
                : remaining.signum() <= 0 ? QrActionStatus.DONE
                : QrActionStatus.OPEN;
        return new QrActionDto(
                a.getId(),
                a.getAction(),
                status,
                a.getBatch().getId(),
                a.getBatch().getBatchNumber(),
                remaining,
                a.getWarehouse().getId(),
                a.getWarehouse().getNumber(),
                a.getCreatedAt());
    }
}
