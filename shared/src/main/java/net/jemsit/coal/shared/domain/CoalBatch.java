package net.jemsit.coal.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "coal_batch")
public class CoalBatch extends BaseEntity {

    @Column(name = "batch_number", nullable = false, unique = true, length = 50)
    private String batchNumber;

    @Column(name = "product_type", length = 100)
    private String productType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BatchStatus status = BatchStatus.NEW;

    @Column(name = "coal_grade", length = 50)
    private String coalGrade;

    @Column(length = 50)
    private String fraction;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", nullable = false, length = 20)
    private OriginType originType = OriginType.OWN_MINING;

    @Column(length = 100)
    private String deposit;

    @Column(name = "mining_site", length = 100)
    private String miningSite;

    @Column(length = 100)
    private String quarry;

    @Column(name = "shift_period", length = 100)
    private String shiftPeriod;

    @Column(length = 200)
    private String supplier;

    @Column(name = "mined_at")
    private Instant minedAt;

    @Column(name = "initial_weight", nullable = false, precision = 14, scale = 2)
    private BigDecimal initialWeight;

    @Column(name = "reserved_volume", nullable = false, precision = 14, scale = 2)
    private BigDecimal reservedVolume = BigDecimal.ZERO;

    @Column(name = "in_processing_volume", nullable = false, precision = 14, scale = 2)
    private BigDecimal inProcessingVolume = BigDecimal.ZERO;

    @Column(name = "shipped_volume", nullable = false, precision = 14, scale = 2)
    private BigDecimal shippedVolume = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal losses = BigDecimal.ZERO;

    @ManyToMany
    @JoinTable(name = "coal_batch_source",
            joinColumns = @JoinColumn(name = "batch_id"),
            inverseJoinColumns = @JoinColumn(name = "source_batch_id"))
    private Set<CoalBatch> sourceBatches = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(name = "coal_batch_heap",
            joinColumns = @JoinColumn(name = "batch_id"),
            inverseJoinColumns = @JoinColumn(name = "heap_id"))
    private Set<CoalHeap> heaps = new LinkedHashSet<>();

    public BigDecimal currentRemainder() {
        return initialWeight.subtract(shippedVolume).subtract(losses);
    }

    public BigDecimal freeVolume() {
        return currentRemainder().subtract(reservedVolume).subtract(inProcessingVolume);
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public String getCoalGrade() {
        return coalGrade;
    }

    public void setCoalGrade(String coalGrade) {
        this.coalGrade = coalGrade;
    }

    public String getFraction() {
        return fraction;
    }

    public void setFraction(String fraction) {
        this.fraction = fraction;
    }

    public OriginType getOriginType() {
        return originType;
    }

    public void setOriginType(OriginType originType) {
        this.originType = originType;
    }

    public String getDeposit() {
        return deposit;
    }

    public void setDeposit(String deposit) {
        this.deposit = deposit;
    }

    public String getMiningSite() {
        return miningSite;
    }

    public void setMiningSite(String miningSite) {
        this.miningSite = miningSite;
    }

    public String getQuarry() {
        return quarry;
    }

    public void setQuarry(String quarry) {
        this.quarry = quarry;
    }

    public String getShiftPeriod() {
        return shiftPeriod;
    }

    public void setShiftPeriod(String shiftPeriod) {
        this.shiftPeriod = shiftPeriod;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public Instant getMinedAt() {
        return minedAt;
    }

    public void setMinedAt(Instant minedAt) {
        this.minedAt = minedAt;
    }

    public BigDecimal getInitialWeight() {
        return initialWeight;
    }

    public void setInitialWeight(BigDecimal initialWeight) {
        this.initialWeight = initialWeight;
    }

    public BigDecimal getReservedVolume() {
        return reservedVolume;
    }

    public void setReservedVolume(BigDecimal reservedVolume) {
        this.reservedVolume = reservedVolume;
    }

    public BigDecimal getInProcessingVolume() {
        return inProcessingVolume;
    }

    public void setInProcessingVolume(BigDecimal inProcessingVolume) {
        this.inProcessingVolume = inProcessingVolume;
    }

    public BigDecimal getShippedVolume() {
        return shippedVolume;
    }

    public void setShippedVolume(BigDecimal shippedVolume) {
        this.shippedVolume = shippedVolume;
    }

    public BigDecimal getLosses() {
        return losses;
    }

    public void setLosses(BigDecimal losses) {
        this.losses = losses;
    }

    public Set<CoalBatch> getSourceBatches() {
        return sourceBatches;
    }

    public Set<CoalHeap> getHeaps() {
        return heaps;
    }
}
