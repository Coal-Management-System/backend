package net.jemsit.coal.api;

import jakarta.validation.Valid;
import net.jemsit.coal.api.dto.LabDtos.AnalysisCreateRequest;
import net.jemsit.coal.api.dto.LabDtos.AnalysisDto;
import net.jemsit.coal.api.dto.LabDtos.LabCreateRequest;
import net.jemsit.coal.api.dto.LabDtos.LabDto;
import net.jemsit.coal.api.dto.OptionDtos.OptionDto;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.api.service.LabService;
import net.jemsit.coal.api.service.ActivityLog;
import net.jemsit.coal.shared.domain.ActivityKind;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class LabController {

    private final LabService service;
    private final ActivityLog activity;

    public LabController(LabService service, ActivityLog activity) {
        this.service = service;
        this.activity = activity;
    }

    @GetMapping("/labs")
    public PageResponse<LabDto> labs(
            @PageableDefault(size = 10, sort = {"name", "id"}) Pageable pageable) {
        return service.labs(pageable);
    }

    @GetMapping("/labs/options")
    public List<OptionDto> labOptions() {
        return service.options();
    }

    @PostMapping("/labs")
    @ResponseStatus(HttpStatus.CREATED)
    public LabDto createLab(@Valid @RequestBody LabCreateRequest req) {
        LabDto lab = service.createLab(req);
        activity.record(ActivityKind.CREATE, "LAB", lab.name(), "Создал лабораторию " + lab.name());
        return lab;
    }

    @DeleteMapping("/labs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLab(@PathVariable UUID id,
                          @RequestHeader(name = "X-Admin-Key", required = false) String adminKey,
                          @RequestParam(required = false) String confirmName) {
        service.deleteLab(id, adminKey, confirmName);
        activity.record(ActivityKind.DELETE, "LAB", confirmName, "Удалил лабораторию " + confirmName);
    }

    @GetMapping("/lab-analyses")
    public PageResponse<AnalysisDto> analyses(
            @PageableDefault(size = 10, sort = {"analysisDate", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return service.analyses(pageable);
    }

    @PostMapping("/lab-analyses")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisDto createAnalysis(@Valid @RequestBody AnalysisCreateRequest req) {
        AnalysisDto a = service.createAnalysis(req);
        activity.record(ActivityKind.CREATE, "ANALYSIS", a.sampleNumber(),
                "Добавил анализ " + a.sampleNumber() + " для партии " + a.batchNumber());
        return a;
    }

    @DeleteMapping("/lab-analyses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnalysis(@PathVariable UUID id,
                               @RequestHeader(name = "X-Admin-Key", required = false) String adminKey,
                               @RequestParam(required = false) String confirmNumber) {
        service.deleteAnalysis(id, adminKey, confirmNumber);
        activity.record(ActivityKind.DELETE, "ANALYSIS", confirmNumber, "Удалил анализ " + confirmNumber);
    }
}
