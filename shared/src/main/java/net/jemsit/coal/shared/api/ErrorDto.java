package net.jemsit.coal.shared.api;

import java.time.Instant;

public record ErrorDto(Instant timestamp, int status, String message) {
}
