package net.jemsit.coal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import net.jemsit.coal.shared.domain.ActivityKind;

import java.time.Instant;
import java.util.UUID;

public final class ActivityDtos {

    private ActivityDtos() {
    }

    public record ActivityDto(
            UUID id,
            Instant createdAt,
            String username,
            String fullName,
            ActivityKind kind,
            String entityType,
            String entityLabel,
            String summary) {
    }

    public record VisitRequest(
            @NotBlank @Size(max = 200) String path,
            @Size(max = 120) String title) {
    }
}
