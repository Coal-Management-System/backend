package net.jemsit.coal.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import net.jemsit.coal.shared.domain.QrActionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class QrActionDtos {

    private QrActionDtos() {
    }

    public enum QrActionStatus {
        OPEN,
        DONE,
        CANCELLED
    }

    public record QrActionDto(
            UUID id,
            QrActionType action,
            QrActionStatus status,
            UUID batchId,
            String batchNumber,
            BigDecimal remainingVolume,
            UUID warehouseId,
            String warehouseNumber,
            Instant createdAt) {
    }

    /** What the operator sees after scanning: the task and the piles it can go into. */
    public record QrActionScanDto(QrActionDto action, List<PileDtos.PileDto> piles) {
    }

    public record QrActionCreateRequest(
            @NotNull QrActionType action,
            @NotNull UUID batchId,
            @NotNull UUID warehouseId) {
    }

    public record QrActionExecuteRequest(
            @NotNull UUID pileId,
            @NotNull @Positive BigDecimal volume) {
    }
}
