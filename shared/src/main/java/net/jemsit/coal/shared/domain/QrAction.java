package net.jemsit.coal.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A printed request to accept a party into a pile of one warehouse. It stays usable for partial
 * deliveries; what has been done is read from the movement ledger, so only cancelling is stored.
 */
@Entity
@Table(name = "qr_action")
public class QrAction extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private QrActionType action;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private CoalBatch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public QrActionType getAction() {
        return action;
    }

    public void setAction(QrActionType action) {
        this.action = action;
    }

    public CoalBatch getBatch() {
        return batch;
    }

    public void setBatch(CoalBatch batch) {
        this.batch = batch;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
