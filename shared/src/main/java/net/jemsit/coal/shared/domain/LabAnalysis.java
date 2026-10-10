package net.jemsit.coal.shared.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lab_analysis")
public class LabAnalysis extends BaseEntity {

    @Column(name = "sample_number", nullable = false, unique = true, length = 50)
    private String sampleNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private CoalBatch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_id", nullable = false)
    private Laboratory lab;

    @Column(name = "protocol_number", length = 50)
    private String protocolNumber;

    @Column(name = "responsible_employee", length = 200)
    private String responsibleEmployee;

    @Column(length = 200)
    private String method;

    @Column(name = "analysis_date")
    private Instant analysisDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AnalysisStage stage = AnalysisStage.INITIAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisStatus status = AnalysisStatus.DRAFT;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LabAnalysisValue> values = new ArrayList<>();

    public String getSampleNumber() {
        return sampleNumber;
    }

    public void setSampleNumber(String sampleNumber) {
        this.sampleNumber = sampleNumber;
    }

    public CoalBatch getBatch() {
        return batch;
    }

    public void setBatch(CoalBatch batch) {
        this.batch = batch;
    }

    public Laboratory getLab() {
        return lab;
    }

    public void setLab(Laboratory lab) {
        this.lab = lab;
    }

    public String getProtocolNumber() {
        return protocolNumber;
    }

    public void setProtocolNumber(String protocolNumber) {
        this.protocolNumber = protocolNumber;
    }

    public String getResponsibleEmployee() {
        return responsibleEmployee;
    }

    public void setResponsibleEmployee(String responsibleEmployee) {
        this.responsibleEmployee = responsibleEmployee;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Instant getAnalysisDate() {
        return analysisDate;
    }

    public void setAnalysisDate(Instant analysisDate) {
        this.analysisDate = analysisDate;
    }

    public AnalysisStage getStage() {
        return stage;
    }

    public void setStage(AnalysisStage stage) {
        this.stage = stage;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisStatus status) {
        this.status = status;
    }

    public List<LabAnalysisValue> getValues() {
        return values;
    }
}
