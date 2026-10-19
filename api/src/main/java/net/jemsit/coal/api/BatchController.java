package net.jemsit.coal.api;

import jakarta.validation.Valid;
import net.jemsit.coal.api.dto.BatchDtos.BatchCreateRequest;
import net.jemsit.coal.api.dto.BatchDtos.BatchDto;
import net.jemsit.coal.api.dto.BatchDtos.BatchUpdateRequest;
import net.jemsit.coal.api.dto.BatchDtos.QualityHistoryRow;
import net.jemsit.coal.api.dto.LabDtos.AnalysisDto;
import net.jemsit.coal.api.dto.OptionDtos.OptionDto;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.api.service.BatchService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchService service;

    public BatchController(BatchService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<BatchDto> list(
            @PageableDefault(size = 10, sort = {"batchNumber", "id"}) Pageable pageable) {
        return service.list(pageable);
    }

    // Literal path segments outrank the /{id} template, so these never reach the UUID converter.
    @GetMapping("/options")
    public List<OptionDto> options() {
        return service.options();
    }

    @GetMapping("/by-number/{number}")
    public BatchDto byNumber(@PathVariable String number) {
        return service.byNumber(number);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BatchDto create(@Valid @RequestBody BatchCreateRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public BatchDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public BatchDto update(@PathVariable UUID id, @Valid @RequestBody BatchUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,
                       @RequestHeader(name = "X-Admin-Key", required = false) String adminKey,
                       @RequestParam(required = false) String confirmNumber) {
        service.delete(id, adminKey, confirmNumber);
    }

    @GetMapping("/{id}/quality-history")
    public List<QualityHistoryRow> qualityHistory(@PathVariable UUID id) {
        return service.qualityHistory(id);
    }

    @GetMapping("/{id}/analyses")
    public List<AnalysisDto> analyses(@PathVariable UUID id) {
        return service.analysesOf(id);
    }
}
