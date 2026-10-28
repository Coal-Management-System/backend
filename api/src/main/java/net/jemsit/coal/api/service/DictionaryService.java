package net.jemsit.coal.api.service;

import net.jemsit.coal.api.dto.DictionaryDtos.AddRequest;
import net.jemsit.coal.api.dto.DictionaryDtos.EntryDto;
import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.domain.DictionaryEntry;
import net.jemsit.coal.shared.domain.DictionaryField;
import net.jemsit.coal.shared.repo.DictionaryEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DictionaryService {

    private final DictionaryEntryRepository repo;

    public DictionaryService(DictionaryEntryRepository repo) {
        this.repo = repo;
    }

    /** Every word grouped by field, in a stable order, for typeahead and the browse page. */
    @Transactional(readOnly = true)
    public Map<DictionaryField, List<String>> grouped() {
        Map<DictionaryField, List<String>> out = new LinkedHashMap<>();
        for (DictionaryField field : DictionaryField.values()) {
            out.put(field, new ArrayList<>());
        }
        for (DictionaryEntry entry : repo.findAllByOrderByFieldAscValueAsc()) {
            out.get(entry.getField()).add(entry.getValue());
        }
        return out;
    }

    /** Registers a new word; an existing one (any case) is returned as-is, never duplicated. */
    @Transactional
    public EntryDto add(AddRequest req) {
        String value = req.value() == null ? "" : req.value().trim();
        if (value.isEmpty()) {
            throw ApiException.badRequest("Укажите значение");
        }
        return repo.findByFieldAndValueIgnoreCase(req.field(), value)
                .map(existing -> new EntryDto(existing.getField().name(), existing.getValue()))
                .orElseGet(() -> {
                    DictionaryEntry entry = new DictionaryEntry();
                    entry.setField(req.field());
                    entry.setValue(value);
                    entry = repo.save(entry);
                    return new EntryDto(entry.getField().name(), entry.getValue());
                });
    }
}
