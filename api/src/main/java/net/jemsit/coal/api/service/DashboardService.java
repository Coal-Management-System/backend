package net.jemsit.coal.api.service;

import net.jemsit.coal.api.dto.DashboardDto;
import net.jemsit.coal.shared.domain.BatchStatus;
import net.jemsit.coal.shared.domain.CoalBatch;
import net.jemsit.coal.shared.domain.Indicator;
import net.jemsit.coal.shared.domain.LabAnalysis;
import net.jemsit.coal.shared.domain.LabAnalysisValue;
import net.jemsit.coal.shared.domain.OriginType;
import net.jemsit.coal.shared.repo.CoalBatchRepository;
import net.jemsit.coal.shared.repo.CoalHeapRepository;
import net.jemsit.coal.shared.repo.LabAnalysisRepository;
import net.jemsit.coal.shared.repo.LaboratoryRepository;
import net.jemsit.coal.shared.repo.WarehouseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService {

    private final CoalBatchRepository batches;
    private final CoalHeapRepository heaps;
    private final WarehouseRepository warehouses;
    private final LaboratoryRepository labs;
    private final LabAnalysisRepository analyses;
    private final BigDecimal maxAsh;
    private final BigDecimal maxMoisture;

    public DashboardService(CoalBatchRepository batches,
                            CoalHeapRepository heaps,
                            WarehouseRepository warehouses,
                            LaboratoryRepository labs,
                            LabAnalysisRepository analyses,
                            @Value("${coal.quality.max-ash:20}") BigDecimal maxAsh,
                            @Value("${coal.quality.max-moisture:12}") BigDecimal maxMoisture) {
        this.batches = batches;
        this.heaps = heaps;
        this.warehouses = warehouses;
        this.labs = labs;
        this.analyses = analyses;
        this.maxAsh = maxAsh;
        this.maxMoisture = maxMoisture;
    }

    @Transactional(readOnly = true)
    public DashboardDto compute() {
        List<CoalBatch> all = batches.findAll();
        Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal minedToday = batches.sumInitialWeightMinedAfter(OriginType.OWN_MINING, todayStart);

        BigDecimal transferredToProcessing = sum(all, CoalBatch::getInProcessingVolume);
        BigDecimal totalRemainder = sum(all, CoalBatch::currentRemainder);
        BigDecimal freeVolume = sum(all, CoalBatch::freeVolume);
        BigDecimal reservedVolume = sum(all, CoalBatch::getReservedVolume);
        BigDecimal losses = sum(all, CoalBatch::getLosses);
        BigDecimal readyForShipment = all.stream()
                .filter(b -> b.getStatus() == BatchStatus.READY_FOR_SHIPMENT)
                .map(CoalBatch::currentRemainder)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<UUID, LabAnalysis> latestByBatch = new HashMap<>();
        analyses.findAllByOrderByAnalysisDateDesc().forEach(a ->
                latestByBatch.putIfAbsent(a.getBatch().getId(), a));

        BigDecimal avgAsh = averageIndicator(latestByBatch, Indicator.ASH);
        BigDecimal avgMoisture = averageIndicator(latestByBatch, Indicator.MOISTURE);
        BigDecimal avgCalorific = averageIndicator(latestByBatch, Indicator.CALORIFIC);

        long deviations = latestByBatch.values().stream().filter(a -> {
            BigDecimal ash = valueOf(a, Indicator.ASH);
            BigDecimal moisture = valueOf(a, Indicator.MOISTURE);
            return (ash != null && ash.compareTo(maxAsh) > 0)
                    || (moisture != null && moisture.compareTo(maxMoisture) > 0);
        }).count();

        return new DashboardDto(
                minedToday,
                transferredToProcessing,
                avgAsh,
                avgMoisture,
                avgCalorific,
                totalRemainder,
                freeVolume,
                reservedVolume,
                readyForShipment,
                losses,
                deviations,
                null,
                null,
                null,
                all.size(),
                heaps.count(),
                warehouses.count(),
                labs.count());
    }

    private BigDecimal sum(List<CoalBatch> all, java.util.function.Function<CoalBatch, BigDecimal> f) {
        return all.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal valueOf(LabAnalysis a, Indicator indicator) {
        return a.getValues().stream()
                .filter(v -> v.getIndicator() == indicator)
                .map(LabAnalysisValue::getValue)
                .findFirst().orElse(null);
    }

    private BigDecimal averageIndicator(Map<UUID, LabAnalysis> latestByBatch, Indicator indicator) {
        List<BigDecimal> values = new ArrayList<>();
        latestByBatch.values().forEach(a -> {
            BigDecimal v = valueOf(a, indicator);
            if (v != null) {
                values.add(v);
            }
        });
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }
}
