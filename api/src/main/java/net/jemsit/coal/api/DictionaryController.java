package net.jemsit.coal.api;

import jakarta.validation.Valid;
import net.jemsit.coal.api.dto.DictionaryDtos.AddRequest;
import net.jemsit.coal.api.dto.DictionaryDtos.EntryDto;
import net.jemsit.coal.api.service.DictionaryService;
import net.jemsit.coal.shared.domain.DictionaryField;
import net.jemsit.coal.api.service.ActivityLog;
import net.jemsit.coal.shared.domain.ActivityKind;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dictionary")
public class DictionaryController {

    private final DictionaryService service;
    private final ActivityLog activity;

    public DictionaryController(DictionaryService service, ActivityLog activity) {
        this.service = service;
        this.activity = activity;
    }

    @GetMapping
    public Map<DictionaryField, List<String>> list() {
        return service.grouped();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntryDto add(@Valid @RequestBody AddRequest req) {
        EntryDto e = service.add(req);
        activity.record(ActivityKind.CREATE, "DICTIONARY", e.value(),
                "Добавил в словарь " + e.field() + ": " + e.value());
        return e;
    }

    @DeleteMapping("/{field}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable DictionaryField field,
                       @RequestParam String value,
                       @RequestHeader(name = "X-Admin-Key", required = false) String adminKey,
                       @RequestParam(required = false) String confirmValue) {
        service.delete(field, value, adminKey, confirmValue);
        activity.record(ActivityKind.DELETE, "DICTIONARY", value, "Удалил из словаря " + field + ": " + value);
    }
}
