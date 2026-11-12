package net.jemsit.coal.shared.domain;

/** What sort of thing a user did, for the admin «Журнал действий» page. */
public enum ActivityKind {
    LOGIN,
    LOGOUT,
    /** Opened a menu page. */
    VISIT,
    CREATE,
    UPDATE,
    DELETE,
    /** An operation on an existing record: accept, split, merge, transfer, QR execute/cancel. */
    ACTION
}
