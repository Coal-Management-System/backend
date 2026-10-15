package net.jemsit.coal.api.dto;

import jakarta.validation.constraints.NotBlank;
import net.jemsit.coal.shared.domain.WarehouseStatus;

import java.time.Instant;
import java.util.UUID;

public final class WarehouseDtos {

    private WarehouseDtos() {
    }

    public record WarehouseDto(
            UUID id,
            String number,
            String location,
            WarehouseStatus status,
            long pileCount,
            Instant createdAt) {
    }

    public record WarehouseCreateRequest(
            @NotBlank String number,
            String location,
            WarehouseStatus status) {
    }
}
