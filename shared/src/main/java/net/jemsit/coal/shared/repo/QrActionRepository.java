package net.jemsit.coal.shared.repo;

import jakarta.persistence.LockModeType;
import net.jemsit.coal.shared.domain.QrAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface QrActionRepository extends JpaRepository<QrAction, UUID> {

    /** Row-locked read, so two operators scanning one code cannot both accept the same volume. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from QrAction a where a.id = :id")
    Optional<QrAction> findForUpdate(@Param("id") UUID id);

    /** The code already issued for this party and warehouse, if it was not cancelled. */
    Optional<QrAction> findFirstByBatchIdAndWarehouseIdAndCancelledAtIsNull(UUID batchId, UUID warehouseId);
}
