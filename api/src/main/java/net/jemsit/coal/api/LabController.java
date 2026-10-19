package net.jemsit.coal.api;

import jakarta.validation.Valid;
import net.jemsit.coal.api.dto.LabDtos.AnalysisCreateRequest;
import net.jemsit.coal.api.dto.LabDtos.AnalysisDto;
import net.jemsit.coal.api.dto.LabDtos.LabCreateRequest;
import net.jemsit.coal.api.dto.LabDtos.LabDto;
import net.jemsit.coal.api.dto.OptionDtos.OptionDto;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.api.service.LabService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LabController {

    private final LabService service;

    public LabController(LabService service) {
        this.service = service;
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
        return service.createLab(req);
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
        return service.createAnalysis(req);
    }
}
