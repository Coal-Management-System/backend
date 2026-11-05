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
            BigDecimal transferredVolume,
            BigDecimal currentRemainder,
            BigDecimal freeVolume,
            BigDecimal acceptedVolume,
            BigDecimal remainingVolume,
            List<String> storageLocations,
            List<UUID> sourceBatchIds,
            List<String> sourceBatchNumbers,
            List<UUID> pileIds,
            List<String> pileNumbers,
            Instant createdAt) {
    }

    /**
     * Party creation. The batch number is optional: left empty, the server assigns it from
     * the day's counter ({@code BatchNumberGenerator}) inside the create transaction. An
     * operator who needs a specific number — one already written on a paper document, say —
     * can send it explicitly, and it is then checked for duplicates like any typed number.
     */
    public record BatchCreateRequest(
            String batchNumber,
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

    public record BatchUpdateRequest(
            @NotBlank String batchNumber,
            String productType,
            @NotNull BatchStatus status,
            String coalGrade,
            String fraction,
            @NotNull OriginType originType,
            String deposit,
            String miningSite,
            String quarry,
            String shiftPeriod,
            String supplier,
            Instant minedAt,
            @NotNull @Positive BigDecimal initialWeight) {
    }

    /** What a party created right now would be numbered; an estimate, not a reservation. */
    public record NextNumberResponse(String batchNumber) {
    }

    public record QualityHistoryRow(
            Instant analysisDate,
            BigDecimal ash,
            BigDecimal moisture,
            BigDecimal calorific,
            AnalysisStage stage) {
    }
}
