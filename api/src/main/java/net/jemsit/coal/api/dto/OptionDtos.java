package net.jemsit.coal.api.dto;

import java.util.UUID;

public final class OptionDtos {

    private OptionDtos() {
    }

    /** Unpaged {id, label} pair for select boxes, so a paged collection never truncates a dropdown. */
    public record OptionDto(UUID id, String label) {
    }
}
