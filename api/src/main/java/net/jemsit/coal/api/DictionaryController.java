package net.jemsit.coal.api;

import jakarta.validation.Valid;
import net.jemsit.coal.api.dto.DictionaryDtos.AddRequest;
import net.jemsit.coal.api.dto.DictionaryDtos.EntryDto;
import net.jemsit.coal.api.service.DictionaryService;
import net.jemsit.coal.shared.domain.DictionaryField;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dictionary")
public class DictionaryController {

    private final DictionaryService service;

    public DictionaryController(DictionaryService service) {
        this.service = service;
    }

    @GetMapping
    public Map<DictionaryField, List<String>> list() {
        return service.grouped();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntryDto add(@Valid @RequestBody AddRequest req) {
        return service.add(req);
    }
}
