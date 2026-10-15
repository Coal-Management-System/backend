package net.jemsit.coal.shared.repo;

import net.jemsit.coal.shared.domain.Movement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MovementRepository extends JpaRepository<Movement, UUID> {

    List<Movement> findAllByOrderByCreatedAtDesc();

    List<Movement> findByPileIdOrderByCreatedAtDesc(UUID pileId);

    boolean existsByBatchId(UUID batchId);
}
