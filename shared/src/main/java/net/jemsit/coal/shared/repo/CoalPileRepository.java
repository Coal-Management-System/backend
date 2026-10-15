package net.jemsit.coal.shared.repo;

import net.jemsit.coal.shared.domain.CoalPile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoalPileRepository extends JpaRepository<CoalPile, UUID> {

    Optional<CoalPile> findByPileNumber(String pileNumber);

    boolean existsByPileNumber(String pileNumber);

    List<CoalPile> findAllByOrderByPileNumberAsc();

    List<CoalPile> findByWarehouseIdOrderByPileNumberAsc(UUID warehouseId);
}
