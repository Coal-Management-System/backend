package net.jemsit.coal.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public final class PageDtos {

    private PageDtos() {
    }

    // Deliberately not a serialized Spring Page: Boot warns that PageImpl's JSON shape is unstable.
    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {

        public static <E, T> PageResponse<T> of(Page<E> source, Function<? super E, T> mapper) {
            List<T> content = source.getContent().stream().map(mapper).toList();
            return new PageResponse<>(
                    content,
                    source.getNumber(),
                    source.getSize(),
                    source.getTotalElements(),
                    source.getTotalPages());
        }
    }
}
