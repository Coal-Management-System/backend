package net.jemsit.coal.shared.repo;

import net.jemsit.coal.shared.domain.CoalPile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoalPileRepository extends JpaRepository<CoalPile, UUID> {

    Optional<CoalPile> findByPileNumber(String pileNumber);

    /** Row-locked read, so concurrent movements cannot both spend the same pile volume. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from CoalPile p where p.id = :id")
    Optional<CoalPile> findForUpdate(@Param("id") UUID id);

    boolean existsByPileNumber(String pileNumber);

    List<CoalPile> findByWarehouseIdOrderByPileNumberAsc(UUID warehouseId);

    long countByWarehouseId(UUID warehouseId);
}
