package net.jemsit.coal.api.service;

import net.jemsit.coal.api.dto.BatchDtos.BatchCreateRequest;
import net.jemsit.coal.api.dto.BatchDtos.BatchDto;
import net.jemsit.coal.api.dto.BatchDtos.BatchUpdateRequest;
import net.jemsit.coal.api.dto.BatchDtos.QualityHistoryRow;
import net.jemsit.coal.api.dto.LabDtos.AnalysisDto;
import net.jemsit.coal.api.dto.LabDtos.AnalysisValueDto;
import net.jemsit.coal.api.dto.OptionDtos.OptionDto;
import net.jemsit.coal.api.dto.PageDtos.PageResponse;
import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.domain.CoalBatch;
import net.jemsit.coal.shared.domain.Indicator;
import net.jemsit.coal.shared.domain.LabAnalysis;
import net.jemsit.coal.shared.domain.LabAnalysisValue;
import net.jemsit.coal.shared.domain.BatchStatus;
import net.jemsit.coal.shared.domain.OriginType;
import net.jemsit.coal.shared.repo.CoalBatchRepository;
import net.jemsit.coal.shared.repo.LabAnalysisRepository;
import net.jemsit.coal.shared.repo.MovementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BatchService {

    private final CoalBatchRepository batches;
    private final LabAnalysisRepository analyses;
    private final MovementRepository movements;
    private final LabService labService;
    private final String deleteKey;

    public BatchService(CoalBatchRepository batches, LabAnalysisRepository analyses,
                        MovementRepository movements, LabService labService,
                        @Value("${coal.admin.delete-key:}") String deleteKey) {
        this.batches = batches;
        this.analyses = analyses;
        this.movements = movements;
        this.labService = labService;
        this.deleteKey = deleteKey;
    }

    @Transactional(readOnly = true)
    public PageResponse<BatchDto> list(Pageable pageable) {
        return PageResponse.of(batches.findAll(pageable), this::toDto);
    }

    @Transactional(readOnly = true)
    public List<OptionDto> options() {
        return batches.findAll(Sort.by("batchNumber", "id")).stream()
                .map(b -> new OptionDto(b.getId(), b.getBatchNumber()))
                .toList();
    }

    @Transactional(readOnly = true)
    public BatchDto byNumber(String number) {
        return toDto(batches.findByBatchNumberIgnoreCase(number)
                .orElseThrow(() -> ApiException.notFound("Партия «" + number + "» не найдена")));
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

    /**
     * Permanently deletes a batch. Requires the admin key, the batch number typed back
     * as confirmation, and a batch with nothing referencing it.
     */
    @Transactional
    public void delete(UUID id, String adminKey, String confirmNumber) {
        if (deleteKey.isBlank()) {
            throw ApiException.forbidden("Удаление партий отключено на сервере");
        }
        if (adminKey == null || !MessageDigest.isEqual(
                deleteKey.getBytes(StandardCharsets.UTF_8), adminKey.getBytes(StandardCharsets.UTF_8))) {
            throw ApiException.forbidden("Неверный ключ администратора");
        }
        CoalBatch b = load(id);
        if (!b.getBatchNumber().equals(confirmNumber)) {
            throw ApiException.badRequest("Номер партии для подтверждения не совпадает");
        }
        if (!b.getPiles().isEmpty()) {
            throw ApiException.conflict("Партия находится в штабелях, удаление невозможно");
        }
        if (movements.existsByBatchId(id)) {
            throw ApiException.conflict("По партии есть движения, удаление невозможно");
        }
        if (analyses.existsByBatchId(id)) {
            throw ApiException.conflict("По партии есть лабораторные анализы, удаление невозможно");
        }
        if (batches.existsBySourceBatchesId(id)) {
            throw ApiException.conflict("Партия является источником других партий, удаление невозможно");
        }
        b.getSourceBatches().clear();
        batches.delete(b);
    }

    @Transactional(readOnly = true)
    public List<AnalysisDto> analysesOf(UUID batchId) {
        load(batchId);
        return analyses.findByBatchIdOrderByAnalysisDateAsc(batchId).stream()
                .map(labService::toDto)
                .toList();
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
                b.getPiles().stream()
                        .map(p -> p.getWarehouse() == null ? null : p.getWarehouse().getNumber())
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList(),
                b.getSourceBatches().stream().map(CoalBatch::getId).toList(),
                b.getSourceBatches().stream().map(CoalBatch::getBatchNumber).toList(),
                b.getPiles().stream().map(p -> p.getId()).toList(),
                b.getPiles().stream().map(p -> p.getPileNumber()).toList(),
                b.getCreatedAt());
    }

    static AnalysisValueDto valueToDto(LabAnalysisValue v) {
        return new AnalysisValueDto(v.getIndicator(), v.getValue(), v.getUnit());
    }

    static List<AnalysisValueDto> valuesToDto(LabAnalysis a) {
        return a.getValues().stream().map(BatchService::valueToDto).toList();
    }
}
