package net.jemsit.coal.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * One line of the user activity journal. Kept to a one-sentence summary instead of a
 * field-by-field diff so the table stays small. The user's name is copied in, so the
 * journal still reads correctly after a user is renamed or removed.
 */
@Entity
@Table(name = "user_activity")
public class UserActivity extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String username;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityKind kind;

    /** PILE, BATCH, WAREHOUSE, MOVEMENT, LAB, ANALYSIS, DICTIONARY, QR_ACTION, PAGE; null for login/logout. */
    @Column(name = "entity_type", length = 30)
    private String entityType;

    /** The number or name the user sees: pile number, party number, page path. */
    @Column(name = "entity_label", length = 200)
    private String entityLabel;

    @Column(nullable = false, length = 500)
    private String summary;

    public UserActivity() {
    }

    public UserActivity(String username, String fullName, ActivityKind kind,
                        String entityType, String entityLabel, String summary) {
        this.username = username;
        this.fullName = fullName;
        this.kind = kind;
        this.entityType = entityType;
        this.entityLabel = entityLabel;
        this.summary = summary;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public ActivityKind getKind() {
        return kind;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityLabel() {
        return entityLabel;
    }

    public String getSummary() {
        return summary;
    }
}
