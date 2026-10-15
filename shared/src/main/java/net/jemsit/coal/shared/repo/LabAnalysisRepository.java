package net.jemsit.coal.shared.repo;

import net.jemsit.coal.shared.domain.LabAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LabAnalysisRepository extends JpaRepository<LabAnalysis, UUID> {

    List<LabAnalysis> findAllByOrderByAnalysisDateDesc();

    List<LabAnalysis> findByBatchIdOrderByAnalysisDateAsc(UUID batchId);

    boolean existsByBatchId(UUID batchId);
}
