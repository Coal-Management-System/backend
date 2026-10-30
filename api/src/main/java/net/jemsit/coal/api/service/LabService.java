package net.jemsit.coal.api.service;

import net.jemsit.coal.api.dto.LabDtos.AnalysisCreateRequest;
import net.jemsit.coal.api.dto.LabDtos.AnalysisDto;
import net.jemsit.coal.api.dto.LabDtos.AnalysisValueInput;
import net.jemsit.coal.api.dto.LabDtos.LabCreateRequest;
import net.jemsit.coal.api.dto.LabDtos.LabDto;
import net.jemsit.coal.api.dto.OptionDtos.OptionDto;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.domain.AnalysisStage;
import net.jemsit.coal.shared.domain.AnalysisStatus;
import net.jemsit.coal.shared.domain.LabAnalysis;
import net.jemsit.coal.shared.domain.LabAnalysisValue;
import net.jemsit.coal.shared.domain.Laboratory;
import net.jemsit.coal.shared.repo.CoalBatchRepository;
import net.jemsit.coal.shared.repo.LabAnalysisRepository;
import net.jemsit.coal.shared.repo.LaboratoryRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LabService {

    private final LaboratoryRepository labs;
    private final LabAnalysisRepository analyses;
    private final CoalBatchRepository batches;
    private final AdminKeyGuard guard;

    public LabService(LaboratoryRepository labs, LabAnalysisRepository analyses,
                      CoalBatchRepository batches, AdminKeyGuard guard) {
        this.labs = labs;
        this.analyses = analyses;
        this.batches = batches;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public PageResponse<LabDto> labs(Pageable pageable) {
        return PageResponse.of(labs.findAll(pageable), l -> new LabDto(l.getId(), l.getName()));
    }

    @Transactional(readOnly = true)
    public List<OptionDto> options() {
        return labs.findAll(Sort.by("name", "id")).stream()
                .map(l -> new OptionDto(l.getId(), l.getName()))
                .toList();
    }

    @Transactional
    public LabDto createLab(LabCreateRequest req) {
        Laboratory lab = new Laboratory();
        lab.setName(req.name());
        return new LabDto(labs.save(lab).getId(), lab.getName());
    }

    @Transactional(readOnly = true)
    public PageResponse<AnalysisDto> analyses(Pageable pageable) {
        return PageResponse.of(analyses.findAll(pageable), this::toDto);
    }

    @Transactional
    public AnalysisDto createAnalysis(AnalysisCreateRequest req) {
        LabAnalysis a = new LabAnalysis();
        a.setSampleNumber(req.sampleNumber());
        a.setBatch(batches.findById(req.batchId())
                .orElseThrow(() -> ApiException.notFound("Партия не найдена: " + req.batchId())));
        a.setLab(labs.findById(req.labId())
                .orElseThrow(() -> ApiException.notFound("Лаборатория не найдена: " + req.labId())));
        a.setProtocolNumber(req.protocolNumber());
        a.setResponsibleEmployee(req.responsibleEmployee());
        a.setMethod(req.method());
        a.setAnalysisDate(req.analysisDate());
        a.setStage(req.stage() == null ? AnalysisStage.INITIAL : req.stage());
        a.setStatus(AnalysisStatus.UPLOADED);
        if (req.values() != null) {
            for (AnalysisValueInput in : req.values()) {
                LabAnalysisValue v = new LabAnalysisValue();
                v.setAnalysis(a);
                v.setIndicator(in.indicator());
                v.setValue(in.value());
                v.setUnit(in.unit());
                a.getValues().add(v);
            }
        }
        return toDto(analyses.save(a));
    }

    /**
     * Permanently deletes a laboratory. Requires the admin key and the lab name
     * typed back. Blocked while any analysis still references it (lab_id is
     * NOT NULL), so results must be removed first.
     */
    @Transactional
    public void deleteLab(UUID id, String adminKey, String confirmName) {
        guard.check(adminKey);
        Laboratory lab = labs.findById(id)
                .orElseThrow(() -> ApiException.notFound("Лаборатория не найдена: " + id));
        if (!lab.getName().equals(confirmName)) {
            throw ApiException.badRequest("Название лаборатории для подтверждения не совпадает");
        }
        if (analyses.existsByLabId(id)) {
            throw ApiException.conflict("По лаборатории есть результаты анализов, удаление невозможно");
        }
        labs.delete(lab);
    }

    /**
     * Permanently deletes one analysis result. Requires the admin key and the
     * sample number typed back. Its indicator values cascade away with it.
     */
    @Transactional
    public void deleteAnalysis(UUID id, String adminKey, String confirmNumber) {
        guard.check(adminKey);
        LabAnalysis a = analyses.findById(id)
                .orElseThrow(() -> ApiException.notFound("Результат анализа не найден: " + id));
        if (!a.getSampleNumber().equals(confirmNumber)) {
            throw ApiException.badRequest("Номер пробы для подтверждения не совпадает");
        }
        analyses.delete(a);
    }

    public AnalysisDto toDto(LabAnalysis a) {
        return new AnalysisDto(
                a.getId(),
                a.getSampleNumber(),
                a.getBatch().getId(),
                a.getBatch().getBatchNumber(),
                a.getLab().getId(),
                a.getLab().getName(),
                a.getProtocolNumber(),
                a.getResponsibleEmployee(),
                a.getMethod(),
                a.getAnalysisDate(),
                a.getStage(),
                a.getStatus(),
                BatchService.valuesToDto(a),
                a.getCreatedAt());
    }
}
