package net.jemsit.coal.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "lab_analysis_value")
public class LabAnalysisValue extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private LabAnalysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Indicator indicator;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal value;

    @Column(length = 20)
    private String unit;

    public LabAnalysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(LabAnalysis analysis) {
        this.analysis = analysis;
    }

    public Indicator getIndicator() {
        return indicator;
    }

    public void setIndicator(Indicator indicator) {
        this.indicator = indicator;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
