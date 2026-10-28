package net.jemsit.coal.shared.repo;

import net.jemsit.coal.shared.domain.DictionaryEntry;
import net.jemsit.coal.shared.domain.DictionaryField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntry, UUID> {

    List<DictionaryEntry> findAllByOrderByFieldAscValueAsc();

    /** Case-insensitive so "уголь" and "Уголь" never become two words. */
    Optional<DictionaryEntry> findByFieldAndValueIgnoreCase(DictionaryField field, String value);
}
