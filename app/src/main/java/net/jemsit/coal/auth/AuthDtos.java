package net.jemsit.coal.auth;

import jakarta.validation.constraints.NotBlank;
import net.jemsit.coal.shared.domain.UserRole;
import net.jemsit.coal.shared.domain.UserShift;

import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {
    }

    public record UserDto(UUID id, String username, String fullName, UserRole role, UserShift shift) {
    }

    public record LoginResponse(String token, UserDto user) {
    }
}
