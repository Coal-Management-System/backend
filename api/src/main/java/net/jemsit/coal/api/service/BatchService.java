package net.jemsit.coal.api.service;

import net.jemsit.coal.api.dto.BatchDtos.BatchCreateRequest;
import net.jemsit.coal.api.dto.BatchDtos.BatchDto;
import net.jemsit.coal.api.dto.BatchDtos.BatchUpdateRequest;
import net.jemsit.coal.api.dto.BatchDtos.QualityHistoryRow;
import net.jemsit.coal.api.dto.LabDtos.AnalysisValueDto;
import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.domain.CoalBatch;
import net.jemsit.coal.shared.domain.Indicator;
import net.jemsit.coal.shared.domain.LabAnalysis;
import net.jemsit.coal.shared.domain.LabAnalysisValue;
import net.jemsit.coal.shared.domain.BatchStatus;
import net.jemsit.coal.shared.domain.OriginType;
import net.jemsit.coal.shared.repo.CoalBatchRepository;
import net.jemsit.coal.shared.repo.LabAnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BatchService {

    private final CoalBatchRepository batches;
    private final LabAnalysisRepository analyses;

    public BatchService(CoalBatchRepository batches, LabAnalysisRepository analyses) {
        this.batches = batches;
        this.analyses = analyses;
    }

    @Transactional(readOnly = true)
    public List<BatchDto> list() {
        return batches.findAllByOrderByBatchNumberAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BatchDto get(UUID id) {
        return toDto(load(id));
    }

    @Transactional
    public BatchDto create(BatchCreateRequest req) {
        if (batches.existsByBatchNumber(req.batchNumber())) {
            throw ApiException.conflict("Партия с номером " + req.batchNumber() + " уже существует");
        }
        CoalBatch b = new CoalBatch();
        b.setBatchNumber(req.batchNumber());
        b.setProductType(req.productType());
        b.setCoalGrade(req.coalGrade());
        b.setFraction(req.fraction());
        b.setOriginType(req.originType() == null ? OriginType.OWN_MINING : req.originType());
        b.setDeposit(req.deposit());
        b.setMiningSite(req.miningSite());
        b.setQuarry(req.quarry());
        b.setShiftPeriod(req.shiftPeriod());
        b.setSupplier(req.supplier());
        b.setMinedAt(req.minedAt());
        b.setInitialWeight(req.initialWeight());
        b.setStatus(BatchStatus.NEW);
        return toDto(batches.save(b));
    }

    @Transactional
    public BatchDto update(UUID id, BatchUpdateRequest req) {
        CoalBatch b = load(id);
        if (!b.getBatchNumber().equals(req.batchNumber())
                && batches.existsByBatchNumber(req.batchNumber())) {
            throw ApiException.conflict("Партия с номером " + req.batchNumber() + " уже существует");
        }
        b.setBatchNumber(req.batchNumber());
        b.setProductType(req.productType());
        b.setStatus(req.status());
        b.setCoalGrade(req.coalGrade());
        b.setFraction(req.fraction());
        b.setOriginType(req.originType());
        b.setDeposit(req.deposit());
        b.setMiningSite(req.miningSite());
        b.setQuarry(req.quarry());
        b.setShiftPeriod(req.shiftPeriod());
        b.setSupplier(req.supplier());
        b.setMinedAt(req.minedAt());
        b.setInitialWeight(req.initialWeight());
        return toDto(batches.save(b));
    }

    @Transactional(readOnly = true)
    public List<QualityHistoryRow> qualityHistory(UUID batchId) {
        load(batchId);
        return analyses.findByBatchIdOrderByAnalysisDateAsc(batchId).stream()
                .map(a -> {
                    Map<Indicator, BigDecimal> byIndicator = a.getValues().stream()
                            .collect(Collectors.toMap(LabAnalysisValue::getIndicator, LabAnalysisValue::getValue, (x, y) -> x));
                    return new QualityHistoryRow(
                            a.getAnalysisDate(),
                            byIndicator.get(Indicator.ASH),
                            byIndicator.get(Indicator.MOISTURE),
                            byIndicator.get(Indicator.CALORIFIC),
                            a.getStage());
                })
                .toList();
    }

    public CoalBatch load(UUID id) {
        return batches.findById(id)
                .orElseThrow(() -> ApiException.notFound("Партия не найдена: " + id));
    }

    public BatchDto toDto(CoalBatch b) {
        return new BatchDto(
                b.getId(),
                b.getBatchNumber(),
                b.getProductType(),
                b.getStatus(),
                b.getCoalGrade(),
                b.getFraction(),
                b.getOriginType(),
                b.getDeposit(),
                b.getMiningSite(),
                b.getQuarry(),
                b.getShiftPeriod(),
                b.getSupplier(),
                b.getMinedAt(),
                b.getInitialWeight(),
                b.getReservedVolume(),
                b.getInProcessingVolume(),
                b.getShippedVolume(),
                b.getLosses(),
                b.currentRemainder(),
                b.freeVolume(),
                b.getHeaps().stream()
                        .map(h -> h.getWarehouse() == null ? null : h.getWarehouse().getNumber())
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList(),
                b.getSourceBatches().stream().map(CoalBatch::getId).toList(),
                b.getSourceBatches().stream().map(CoalBatch::getBatchNumber).toList(),
                b.getHeaps().stream().map(h -> h.getId()).toList(),
                b.getHeaps().stream().map(h -> h.getHeapNumber()).toList(),
                b.getCreatedAt());
    }

    static AnalysisValueDto valueToDto(LabAnalysisValue v) {
        return new AnalysisValueDto(v.getIndicator(), v.getValue(), v.getUnit());
    }

    static List<AnalysisValueDto> valuesToDto(LabAnalysis a) {
        return a.getValues().stream().map(BatchService::valueToDto).toList();
    }
}
