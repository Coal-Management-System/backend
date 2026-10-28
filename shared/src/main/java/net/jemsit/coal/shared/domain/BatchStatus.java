package net.jemsit.coal.shared.domain;

public enum BatchStatus {
    NEW,
    IN_PROCESSING,
    READY_FOR_SHIPMENT,
    SHIPPED,
    /** Set automatically once the party has been accepted into a pile. */
    ARCHIVED
}
