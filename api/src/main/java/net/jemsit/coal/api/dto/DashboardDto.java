package net.jemsit.coal.api.dto;

import java.math.BigDecimal;

public record DashboardDto(
        BigDecimal minedToday,
        BigDecimal transferredToProcessing,
        BigDecimal avgAsh,
        BigDecimal avgMoisture,
        BigDecimal avgCalorific,
        BigDecimal totalRemainder,
        BigDecimal freeVolume,
        BigDecimal reservedVolume,
        BigDecimal readyForShipment,
        BigDecimal losses,
        long batchesWithDeviations,
        BigDecimal concentrateYield,
        BigDecimal costPerTon,
        BigDecimal profitPerTon,
        long batchCount,
        long pileCount,
        long warehouseCount,
        long labCount) {
}
