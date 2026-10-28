package net.jemsit.coal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.jemsit.coal.shared.domain.DictionaryField;

public final class DictionaryDtos {

    private DictionaryDtos() {
    }

    public record EntryDto(String field, String value) {
    }

    public record AddRequest(
            @NotNull DictionaryField field,
            @NotBlank @Size(max = 120) String value) {
    }
}
