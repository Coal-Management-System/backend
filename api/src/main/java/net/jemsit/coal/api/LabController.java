package net.jemsit.coal.api;

import jakarta.validation.Valid;
import net.jemsit.coal.api.dto.LabDtos.AnalysisCreateRequest;
import net.jemsit.coal.api.dto.LabDtos.AnalysisDto;
import net.jemsit.coal.api.dto.LabDtos.LabCreateRequest;
import net.jemsit.coal.api.dto.LabDtos.LabDto;
import net.jemsit.coal.api.service.LabService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    public LabController(LabService service) {
        this.service = service;
    }

    @GetMapping("/labs")
    public List<LabDto> labs() {
        return service.labs();
    }

    @PostMapping("/labs")
    @ResponseStatus(HttpStatus.CREATED)
    public LabDto createLab(@Valid @RequestBody LabCreateRequest req) {
        return service.createLab(req);
    }

    @GetMapping("/lab-analyses")
    public List<AnalysisDto> analyses(@RequestParam(required = false) UUID batchId) {
        return service.analyses(batchId);
    }

    @PostMapping("/lab-analyses")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisDto createAnalysis(@Valid @RequestBody AnalysisCreateRequest req) {
        return service.createAnalysis(req);
    }
}
