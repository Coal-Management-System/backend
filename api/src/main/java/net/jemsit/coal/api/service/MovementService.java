package net.jemsit.coal.api.service;

import net.jemsit.coal.api.dto.MovementDtos.MovementCreateRequest;
import net.jemsit.coal.api.dto.MovementDtos.MovementDto;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.domain.BatchStatus;
import net.jemsit.coal.shared.domain.CoalBatch;
import net.jemsit.coal.shared.domain.CoalPile;
import net.jemsit.coal.shared.domain.Movement;
import net.jemsit.coal.shared.domain.MovementOperation;
import net.jemsit.coal.shared.domain.PileStatus;
import net.jemsit.coal.shared.repo.CoalBatchRepository;
import net.jemsit.coal.shared.repo.CoalPileRepository;
import net.jemsit.coal.shared.repo.MovementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class MovementService {

    private final MovementRepository movements;
    private final CoalBatchRepository batches;
    private final CoalPileRepository piles;
    private final AdminKeyGuard guard;
    private final String qrBaseUrl;

    public MovementService(MovementRepository movements,
                           CoalBatchRepository batches,
                           CoalPileRepository piles,
                           AdminKeyGuard guard,
                           @Value("${coal.qr.base-url:http://localhost:5174}") String qrBaseUrl) {
        this.movements = movements;
        this.batches = batches;
        this.piles = piles;
        this.guard = guard;
        this.qrBaseUrl = qrBaseUrl;
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementDto> list(Pageable pageable) {
        return PageResponse.of(movements.findAll(pageable), this::toDto);
    }

    @Transactional(readOnly = true)
    public MovementDto get(UUID id) {
        return toDto(movements.findById(id)
                .orElseThrow(() -> ApiException.notFound("Движение не найдено")));
    }

    @Transactional
    public MovementDto create(MovementCreateRequest req) {
        // Batch before pile, in both create and delete, so two movements cannot deadlock.
        CoalBatch batch = req.batchId() == null ? null : batches.findForUpdate(req.batchId())
                .orElseThrow(() -> ApiException.notFound("Партия не найдена: " + req.batchId()));
        CoalPile pile = req.pileId() == null ? null : piles.findForUpdate(req.pileId())
                .orElseThrow(() -> ApiException.notFound("Штабель не найден: " + req.pileId()));
        // Accepted-into-pile rows are the source the party's accepted volume is derived from,
        // so a free-form one here would shrink the party's remainder without growing any pile.
        // That operation belongs to the piles screen, where both sides are updated together.
        if (req.operation() == MovementOperation.BATCH_ACCEPTED_INTO_PILE) {
            throw ApiException.badRequest(
                    "Приём партии в штабель выполняется на странице штабелей");
        }
        boolean movesVolumes = movesVolumes(req.operation());
        if (movesVolumes) {
            checkAvailable(req.operation(), req.volume(), batch, pile);
        }
        apply(req.operation(), req.volume(), batch, pile, 1);
        Movement m = new Movement();
        m.setBatch(batch);
        m.setPile(pile);
        m.setOperation(req.operation());
        m.setVolume(req.volume());
        m.setFromLocation(req.fromLocation());
        m.setToLocation(req.toLocation());
        m.setOriginSource(req.originSource());
        m.setVolumesApplied(movesVolumes);
        m.setTakenFromPile(pile != null && leavesPile(req.operation()));
        return toDto(movements.save(m));
    }

    /**
     * Permanently deletes one movement record. A movement has no unique name, so
     * the confirmation word is its operation code (e.g. {@code SHIPMENT}) typed
     * back, alongside the admin key. The volumes it moved are put back on the party
     * and the pile; rows older than volume tracking never moved any, so they are
     * just removed. A {@code BATCH_ACCEPTED_INTO_PILE} row feeds the batch's
     * accepted-volume total, so deleting one gives that volume back too.
     */
    @Transactional
    public void delete(UUID id, String adminKey, String confirmOperation) {
        guard.check(adminKey);
        Movement m = movements.findById(id)
                .orElseThrow(() -> ApiException.notFound("Движение не найдено"));
        if (!m.getOperation().name().equals(confirmOperation)) {
            throw ApiException.badRequest("Операция для подтверждения не совпадает");
        }
        if (m.isVolumesApplied()) {
            CoalBatch batch = m.getBatch() == null ? null
                    : batches.findForUpdate(m.getBatch().getId()).orElse(null);
            CoalPile pile = m.getPile() == null ? null
                    : piles.findForUpdate(m.getPile().getId()).orElse(null);
            apply(m.getOperation(), m.getVolume(), batch, pile, -1);
        }
        movements.delete(m);
    }

    private static boolean movesVolumes(MovementOperation operation) {
        return operation == MovementOperation.RESERVATION || leavesPile(operation);
    }

    /** Reserved coal is only spoken for and stays where it lies; these two physically leave. */
    private static boolean leavesPile(MovementOperation operation) {
        return operation == MovementOperation.PROCESSING || operation == MovementOperation.SHIPMENT;
    }

    /**
     * Moves the volumes one movement stands for, forwards when {@code sign} is 1 (a new
     * record) and backwards when it is -1 (deleting one).
     *
     * <ul>
     *   <li>{@code PROCESSING} — coal goes to enrichment: {@code inProcessingVolume} grows,
     *       free volume falls, and the pile shrinks.</li>
     *   <li>{@code SHIPMENT} — coal leaves the site: {@code shippedVolume} grows, which lowers
     *       the party's {@code currentRemainder} and free volume, and the pile shrinks.</li>
     *   <li>{@code RESERVATION} — coal is only spoken for: {@code reservedVolume} grows and free
     *       volume falls, but nothing leaves the pile.</li>
     * </ul>
     *
     * <p>Every other operation is journal-only: pile lifecycle rows have their volume effects in
     * the pile operations that record them, and applying them twice would corrupt balances.
     */
    private void apply(MovementOperation operation, BigDecimal volume,
                       CoalBatch batch, CoalPile pile, int sign) {
        BigDecimal delta = volume.multiply(BigDecimal.valueOf(sign));
        if (batch != null) {
            switch (operation) {
                case PROCESSING -> {
                    batch.setInProcessingVolume(batch.getInProcessingVolume().add(delta));
                    syncProcessingStatus(batch, sign);
                }
                case SHIPMENT -> {
                    batch.setShippedVolume(batch.getShippedVolume().add(delta));
                    syncShippedStatus(batch);
                }
                case RESERVATION -> batch.setReservedVolume(batch.getReservedVolume().add(delta));
                default -> {
                    // Journal-only: nothing to move.
                }
            }
        }
        if (pile != null && leavesPile(operation)) {
            pile.setVolume(pile.getVolume().subtract(delta));
            if (pile.getVolume().signum() == 0) {
                pile.setStatus(PileStatus.EMPTY);
            } else if (pile.getStatus() == PileStatus.EMPTY) {
                pile.setStatus(PileStatus.ACTIVE);
            }
        }
    }

    /** Undoing the last processing returns the party to NEW instead of leaving it «in processing». */
    private void syncProcessingStatus(CoalBatch batch, int sign) {
        if (sign > 0) {
            markStatus(batch, BatchStatus.IN_PROCESSING);
        } else if (batch.getStatus() == BatchStatus.IN_PROCESSING
                && batch.getInProcessingVolume().signum() <= 0) {
            batch.setStatus(BatchStatus.NEW);
        }
    }

    /** Only a fully shipped party is SHIPPED; undoing a shipment reopens it. */
    private void syncShippedStatus(CoalBatch batch) {
        if (batch.currentRemainder().signum() <= 0) {
            markStatus(batch, BatchStatus.SHIPPED);
        } else if (batch.getStatus() == BatchStatus.SHIPPED) {
            batch.setStatus(BatchStatus.NEW);
        }
    }

    /** An archived party's lifecycle is finished; its status is not rewritten by movements. */
    private void markStatus(CoalBatch batch, BatchStatus status) {
        if (batch.getStatus() != BatchStatus.ARCHIVED) {
            batch.setStatus(status);
        }
    }

    /**
     * Refuses to move more than exists. Only checked when recording a movement — undoing one
     * always gives volume back, and blocking that would leave a record impossible to delete.
     */
    private void checkAvailable(MovementOperation operation, BigDecimal volume,
                                CoalBatch batch, CoalPile pile) {
        String purpose = switch (operation) {
            case PROCESSING -> "переработку";
            case SHIPMENT -> "отгрузку";
            default -> "резервирование";
        };
        if (batch == null && pile == null) {
            throw ApiException.badRequest("Укажите партию или штабель: этой операции не с чего списать объем");
        }
        if (batch != null && pile != null
                && batch.getPiles().stream().noneMatch(p -> p.getId().equals(pile.getId()))) {
            throw ApiException.badRequest("Партии " + batch.getBatchNumber() + " нет в штабеле "
                    + pile.getPileNumber());
        }
        if (batch != null && volume.compareTo(batch.freeVolume()) > 0) {
            throw ApiException.badRequest("Объем на " + purpose + " больше свободного объема партии "
                    + batch.getBatchNumber() + " (" + batch.freeVolume() + " т)");
        }
        if (pile != null && volume.compareTo(pile.getVolume()) > 0) {
            throw ApiException.badRequest("Объем на " + purpose + " больше объема штабеля "
                    + pile.getPileNumber() + " (" + pile.getVolume() + " т)");
        }
        // Coal already in a pile can only leave through that pile, or the pile would keep it too.
        if (batch != null && pile == null && leavesPile(operation)) {
            BigDecimal unplaced = movements.unplacedVolume(batch);
            if (volume.compareTo(unplaced) > 0) {
                throw ApiException.badRequest("Вне штабелей у партии " + batch.getBatchNumber()
                        + " только " + unplaced + " т — для остального укажите штабель");
            }
        }
    }

    public MovementDto toDto(Movement m) {
        return new MovementDto(
                m.getId(),
                m.getBatch() == null ? null : m.getBatch().getId(),
                m.getBatch() == null ? null : m.getBatch().getBatchNumber(),
                m.getPile() == null ? null : m.getPile().getId(),
                m.getPile() == null ? null : m.getPile().getPileNumber(),
                m.getOperation(),
                m.getVolume(),
                m.getFromLocation(),
                m.getToLocation(),
                m.getOriginSource(),
                m.getCreatedAt(),
                qrBaseUrl + "/qr/movement/" + m.getId());
    }
}
