package com.mediinsight.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "report_risks")
public class ReportRisk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    @JsonIgnore
    private Report report;

    @Column(name = "condition_name", nullable = false)
    private String conditionName;

    @Column(name = "related_to")
    private String relatedTo;

    private String severity; // Low, Medium, High

    @Lob
    @Column(columnDefinition = "TEXT")
    private String explanation;

    // Constructors
    public ReportRisk() {}

    public ReportRisk(Report report, String conditionName, String relatedTo, String severity, String explanation) {
        this.report = report;
        this.conditionName = conditionName;
        this.relatedTo = relatedTo;
        this.severity = severity;
        this.explanation = explanation;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }

    public String getConditionName() { return conditionName; }
    public void setConditionName(String conditionName) { this.conditionName = conditionName; }

    public String getRelatedTo() { return relatedTo; }
    public void setRelatedTo(String relatedTo) { this.relatedTo = relatedTo; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
