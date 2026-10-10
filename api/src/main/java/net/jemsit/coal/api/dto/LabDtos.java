package net.jemsit.coal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.jemsit.coal.shared.domain.AnalysisStage;
import net.jemsit.coal.shared.domain.AnalysisStatus;
import net.jemsit.coal.shared.domain.Indicator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class LabDtos {

    private LabDtos() {
    }

    public record LabDto(UUID id, String name) {
    }

    public record LabCreateRequest(@NotBlank String name) {
    }

    public record AnalysisValueDto(Indicator indicator, BigDecimal value, String unit) {
    }

    public record AnalysisValueInput(@NotNull Indicator indicator, @NotNull BigDecimal value, String unit) {
    }

    public record AnalysisDto(
            UUID id,
            String sampleNumber,
            UUID batchId,
            String batchNumber,
            UUID labId,
            String labName,
            String protocolNumber,
            String responsibleEmployee,
            String method,
            Instant analysisDate,
            AnalysisStage stage,
            AnalysisStatus status,
            List<AnalysisValueDto> values,
            Instant createdAt) {
    }

    public record AnalysisCreateRequest(
            @NotBlank String sampleNumber,
            @NotNull UUID batchId,
            @NotNull UUID labId,
            String protocolNumber,
            String responsibleEmployee,
            String method,
            Instant analysisDate,
            AnalysisStage stage,
            List<AnalysisValueInput> values) {
    }
}
