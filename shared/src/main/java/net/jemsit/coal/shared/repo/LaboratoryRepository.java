package net.jemsit.coal.shared.repo;

import net.jemsit.coal.shared.domain.Laboratory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LaboratoryRepository extends JpaRepository<Laboratory, UUID> {

    List<Laboratory> findAllByOrderByNameAsc();
}
