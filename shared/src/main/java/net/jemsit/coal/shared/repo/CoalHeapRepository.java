package net.jemsit.coal.shared.repo;

import net.jemsit.coal.shared.domain.CoalHeap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoalHeapRepository extends JpaRepository<CoalHeap, UUID> {

    Optional<CoalHeap> findByHeapNumber(String heapNumber);

    boolean existsByHeapNumber(String heapNumber);

    List<CoalHeap> findAllByOrderByHeapNumberAsc();

    List<CoalHeap> findByWarehouseIdOrderByHeapNumberAsc(UUID warehouseId);
}
