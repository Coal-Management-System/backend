package net.jemsit.coal.api.service;

import net.jemsit.coal.api.dto.PileDtos.PileAcceptBatchRequest;
import net.jemsit.coal.api.dto.PileDtos.PileCreateRequest;
import net.jemsit.coal.api.dto.PileDtos.PileDto;
import net.jemsit.coal.api.dto.PileDtos.PileLineageDto;
import net.jemsit.coal.api.dto.PileDtos.PileMergeRequest;
import net.jemsit.coal.api.dto.PileDtos.PileSplitRequest;
import net.jemsit.coal.api.dto.PileDtos.PileTransferRequest;
import net.jemsit.coal.api.dto.PileDtos.LineageEdgeDto;
import net.jemsit.coal.api.dto.PileDtos.LineageNodeDto;
import net.jemsit.coal.api.dto.MovementDtos.MovementDto;
import net.jemsit.coal.api.dto.OptionDtos.OptionDto;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.domain.BatchStatus;
import net.jemsit.coal.shared.domain.CoalBatch;
import net.jemsit.coal.shared.domain.CoalPile;
import net.jemsit.coal.shared.domain.PileStatus;
import net.jemsit.coal.shared.domain.Movement;
import net.jemsit.coal.shared.domain.MovementOperation;
import net.jemsit.coal.shared.domain.Warehouse;
import net.jemsit.coal.shared.repo.CoalBatchRepository;
import net.jemsit.coal.shared.repo.CoalPileRepository;
import net.jemsit.coal.shared.repo.MovementRepository;
import net.jemsit.coal.shared.repo.WarehouseRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PileService {

    private final CoalPileRepository piles;
    private final CoalBatchRepository batches;
    private final WarehouseRepository warehouses;
    private final MovementRepository movements;
    private final MovementService movementService;

    public PileService(CoalPileRepository piles,
                       CoalBatchRepository batches,
                       WarehouseRepository warehouses,
                       MovementRepository movements,
                       MovementService movementService) {
        this.piles = piles;
        this.batches = batches;
        this.warehouses = warehouses;
        this.movements = movements;
        this.movementService = movementService;
    }

    @Transactional(readOnly = true)
    public PageResponse<PileDto> list(Pageable pageable) {
        return PageResponse.of(piles.findAll(pageable), this::toDto);
    }

    @Transactional(readOnly = true)
    public List<OptionDto> options() {
        return piles.findAll(Sort.by("pileNumber", "id")).stream()
                .map(p -> new OptionDto(p.getId(), p.getPileNumber()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PileDto get(UUID id) {
        return toDto(load(id));
    }

    @Transactional(readOnly = true)
    public List<MovementDto> history(UUID pileId) {
        load(pileId);
        return movements.findByPileIdOrderByCreatedAtDesc(pileId).stream()
                .map(movementService::toDto).toList();
    }

    /** How deep the provenance walk goes in each direction, and how big the graph may get. */
    private static final int LINEAGE_DEPTH = 3;
    private static final int LINEAGE_MAX_NODES = 80;

    /**
     * Provenance graph around one pile: which parties fed into it (and which piles those came
     * from), and which parties/piles it was split into. Edges follow the material flow —
     * batch→pile when a party sits in a pile, pile→batch when a split produced the party.
     */
    @Transactional(readOnly = true)
    public PileLineageDto lineage(UUID pileId) {
        CoalPile start = load(pileId);
        Map<UUID, LineageNodeDto> nodes = new LinkedHashMap<>();
        Map<String, LineageEdgeDto> edges = new LinkedHashMap<>();
        addPileNode(nodes, start, true);
        walkUp(start, LINEAGE_DEPTH, nodes, edges, new HashSet<>());
        walkDown(start, LINEAGE_DEPTH, nodes, edges, new HashSet<>());
        return new PileLineageDto(new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()));
    }

    private void walkUp(CoalPile pile, int depth, Map<UUID, LineageNodeDto> nodes,
                        Map<String, LineageEdgeDto> edges, Set<UUID> visited) {
        if (depth <= 0 || nodes.size() >= LINEAGE_MAX_NODES || !visited.add(pile.getId())) {
            return;
        }
        for (CoalBatch b : pile.getBatches()) {
            addBatchNode(nodes, b);
            addEdge(edges, b.getId(), pile.getId());
            // The party was split off some earlier pile: find that pile via the source party.
            for (CoalBatch source : b.getSourceBatches()) {
                addBatchNode(nodes, source);
                for (CoalPile parent : source.getPiles()) {
                    if (parent.getId().equals(pile.getId())) {
                        continue;
                    }
                    addPileNode(nodes, parent, false);
                    addEdge(edges, parent.getId(), b.getId());
                    walkUp(parent, depth - 1, nodes, edges, visited);
                }
            }
        }
    }

    private void walkDown(CoalPile pile, int depth, Map<UUID, LineageNodeDto> nodes,
                          Map<String, LineageEdgeDto> edges, Set<UUID> visited) {
        if (depth <= 0 || nodes.size() >= LINEAGE_MAX_NODES || !visited.add(pile.getId())) {
            return;
        }
        List<UUID> batchIds = pile.getBatches().stream().map(CoalBatch::getId).toList();
        if (batchIds.isEmpty()) {
            return;
        }
        for (CoalBatch child : batches.findBySourceBatchesIdIn(batchIds)) {
            addBatchNode(nodes, child);
            addEdge(edges, pile.getId(), child.getId());
            for (CoalPile next : child.getPiles()) {
                if (next.getId().equals(pile.getId())) {
                    continue;
                }
                addPileNode(nodes, next, false);
                addEdge(edges, child.getId(), next.getId());
                walkDown(next, depth - 1, nodes, edges, visited);
            }
        }
    }

    private void addPileNode(Map<UUID, LineageNodeDto> nodes, CoalPile pile, boolean current) {
        if (nodes.size() < LINEAGE_MAX_NODES || nodes.containsKey(pile.getId())) {
            nodes.putIfAbsent(pile.getId(),
                    new LineageNodeDto(pile.getId(), "pile", pile.getPileNumber(), current));
        }
    }

    private void addBatchNode(Map<UUID, LineageNodeDto> nodes, CoalBatch batch) {
        if (nodes.size() < LINEAGE_MAX_NODES || nodes.containsKey(batch.getId())) {
            nodes.putIfAbsent(batch.getId(),
                    new LineageNodeDto(batch.getId(), "batch", batch.getBatchNumber(), false));
        }
    }

    private void addEdge(Map<String, LineageEdgeDto> edges, UUID from, UUID to) {
        edges.putIfAbsent(from + ">" + to, new LineageEdgeDto(from, to));
    }

    @Transactional
    public PileDto create(PileCreateRequest req) {
        if (piles.existsByPileNumber(req.pileNumber())) {
            throw ApiException.conflict("Штабель с номером " + req.pileNumber() + " уже существует");
        }
        CoalPile pile = new CoalPile();
        pile.setPileNumber(req.pileNumber());
        pile.setVolume(req.volume());
        pile.setWarehouse(req.warehouseId() == null ? null : warehouses.findById(req.warehouseId())
                .orElseThrow(() -> ApiException.notFound("Склад не найден: " + req.warehouseId())));
        pile.setOriginSource(req.originSource());
        pile.setStatus(PileStatus.ACTIVE);
        if (req.batchIds() != null) {
            req.batchIds().forEach(id -> {
                CoalBatch batch = batches.findById(id)
                        .orElseThrow(() -> ApiException.notFound("Партия не найдена: " + id));
                batch.getPiles().add(pile);
                pile.getBatches().add(batch);
            });
        }
        CoalPile saved = piles.save(pile);
        recordMovement(saved, null, MovementOperation.PILE_CREATED, saved.getVolume(),
                saved.getOriginSource(), warehouseNumber(saved));
        return toDto(saved);
    }

    @Transactional
    public PileDto split(UUID pileId, PileSplitRequest req) {
        CoalPile source = load(pileId);
        if (req.volume().compareTo(source.getVolume()) > 0) {
            throw ApiException.badRequest("Объем отделения больше объема штабеля");
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

        CoalPile newPile = new CoalPile();
        newPile.setPileNumber(nextSplitNumber(source.getPileNumber()));
        newPile.setVolume(req.volume());
        newPile.setWarehouse(target);
        newPile.setOriginSource(source.getOriginSource());
        newPile.setStatus(PileStatus.ACTIVE);
        newBatch.getPiles().add(newPile);
        newPile.getBatches().add(newBatch);
        piles.save(newPile);

        source.setVolume(source.getVolume().subtract(req.volume()));
        if (source.getVolume().compareTo(BigDecimal.ZERO) == 0) {
            source.setStatus(PileStatus.EMPTY);
        }

        recordMovement(newPile, newBatch, MovementOperation.PILE_SPLIT, req.volume(),
                warehouseNumber(source), warehouseNumber(newPile));
        return toDto(newPile);
    }

    @Transactional
    public PileDto merge(PileMergeRequest req) {
        CoalPile target = load(req.targetPileId());
        for (UUID sourceId : req.sourcePileIds()) {
            if (sourceId.equals(req.targetPileId())) {
                continue;
            }
            CoalPile source = load(sourceId);
            BigDecimal moved = source.getVolume();
            target.setVolume(target.getVolume().add(moved));
            for (CoalBatch batch : List.copyOf(source.getBatches())) {
                batch.getPiles().remove(source);
                batch.getPiles().add(target);
            }
            target.getBatches().addAll(source.getBatches());
            source.getBatches().clear();
            source.setVolume(BigDecimal.ZERO);
            source.setStatus(PileStatus.MERGED);
            recordMovement(target, null, MovementOperation.PILE_MERGED, moved,
                    warehouseNumber(source), warehouseNumber(target));
        }
        return toDto(target);
    }

    @Transactional
    public PileDto acceptBatch(UUID pileId, PileAcceptBatchRequest req) {
        CoalPile pile = load(pileId);
        CoalBatch batch = batches.findById(req.batchId())
                .orElseThrow(() -> ApiException.notFound("Партия не найдена: " + req.batchId()));
        // Guard against re-accepting a party that already sits in this pile.
        boolean alreadyLinked = pile.getBatches().stream()
                .anyMatch(b -> b.getId().equals(batch.getId()));
        if (alreadyLinked) {
            throw ApiException.badRequest("Партия " + batch.getBatchNumber()
                    + " уже принята в штабель " + pile.getPileNumber());
        }
        if (req.volume().compareTo(batch.freeVolume()) > 0) {
            throw ApiException.badRequest(
                    "Объем приёма больше свободного остатка партии " + batch.getBatchNumber());
        }
        // Where the batch lay before this acceptance; empty when the batch had no piles yet.
        String from = batch.getPiles().stream()
                .map(this::warehouseNumber)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(", "));
        batch.getPiles().add(pile);
        pile.getBatches().add(batch);
        // The party document keeps its declared size; only the pile physically grows.
        pile.setVolume(pile.getVolume().add(req.volume()));
        recordMovement(pile, batch, MovementOperation.BATCH_ACCEPTED_INTO_PILE, req.volume(),
                from.isEmpty() ? null : from, warehouseNumber(pile));
        return toDto(pile);
    }

    @Transactional
    public PileDto transfer(UUID pileId, PileTransferRequest req) {
        CoalPile pile = load(pileId);
        Warehouse from = pile.getWarehouse();
        Warehouse to = warehouses.findById(req.toWarehouseId())
                .orElseThrow(() -> ApiException.notFound("Склад не найден: " + req.toWarehouseId()));
        pile.setWarehouse(to);
        recordMovement(pile, null, MovementOperation.PILE_TRANSFERRED, pile.getVolume(),
                warehouseNumber(from), warehouseNumber(to));
        return toDto(pile);
    }

    public CoalPile load(UUID id) {
        return piles.findById(id).orElseThrow(() -> ApiException.notFound("Штабель не найден: " + id));
    }

    private String nextSplitNumber(String base) {
        int seq = 1;
        String candidate;
        do {
            candidate = base + "-S" + seq++;
        } while (piles.existsByPileNumber(candidate));
        return candidate;
    }

    private String warehouseNumber(CoalPile pile) {
        return warehouseNumber(pile.getWarehouse());
    }

    private String warehouseNumber(Warehouse warehouse) {
        return warehouse == null ? null : warehouse.getNumber();
    }

    private void recordMovement(CoalPile pile, CoalBatch batch, MovementOperation operation,
                                BigDecimal volume, String from, String to) {
        Movement m = new Movement();
        m.setPile(pile);
        m.setBatch(batch);
        m.setOperation(operation);
        m.setVolume(volume);
        m.setFromLocation(from);
        m.setToLocation(to);
        m.setOriginSource(pile.getOriginSource());
        movements.save(m);
    }

    public PileDto toDto(CoalPile p) {
        return new PileDto(
                p.getId(),
                p.getPileNumber(),
                p.getVolume(),
                p.getWarehouse() == null ? null : p.getWarehouse().getId(),
                p.getWarehouse() == null ? null : p.getWarehouse().getNumber(),
                p.getStatus(),
                p.getOriginSource(),
                p.getBatches().stream().map(CoalBatch::getId).toList(),
                p.getBatches().stream().map(CoalBatch::getBatchNumber).toList(),
                p.getCreatedAt());
    }
}
