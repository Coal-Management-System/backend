package net.jemsit.coal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import net.jemsit.coal.shared.domain.AnalysisStage;
import net.jemsit.coal.shared.domain.BatchStatus;
import net.jemsit.coal.shared.domain.OriginType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class BatchDtos {

    private BatchDtos() {
    }

    public record BatchDto(
            UUID id,
            String batchNumber,
            String productType,
            BatchStatus status,
            String coalGrade,
            String fraction,
            OriginType originType,
            String deposit,
            String miningSite,
            String quarry,
            String shiftPeriod,
            String supplier,
            Instant minedAt,
            BigDecimal initialWeight,
            BigDecimal reservedVolume,
            BigDecimal inProcessingVolume,
            BigDecimal shippedVolume,
            BigDecimal losses,
            BigDecimal currentRemainder,
            BigDecimal freeVolume,
            List<String> storageLocations,
            List<UUID> sourceBatchIds,
            List<String> sourceBatchNumbers,
            List<UUID> heapIds,
            List<String> heapNumbers,
            Instant createdAt) {
    }

    public record BatchCreateRequest(
            @NotBlank String batchNumber,
            String productType,
            String coalGrade,
            String fraction,
            OriginType originType,
            String deposit,
            String miningSite,
            String quarry,
            String shiftPeriod,
            String supplier,
            Instant minedAt,
            @NotNull @Positive BigDecimal initialWeight) {
    }

    public record QualityHistoryRow(
            Instant analysisDate,
            BigDecimal ash,
            BigDecimal moisture,
            BigDecimal calorific,
            AnalysisStage stage) {
    }
}
