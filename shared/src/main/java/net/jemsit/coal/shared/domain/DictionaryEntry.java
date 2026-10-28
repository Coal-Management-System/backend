package net.jemsit.coal.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One vocabulary word for one batch field; (field, value) pairs are unique. */
@Entity
@Table(
        name = "dictionary_entry",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_dictionary_entry_field_value",
                columnNames = {"field", "value"}))
public class DictionaryEntry extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "field", nullable = false, length = 30)
    private DictionaryField field;

    @Column(nullable = false, length = 120)
    private String value;

    public DictionaryField getField() {
        return field;
    }

    public void setField(DictionaryField field) {
        this.field = field;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
