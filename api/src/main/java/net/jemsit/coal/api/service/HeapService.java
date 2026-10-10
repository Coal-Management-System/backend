package net.jemsit.coal.api.service;

import net.jemsit.coal.api.dto.HeapDtos.HeapCreateRequest;
import net.jemsit.coal.api.dto.HeapDtos.HeapDto;
import net.jemsit.coal.api.dto.HeapDtos.HeapMergeRequest;
import net.jemsit.coal.api.dto.HeapDtos.HeapSplitRequest;
import net.jemsit.coal.api.dto.HeapDtos.HeapTransferRequest;
import net.jemsit.coal.api.dto.MovementDtos.MovementDto;
import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.domain.BatchStatus;
import net.jemsit.coal.shared.domain.CoalBatch;
import net.jemsit.coal.shared.domain.CoalHeap;
import net.jemsit.coal.shared.domain.HeapStatus;
import net.jemsit.coal.shared.domain.Movement;
import net.jemsit.coal.shared.domain.MovementOperation;
import net.jemsit.coal.shared.domain.Warehouse;
import net.jemsit.coal.shared.repo.CoalBatchRepository;
import net.jemsit.coal.shared.repo.CoalHeapRepository;
import net.jemsit.coal.shared.repo.MovementRepository;
import net.jemsit.coal.shared.repo.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class HeapService {

    private final CoalHeapRepository heaps;
    private final CoalBatchRepository batches;
    private final WarehouseRepository warehouses;
    private final MovementRepository movements;
    private final MovementService movementService;

    public HeapService(CoalHeapRepository heaps,
                       CoalBatchRepository batches,
                       WarehouseRepository warehouses,
                       MovementRepository movements,
                       MovementService movementService) {
        this.heaps = heaps;
        this.batches = batches;
        this.warehouses = warehouses;
        this.movements = movements;
        this.movementService = movementService;
    }

    @Transactional(readOnly = true)
    public List<HeapDto> list() {
        return heaps.findAllByOrderByHeapNumberAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public HeapDto get(UUID id) {
        return toDto(load(id));
    }

    @Transactional(readOnly = true)
    public List<MovementDto> history(UUID heapId) {
        load(heapId);
        return movements.findByHeapIdOrderByCreatedAtDesc(heapId).stream()
                .map(movementService::toDto).toList();
    }

    @Transactional
    public HeapDto create(HeapCreateRequest req) {
        if (heaps.existsByHeapNumber(req.heapNumber())) {
            throw ApiException.conflict("Куча с номером " + req.heapNumber() + " уже существует");
        }
        CoalHeap heap = new CoalHeap();
        heap.setHeapNumber(req.heapNumber());
        heap.setVolume(req.volume());
        heap.setWarehouse(req.warehouseId() == null ? null : warehouses.findById(req.warehouseId())
                .orElseThrow(() -> ApiException.notFound("Склад не найден: " + req.warehouseId())));
        heap.setOriginSource(req.originSource());
        heap.setStatus(HeapStatus.ACTIVE);
        if (req.batchIds() != null) {
            req.batchIds().forEach(id -> {
                CoalBatch batch = batches.findById(id)
                        .orElseThrow(() -> ApiException.notFound("Партия не найдена: " + id));
                batch.getHeaps().add(heap);
                heap.getBatches().add(batch);
            });
        }
        CoalHeap saved = heaps.save(heap);
        recordMovement(saved, null, MovementOperation.PILE_CREATED, saved.getVolume(),
                saved.getOriginSource(), warehouseNumber(saved));
        return toDto(saved);
    }

    @Transactional
    public HeapDto split(UUID heapId, HeapSplitRequest req) {
        CoalHeap source = load(heapId);
        if (req.volume().compareTo(source.getVolume()) > 0) {
            throw ApiException.badRequest("Объем отделения больше объема кучи");
        }
        if (batches.existsByBatchNumber(req.newBatchNumber())) {
            throw ApiException.conflict("Партия с номером " + req.newBatchNumber() + " уже существует");
        }
        Warehouse target = req.toWarehouseId() == null
                ? source.getWarehouse()
                : warehouses.findById(req.toWarehouseId())
                .orElseThrow(() -> ApiException.notFound("Склад не найден: " + req.toWarehouseId()));

        CoalBatch origin = source.getBatches().stream().findFirst().orElse(null);

        CoalBatch newBatch = new CoalBatch();
        newBatch.setBatchNumber(req.newBatchNumber());
        newBatch.setInitialWeight(req.volume());
        newBatch.setStatus(BatchStatus.NEW);
        if (origin != null) {
            newBatch.setProductType(origin.getProductType());
            newBatch.setCoalGrade(origin.getCoalGrade());
            newBatch.setFraction(origin.getFraction());
            newBatch.setOriginType(origin.getOriginType());
            newBatch.setDeposit(origin.getDeposit());
            newBatch.setMiningSite(origin.getMiningSite());
            newBatch.setQuarry(origin.getQuarry());
            newBatch.setShiftPeriod(origin.getShiftPeriod());
            newBatch.setSupplier(origin.getSupplier());
            newBatch.setMinedAt(origin.getMinedAt());
            newBatch.getSourceBatches().add(origin);
            // the split volume leaves the origin batch for the new one; keeping it on both would double-count the coal
            origin.setInitialWeight(origin.getInitialWeight().subtract(req.volume()));
        }
        if (Boolean.TRUE.equals(req.sendImmediately())) {
            newBatch.setStatus(BatchStatus.IN_PROCESSING);
            newBatch.setInProcessingVolume(req.volume());
        }
        batches.save(newBatch);

        CoalHeap newHeap = new CoalHeap();
        newHeap.setHeapNumber(nextSplitNumber(source.getHeapNumber()));
        newHeap.setVolume(req.volume());
        newHeap.setWarehouse(target);
        newHeap.setOriginSource(source.getOriginSource());
        newHeap.setStatus(HeapStatus.ACTIVE);
        newBatch.getHeaps().add(newHeap);
        newHeap.getBatches().add(newBatch);
        heaps.save(newHeap);

        source.setVolume(source.getVolume().subtract(req.volume()));
        if (source.getVolume().compareTo(BigDecimal.ZERO) == 0) {
            source.setStatus(HeapStatus.EMPTY);
        }

        recordMovement(newHeap, newBatch, MovementOperation.PILE_SPLIT, req.volume(),
                warehouseNumber(source), warehouseNumber(newHeap));
        return toDto(newHeap);
    }

    @Transactional
    public HeapDto merge(HeapMergeRequest req) {
        CoalHeap target = load(req.targetHeapId());
        for (UUID sourceId : req.sourceHeapIds()) {
            if (sourceId.equals(req.targetHeapId())) {
                continue;
            }
            CoalHeap source = load(sourceId);
            BigDecimal moved = source.getVolume();
            target.setVolume(target.getVolume().add(moved));
            for (CoalBatch batch : List.copyOf(source.getBatches())) {
                batch.getHeaps().remove(source);
                batch.getHeaps().add(target);
            }
            target.getBatches().addAll(source.getBatches());
            source.getBatches().clear();
            source.setVolume(BigDecimal.ZERO);
            source.setStatus(HeapStatus.MERGED);
            recordMovement(target, null, MovementOperation.PILE_MERGED, moved,
                    warehouseNumber(source), warehouseNumber(target));
        }
        return toDto(target);
    }

    @Transactional
    public HeapDto transfer(UUID heapId, HeapTransferRequest req) {
        CoalHeap heap = load(heapId);
        Warehouse from = heap.getWarehouse();
        Warehouse to = warehouses.findById(req.toWarehouseId())
                .orElseThrow(() -> ApiException.notFound("Склад не найден: " + req.toWarehouseId()));
        heap.setWarehouse(to);
        recordMovement(heap, null, MovementOperation.PILE_TRANSFERRED, heap.getVolume(),
                warehouseNumber(from), warehouseNumber(to));
        return toDto(heap);
    }

    public CoalHeap load(UUID id) {
        return heaps.findById(id).orElseThrow(() -> ApiException.notFound("Куча не найдена: " + id));
    }

    private String nextSplitNumber(String base) {
        int seq = 1;
        String candidate;
        do {
            candidate = base + "-S" + seq++;
        } while (heaps.existsByHeapNumber(candidate));
        return candidate;
    }

    private String warehouseNumber(CoalHeap heap) {
        return warehouseNumber(heap.getWarehouse());
    }

    private String warehouseNumber(Warehouse warehouse) {
        return warehouse == null ? null : warehouse.getNumber();
    }

    private void recordMovement(CoalHeap heap, CoalBatch batch, MovementOperation operation,
                                BigDecimal volume, String from, String to) {
        Movement m = new Movement();
        m.setHeap(heap);
        m.setBatch(batch);
        m.setOperation(operation);
        m.setVolume(volume);
        m.setFromLocation(from);
        m.setToLocation(to);
        m.setOriginSource(heap.getOriginSource());
        movements.save(m);
    }

    public HeapDto toDto(CoalHeap h) {
        return new HeapDto(
                h.getId(),
                h.getHeapNumber(),
                h.getVolume(),
                h.getWarehouse() == null ? null : h.getWarehouse().getId(),
                h.getWarehouse() == null ? null : h.getWarehouse().getNumber(),
                h.getStatus(),
                h.getOriginSource(),
                h.getBatches().stream().map(CoalBatch::getId).toList(),
                h.getBatches().stream().map(CoalBatch::getBatchNumber).toList(),
                h.getCreatedAt());
    }
}
