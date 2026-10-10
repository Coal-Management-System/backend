package net.jemsit.coal.shared.repo;

import net.jemsit.coal.shared.domain.CoalBatch;
import net.jemsit.coal.shared.domain.OriginType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoalBatchRepository extends JpaRepository<CoalBatch, UUID> {

    Optional<CoalBatch> findByBatchNumber(String batchNumber);

    boolean existsByBatchNumber(String batchNumber);

    List<CoalBatch> findAllByOrderByBatchNumberAsc();

    @Query("select coalesce(sum(b.initialWeight), 0) from CoalBatch b "
            + "where b.originType = :origin and b.minedAt >= :from")
    BigDecimal sumInitialWeightMinedAfter(@Param("origin") OriginType origin, @Param("from") Instant from);
}
