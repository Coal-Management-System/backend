package net.jemsit.coal.shared.domain;

/** The free-text fields that draw their vocabulary from the dictionary table. */
public enum DictionaryField {
    PRODUCT_TYPE,
    COAL_GRADE,
    FRACTION,
    QUARRY,
    DEPOSIT,
    MINING_SITE,
    SUPPLIER,
    // Lab-analysis method; shares the same dictionary table (field is varchar(30),
    // no CHECK constraint), so adding it needs no schema migration.
    METHOD,
    // Movement "from"/"to" places. Both directions share one vocabulary: the same
    // warehouse, shaft or station can be an origin as well as a destination.
    LOCATION
}
