package net.jemsit.coal.api.service;

import net.jemsit.coal.api.dto.MovementDtos.MovementCreateRequest;
import net.jemsit.coal.api.dto.MovementDtos.MovementDto;
import net.jemsit.coal.shared.domain.Movement;
import net.jemsit.coal.shared.repo.CoalBatchRepository;
import net.jemsit.coal.shared.repo.CoalHeapRepository;
import net.jemsit.coal.shared.repo.MovementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MovementService {

    private final MovementRepository movements;
    private final CoalBatchRepository batches;
    private final CoalHeapRepository heaps;
    private final String qrBaseUrl;

    public MovementService(MovementRepository movements,
                           CoalBatchRepository batches,
                           CoalHeapRepository heaps,
                           @Value("${coal.qr.base-url:http://localhost:5174}") String qrBaseUrl) {
        this.movements = movements;
        this.batches = batches;
        this.heaps = heaps;
        this.qrBaseUrl = qrBaseUrl;
    }

    @Transactional(readOnly = true)
    public List<MovementDto> list() {
        return movements.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public MovementDto create(MovementCreateRequest req) {
        Movement m = new Movement();
        if (req.batchId() != null) {
            m.setBatch(batches.findById(req.batchId()).orElseThrow());
        }
        if (req.heapId() != null) {
            m.setHeap(heaps.findById(req.heapId()).orElseThrow());
        }
        m.setOperation(req.operation());
        m.setVolume(req.volume());
        m.setFromLocation(req.fromLocation());
        m.setToLocation(req.toLocation());
        m.setOriginSource(req.originSource());
        return toDto(movements.save(m));
    }

    public MovementDto toDto(Movement m) {
        return new MovementDto(
                m.getId(),
                m.getBatch() == null ? null : m.getBatch().getId(),
                m.getBatch() == null ? null : m.getBatch().getBatchNumber(),
                m.getHeap() == null ? null : m.getHeap().getId(),
                m.getHeap() == null ? null : m.getHeap().getHeapNumber(),
                m.getOperation(),
                m.getVolume(),
                m.getFromLocation(),
                m.getToLocation(),
                m.getOriginSource(),
                m.getCreatedAt(),
                qrBaseUrl + "/qr/movement/" + m.getId());
    }
}
