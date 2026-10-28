package net.jemsit.coal.shared.repo;

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
}
