package net.jemsit.coal.shared.domain;

public enum BatchStatus {
    NEW,
    IN_PROCESSING,
    READY_FOR_SHIPMENT,
    SHIPPED,
    /** Set automatically once the party has been accepted into a pile. */
    ARCHIVED,
    /** Продана: final, set by hand. The party's volumes are kept as they were. */
    SOLD,
    /** Списана: final, set by hand. The party's volumes are kept as they were. */
    WRITTEN_OFF;

    /**
     * A sold or written-off party is closed for good: its status cannot be changed again,
     * automatic status updates leave it alone, and no new operation may use it.
     */
    public boolean isFinal() {
        return this == SOLD || this == WRITTEN_OFF;
    }

    /** Russian name for error messages. */
    public String finalLabel() {
        return this == SOLD ? "продана" : "списана";
    }
}
