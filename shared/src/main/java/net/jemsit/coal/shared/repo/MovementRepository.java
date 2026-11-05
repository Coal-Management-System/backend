package net.jemsit.coal.shared.repo;

import net.jemsit.coal.shared.domain.CoalBatch;
import net.jemsit.coal.shared.domain.Movement;
import net.jemsit.coal.shared.domain.MovementOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface MovementRepository extends JpaRepository<Movement, UUID> {

    List<Movement> findByPileIdOrderByCreatedAtDesc(UUID pileId);

    boolean existsByBatchId(UUID batchId);

    /** Total volume moved for one batch under a given operation; 0 when there are none. */
    @Query("select coalesce(sum(m.volume), 0) from Movement m "
            + "where m.batch.id = :batchId and m.operation = :operation")
    BigDecimal sumVolume(@Param("batchId") UUID batchId,
                         @Param("operation") MovementOperation operation);

    /** Per-batch totals for one operation, as {batchId, sum} rows, for bulk lookups. */
    @Query("select m.batch.id, sum(m.volume) from Movement m "
            + "where m.batch is not null and m.operation = :operation group by m.batch.id")
    List<Object[]> sumVolumeGrouped(@Param("operation") MovementOperation operation);

    String UNPLACED_ADJUSTMENT = "coalesce(sum(case"
            + " when m.operation = net.jemsit.coal.shared.domain.MovementOperation.SHIPMENT"
            + " and m.takenFromPile = true then m.volume"
            + " when m.operation = net.jemsit.coal.shared.domain.MovementOperation.PROCESSING"
            + " and m.takenFromPile = false then 0BD - m.volume"
            + " else 0BD end), 0BD)";

    /**
     * Correction from "remainder minus accepted" to what truly lies outside any pile. A shipment
     * from a pile lowers the remainder, but that coal was already counted as accepted, so it is
     * added back; processing outside a pile leaves the remainder alone, yet that coal is gone.
     */
    @Query("select " + UNPLACED_ADJUSTMENT + " from Movement m "
            + "where m.batch.id = :batchId and m.volumesApplied = true")
    BigDecimal unplacedAdjustment(@Param("batchId") UUID batchId);

    /** {@link #unplacedAdjustment} per batch, as {batchId, adjustment} rows, for bulk lookups. */
    @Query("select m.batch.id, " + UNPLACED_ADJUSTMENT + " from Movement m "
            + "where m.batch is not null and m.volumesApplied = true group by m.batch.id")
    List<Object[]> unplacedAdjustmentGrouped();

    /** Volume of a party not yet in any pile: the «Остаток партии» shown in the parties list. */
    default BigDecimal unplacedVolume(CoalBatch batch) {
        return batch.unplacedVolume(
                sumVolume(batch.getId(), MovementOperation.BATCH_ACCEPTED_INTO_PILE),
                unplacedAdjustment(batch.getId()));
    }
}
