package net.jemsit.coal.shared.domain;

/** The batch free-text fields that draw their vocabulary from the dictionary table. */
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
    METHOD
}
