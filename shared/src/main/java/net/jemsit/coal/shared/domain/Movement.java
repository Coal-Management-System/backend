package net.jemsit.coal.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "coal_movement")
public class Movement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private CoalBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "heap_id")
    private CoalHeap heap;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MovementOperation operation;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal volume;

    @Column(name = "from_location", length = 200)
    private String fromLocation;

    @Column(name = "to_location", length = 200)
    private String toLocation;

    @Column(name = "origin_source", length = 200)
    private String originSource;

    public CoalBatch getBatch() {
        return batch;
    }

    public void setBatch(CoalBatch batch) {
        this.batch = batch;
    }

    public CoalHeap getHeap() {
        return heap;
    }

    public void setHeap(CoalHeap heap) {
        this.heap = heap;
    }

    public MovementOperation getOperation() {
        return operation;
    }

    public void setOperation(MovementOperation operation) {
        this.operation = operation;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public String getOriginSource() {
        return originSource;
    }

    public void setOriginSource(String originSource) {
        this.originSource = originSource;
    }
}
