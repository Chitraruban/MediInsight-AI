package com.mediinsight.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "report_values")
public class ReportValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    @JsonIgnore
    private Report report;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String value;

    private String unit;

    @Column(name = "normal_range")
    private String normalRange;

    private String status; // normal, high, low

    @Lob
    @Column(name = "term_meaning", columnDefinition = "TEXT")
    private String termMeaning;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String explanation;

    // Constructors
    public ReportValue() {}

    public ReportValue(Report report, String name, String value, String unit, String normalRange, String status, String termMeaning, String explanation) {
        this.report = report;
        this.name = name;
        this.value = value;
        this.unit = unit;
        this.normalRange = normalRange;
        this.status = status;
        this.termMeaning = termMeaning;
        this.explanation = explanation;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getNormalRange() { return normalRange; }
    public void setNormalRange(String normalRange) { this.normalRange = normalRange; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTermMeaning() { return termMeaning; }
    public void setTermMeaning(String termMeaning) { this.termMeaning = termMeaning; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
