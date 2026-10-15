package net.jemsit.coal.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import net.jemsit.coal.shared.domain.MovementOperation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class MovementDtos {

    private MovementDtos() {
    }

    public record MovementDto(
            UUID id,
            UUID batchId,
            String batchNumber,
            UUID pileId,
            String pileNumber,
            MovementOperation operation,
            BigDecimal volume,
            String fromLocation,
            String toLocation,
            String originSource,
            Instant createdAt,
            String qrUrl) {
    }

    public record MovementCreateRequest(
            UUID batchId,
            UUID pileId,
            @NotNull MovementOperation operation,
            @NotNull @Positive BigDecimal volume,
            String fromLocation,
            String toLocation,
            String originSource) {
    }
}
