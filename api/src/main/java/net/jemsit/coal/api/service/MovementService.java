package net.jemsit.coal.api.service;

import net.jemsit.coal.api.dto.MovementDtos.MovementCreateRequest;
import net.jemsit.coal.api.dto.MovementDtos.MovementDto;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.domain.Movement;
import net.jemsit.coal.shared.repo.CoalBatchRepository;
import net.jemsit.coal.shared.repo.CoalPileRepository;
import net.jemsit.coal.shared.repo.MovementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MovementService {

    private final MovementRepository movements;
    private final CoalBatchRepository batches;
    private final CoalPileRepository piles;
    private final String qrBaseUrl;

    public MovementService(MovementRepository movements,
                           CoalBatchRepository batches,
                           CoalPileRepository piles,
                           @Value("${coal.qr.base-url:http://localhost:5174}") String qrBaseUrl) {
        this.movements = movements;
        this.batches = batches;
        this.piles = piles;
        this.qrBaseUrl = qrBaseUrl;
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementDto> list(Pageable pageable) {
        return PageResponse.of(movements.findAll(pageable), this::toDto);
    }

    @Transactional(readOnly = true)
    public MovementDto get(UUID id) {
        return toDto(movements.findById(id)
                .orElseThrow(() -> ApiException.notFound("Движение не найдено")));
    }

    @Transactional
    public MovementDto create(MovementCreateRequest req) {
        Movement m = new Movement();
        if (req.batchId() != null) {
            m.setBatch(batches.findById(req.batchId()).orElseThrow());
        }
        if (req.pileId() != null) {
            m.setPile(piles.findById(req.pileId()).orElseThrow());
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
                m.getPile() == null ? null : m.getPile().getId(),
                m.getPile() == null ? null : m.getPile().getPileNumber(),
                m.getOperation(),
                m.getVolume(),
                m.getFromLocation(),
                m.getToLocation(),
                m.getOriginSource(),
                m.getCreatedAt(),
                qrBaseUrl + "/qr/movement/" + m.getId());
    }
}
