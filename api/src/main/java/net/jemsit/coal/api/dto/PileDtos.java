package net.jemsit.coal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import net.jemsit.coal.shared.domain.PileStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PileDtos {

    private PileDtos() {
    }

    public record PileDto(
            UUID id,
            String pileNumber,
            BigDecimal volume,
            UUID warehouseId,
            String warehouseNumber,
            PileStatus status,
            String originSource,
            List<UUID> batchIds,
            List<String> batchNumbers,
            Instant createdAt) {
    }

    public record PileCreateRequest(
            @NotBlank String pileNumber,
            @NotNull @Positive BigDecimal volume,
            UUID warehouseId,
            String originSource,
            List<UUID> batchIds) {
    }

    public record PileSplitRequest(
            @NotNull @Positive BigDecimal volume,
            UUID toWarehouseId,
            @NotBlank String newBatchNumber,
            Boolean sendImmediately) {
    }

    public record PileMergeRequest(
            @NotNull List<UUID> sourcePileIds,
            @NotNull UUID targetPileId) {
    }

    public record PileAcceptBatchRequest(
            @NotNull UUID batchId,
            @NotNull @Positive BigDecimal volume) {
    }

    public record PileTransferRequest(@NotNull UUID toWarehouseId) {
    }
}
