package net.jemsit.coal.shared.repo;

import net.jemsit.coal.shared.domain.CoalBatch;
import net.jemsit.coal.shared.domain.OriginType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoalBatchRepository extends JpaRepository<CoalBatch, UUID> {

    Optional<CoalBatch> findByBatchNumberIgnoreCase(String batchNumber);

    boolean existsByBatchNumber(String batchNumber);

    /**
     * Party numbers already issued for one day, e.g. every «П-021126-…». Read under an
     * advisory lock by {@code BatchNumberGenerator} to find the day's last counter.
     */
    @Query("select b.batchNumber from CoalBatch b where b.batchNumber like concat(:prefix, '%')")
    List<String> findNumbersStartingWith(@Param("prefix") String prefix);

    boolean existsBySourceBatchesId(UUID sourceBatchId);

    /** Batches split off any of the given source batches; used by the provenance graph. */
    List<CoalBatch> findBySourceBatchesIdIn(Collection<UUID> sourceBatchIds);

    @Query("select coalesce(sum(b.initialWeight), 0) from CoalBatch b "
            + "where b.originType = :origin and b.minedAt >= :from")
    BigDecimal sumInitialWeightMinedAfter(@Param("origin") OriginType origin, @Param("from") Instant from);
}
