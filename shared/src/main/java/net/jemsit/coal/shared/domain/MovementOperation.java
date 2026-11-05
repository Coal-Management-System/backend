package net.jemsit.coal.shared.domain;

public enum MovementOperation {
    PILE_CREATED,
    PILE_MERGED,
    PILE_SPLIT,
    PILE_TRANSFERRED,
    BATCH_ACCEPTED_INTO_PILE,
    // Product-lifecycle stages, recordable from the movements menu. Stored as strings
    // in a varchar(30) column, so adding them needs no schema migration. PROCESSING,
    // RESERVATION and SHIPMENT move real volumes on the party (and on the pile, where
    // the coal physically leaves it) — see MovementService.apply. ANALYSIS, BLENDING and
    // the pile rows above are journal entries only; the pile rows' volume effects belong
    // to the pile operations that record them.
    ANALYSIS,
    PROCESSING,
    BLENDING,
    RESERVATION,
    SHIPMENT
}
