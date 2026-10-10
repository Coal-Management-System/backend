package net.jemsit.coal.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "coal_heap")
public class CoalHeap extends BaseEntity {

    @Column(name = "heap_number", nullable = false, unique = true, length = 50)
    private String heapNumber;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal volume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HeapStatus status = HeapStatus.ACTIVE;

    @Column(name = "origin_source", length = 200)
    private String originSource;

    @ManyToMany(mappedBy = "heaps")
    private Set<CoalBatch> batches = new LinkedHashSet<>();

    public String getHeapNumber() {
        return heapNumber;
    }

    public void setHeapNumber(String heapNumber) {
        this.heapNumber = heapNumber;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public HeapStatus getStatus() {
        return status;
    }

    public void setStatus(HeapStatus status) {
        this.status = status;
    }

    public String getOriginSource() {
        return originSource;
    }

    public void setOriginSource(String originSource) {
        this.originSource = originSource;
    }

    public Set<CoalBatch> getBatches() {
        return batches;
    }
}
