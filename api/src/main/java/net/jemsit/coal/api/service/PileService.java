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
import java.util.Comparator;
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
    private final BatchNumberGenerator numberGenerator;
    private final AdminKeyGuard guard;

    public PileService(CoalPileRepository piles,
                       CoalBatchRepository batches,
                       WarehouseRepository warehouses,
                       MovementRepository movements,
                       MovementService movementService,
                       BatchNumberGenerator numberGenerator,
                       AdminKeyGuard guard) {
        this.piles = piles;
        this.batches = batches;
        this.warehouses = warehouses;
        this.movements = movements;
        this.movementService = movementService;
        this.numberGenerator = numberGenerator;
        this.guard = guard;
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
                // A transfer takes its sources out of the pile; link them directly instead.
                if (source.getPiles().isEmpty()) {
                    addEdge(edges, source.getId(), b.getId());
                }
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
        Warehouse target = req.toWarehouseId() == null
                ? source.getWarehouse()
                : warehouses.findById(req.toWarehouseId())
                .orElseThrow(() -> ApiException.notFound("Склад не найден: " + req.toWarehouseId()));

        List<CoalBatch> parties = oldestFirst(source);

        CoalBatch newBatch = new CoalBatch();
        // Same daily counter as a normally created party, drawn inside this transaction so a
        // split and a create running at the same time cannot be handed the same number. An
        // operator-supplied number is honoured instead, after a duplicate check.
        newBatch.setBatchNumber(numberGenerator.resolve(req.newBatchNumber()));
        newBatch.setInitialWeight(req.volume());
        newBatch.setStatus(BatchStatus.NEW);
        // The split volume leaves the old parties for the new one; keeping it on both would
        // double-count the coal. They stay in the pile, which still holds the rest.
        List<CoalBatch> givers = takeVolume(parties, req.volume());
        // A pile whose parties have nothing free still names its oldest one, for the lineage.
        List<CoalBatch> origins = givers.isEmpty() ? parties.stream().limit(1).toList() : givers;
        if (!origins.isEmpty()) {
            copyPassport(origins.getFirst(), newBatch);
            newBatch.getSourceBatches().addAll(origins);
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
        // A party may be folded into piles in several partial accepts; what is left is its
        // declared size minus everything already accepted, so the same party can be accepted
        // again (even into the same pile) until nothing remains.
        BigDecimal remaining = movements.unplacedVolume(batch);
        if (req.volume() == null || req.volume().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Объем приёма должен быть больше нуля");
        }
        if (req.volume().compareTo(remaining) > 0) {
            throw ApiException.badRequest("Объем приёма больше остатка партии "
                    + batch.getBatchNumber() + " (" + remaining + " т)");
        }
        // Where the batch lay before this acceptance; empty when the batch had no piles yet.
        String from = batch.getPiles().stream()
                .map(this::warehouseNumber)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(", "));
        batch.getPiles().add(pile);
        pile.getBatches().add(batch);
        // The party document keeps its declared size; the pile physically grows by the accepted part.
        pile.setVolume(pile.getVolume().add(req.volume()));
        recordMovement(pile, batch, MovementOperation.BATCH_ACCEPTED_INTO_PILE, req.volume(),
                from.isEmpty() ? null : from, warehouseNumber(pile));
        // Once the whole party has been folded into piles, its own lifecycle is over.
        if (remaining.subtract(req.volume()).compareTo(BigDecimal.ZERO) <= 0) {
            batch.setStatus(BatchStatus.ARCHIVED);
        }
        return toDto(pile);
    }

    /**
     * Moves a whole pile to another warehouse. Its coal becomes one new party, which replaces
     * the old parties in the pile and names them as its sources. The tonnage leaves the old
     * parties, oldest first, so totals stay correct; their declared weight is kept as history.
     */
    @Transactional
    public PileDto transfer(UUID pileId, PileTransferRequest req) {
        CoalPile pile = piles.findForUpdate(pileId)
                .orElseThrow(() -> ApiException.notFound("Штабель не найден: " + pileId));
        Warehouse from = pile.getWarehouse();
        Warehouse to = warehouses.findById(req.toWarehouseId())
                .orElseThrow(() -> ApiException.notFound("Склад не найден: " + req.toWarehouseId()));
        if (from != null && from.getId().equals(to.getId())) {
            throw ApiException.badRequest("Штабель " + pile.getPileNumber() + " уже находится на этом складе");
        }
        if (pile.getVolume().signum() <= 0) {
            throw ApiException.badRequest("Штабель " + pile.getPileNumber() + " пуст: переносить нечего");
        }
        List<CoalBatch> sources = oldestFirst(pile);
        // A reservation is a promise about specific coal; it has to be released or moved on purpose.
        for (CoalBatch source : sources) {
            if (source.getReservedVolume().signum() > 0) {
                throw ApiException.badRequest("У партии " + source.getBatchNumber() + " есть бронь "
                        + source.getReservedVolume() + " т — снимите ее перед трансфером");
            }
        }

        CoalBatch newBatch = new CoalBatch();
        newBatch.setBatchNumber(numberGenerator.next());
        newBatch.setInitialWeight(pile.getVolume());
        newBatch.setStatus(BatchStatus.NEW);
        if (!sources.isEmpty()) {
            copyPassport(sources.getFirst(), newBatch);
        }
        takeVolume(sources, pile.getVolume());
        for (CoalBatch source : sources) {
            source.getPiles().remove(pile);
            newBatch.getSourceBatches().add(source);
        }
        pile.getBatches().clear();
        newBatch.getPiles().add(pile);
        pile.getBatches().add(newBatch);
        batches.save(newBatch);

        pile.setWarehouse(to);
        recordMovement(pile, newBatch, MovementOperation.PILE_TRANSFERRED, pile.getVolume(),
                warehouseNumber(from), warehouseNumber(to));
        return toDto(pile);
    }

    private static List<CoalBatch> oldestFirst(CoalPile pile) {
        return pile.getBatches().stream()
                .sorted(Comparator.comparing(CoalBatch::getCreatedAt))
                .toList();
    }

    /**
     * Records {@code volume} as handed over by the given parties, in order, each up to what it
     * has in piles. Declared weights stay as history. Returns the parties that gave something.
     */
    private List<CoalBatch> takeVolume(List<CoalBatch> parties, BigDecimal volume) {
        List<CoalBatch> givers = new ArrayList<>();
        BigDecimal left = volume;
        for (CoalBatch party : parties) {
            BigDecimal taken = left.min(volumeInPile(party)).max(BigDecimal.ZERO);
            if (taken.signum() == 0) {
                continue;
            }
            left = left.subtract(taken);
            party.setTransferredVolume(party.getTransferredVolume().add(taken));
            if (party.currentRemainder().signum() <= 0 && party.getStatus() != BatchStatus.SHIPPED) {
                party.setStatus(BatchStatus.ARCHIVED);
            }
            givers.add(party);
        }
        return givers;
    }

    /**
     * How much of a party's free volume lies in piles. A party that was never accepted can
     * still be linked (split off a pile, or named when the pile was created): all of it counts.
     */
    private BigDecimal volumeInPile(CoalBatch batch) {
        BigDecimal accepted = movements.sumVolume(batch.getId(), MovementOperation.BATCH_ACCEPTED_INTO_PILE);
        if (accepted.signum() <= 0) {
            return batch.freeVolume();
        }
        return batch.freeVolume().subtract(movements.unplacedVolume(batch).max(BigDecimal.ZERO));
    }

    private static void copyPassport(CoalBatch from, CoalBatch to) {
        to.setProductType(from.getProductType());
        to.setCoalGrade(from.getCoalGrade());
        to.setFraction(from.getFraction());
        to.setOriginType(from.getOriginType());
        to.setDeposit(from.getDeposit());
        to.setMiningSite(from.getMiningSite());
        to.setQuarry(from.getQuarry());
        to.setShiftPeriod(from.getShiftPeriod());
        to.setSupplier(from.getSupplier());
        to.setMinedAt(from.getMinedAt());
    }

    public CoalPile load(UUID id) {
        return piles.findById(id).orElseThrow(() -> ApiException.notFound("Штабель не найден: " + id));
    }

    /**
     * Permanently deletes a pile. Requires the admin key and the pile number typed
     * back. The pile's movements are detached (kept as history, {@code pile_id} set
     * null) so the batch accepted-volume totals — derived from the batch side of the
     * ledger — are unchanged; the parties are unlinked but kept; and removing the
     * pile row takes it out of its warehouse.
     */
    @Transactional
    public void delete(UUID id, String adminKey, String confirmNumber) {
        guard.check(adminKey);
        CoalPile pile = load(id);
        if (!pile.getPileNumber().equals(confirmNumber)) {
            throw ApiException.badRequest("Номер штабеля для подтверждения не совпадает");
        }
        for (Movement m : movements.findByPileIdOrderByCreatedAtDesc(id)) {
            m.setPile(null);
        }
        // CoalBatch owns the join, so unlink must remove the pile from each party.
        for (CoalBatch b : List.copyOf(pile.getBatches())) {
            b.getPiles().remove(pile);
        }
        pile.getBatches().clear();
        pile.setWarehouse(null);
        piles.delete(pile);
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
