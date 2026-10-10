package net.jemsit.coal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import net.jemsit.coal.shared.domain.HeapStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class HeapDtos {

    private HeapDtos() {
    }

    public record HeapDto(
            UUID id,
            String heapNumber,
            BigDecimal volume,
            UUID warehouseId,
            String warehouseNumber,
            HeapStatus status,
            String originSource,
            List<UUID> batchIds,
            List<String> batchNumbers,
            Instant createdAt) {
    }

    public record HeapCreateRequest(
            @NotBlank String heapNumber,
            @NotNull @Positive BigDecimal volume,
            UUID warehouseId,
            String originSource,
            List<UUID> batchIds) {
    }

    public record HeapSplitRequest(
            @NotNull @Positive BigDecimal volume,
            UUID toWarehouseId,
            @NotBlank String newBatchNumber,
            Boolean sendImmediately) {
    }

    public record HeapMergeRequest(
            @NotNull List<UUID> sourceHeapIds,
            @NotNull UUID targetHeapId) {
    }

    public record HeapTransferRequest(@NotNull UUID toWarehouseId) {
    }
}
