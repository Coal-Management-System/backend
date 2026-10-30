package net.jemsit.coal.shared.domain;

public enum MovementOperation {
    PILE_CREATED,
    PILE_MERGED,
    PILE_SPLIT,
    PILE_TRANSFERRED,
    BATCH_ACCEPTED_INTO_PILE,
    // Product-lifecycle stages, recordable from the movements menu. Stored as strings
    // in a varchar(30) column, so adding them needs no schema migration. For now they
    // only log a movement; the processing/shipment modules that would give them real
    // volume effects are stage 2.
    ANALYSIS,
    PROCESSING,
    BLENDING,
    RESERVATION,
    SHIPMENT
}
