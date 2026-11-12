package net.jemsit.coal.shared.domain;

/** Access roles from the spec, section 20 «Права доступа», plus the warehouse operator and the admin. */
public enum UserRole {
    /** Администратор: full access, manages the system. */
    ADMIN,
    /** Лаборант: enters analysis results, views assigned batches. */
    LAB_TECHNICIAN,
    /** Производственный персонал: production operations, product transfer, release confirmation. */
    PRODUCTION_STAFF,
    /** Кладовщик: moves, receives and issues product. */
    STOREKEEPER,
    /** Оператор склада: works on the warehouse floor, accepts batches into piles by scanning QR codes. */
    WAREHOUSE_OPERATOR,
    /** Диспетчер: controls movements, reserves, prepares shipments. */
    DISPATCHER,
    /** Руководитель производства: full access to production data, analytics. */
    PRODUCTION_HEAD,
    /** Коммерческий отдел: views available product, reserves, checks order compliance. */
    COMMERCIAL,
    /** Руководство: views all figures, management reporting, efficiency analytics. */
    MANAGEMENT
}
