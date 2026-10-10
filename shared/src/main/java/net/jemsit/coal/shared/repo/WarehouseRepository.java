package net.jemsit.coal.shared.repo;

import net.jemsit.coal.shared.domain.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

    Optional<Warehouse> findByNumber(String number);

    boolean existsByNumber(String number);

    List<Warehouse> findAllByOrderByNumberAsc();
}
