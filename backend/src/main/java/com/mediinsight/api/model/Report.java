package com.mediinsight.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "health_score", nullable = false)
    private int healthScore;

    @Column(name = "overall_status", nullable = false)
    private String overallStatus;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "disclaimer", columnDefinition = "TEXT")
    private String disclaimer;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportValue> values = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportRisk> risks = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LifestyleSuggestion> lifestyleSuggestions = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DoctorQuestion> doctorQuestions = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChecklistItem> checklist = new ArrayList<>();

    // Constructors
    public Report() {
        this.uploadedAt = LocalDateTime.now();
    }

    public Report(String fileName, int healthScore, String overallStatus, String riskLevel, String summary, String disclaimer) {
        this.fileName = fileName;
        this.uploadedAt = LocalDateTime.now();
        this.healthScore = healthScore;
        this.overallStatus = overallStatus;
        this.riskLevel = riskLevel;
        this.summary = summary;
        this.disclaimer = disclaimer;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public int getHealthScore() { return healthScore; }
    public void setHealthScore(int healthScore) { this.healthScore = healthScore; }

    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }

    public List<ReportValue> getValues() { return values; }
    public void setValues(List<ReportValue> values) { this.values = values; }

    public List<ReportRisk> getRisks() { return risks; }
    public void setRisks(List<ReportRisk> risks) { this.risks = risks; }

    public List<LifestyleSuggestion> getLifestyleSuggestions() { return lifestyleSuggestions; }
    public void setLifestyleSuggestions(List<LifestyleSuggestion> lifestyleSuggestions) { this.lifestyleSuggestions = lifestyleSuggestions; }

    public List<DoctorQuestion> getDoctorQuestions() { return doctorQuestions; }
    public void setDoctorQuestions(List<DoctorQuestion> doctorQuestions) { this.doctorQuestions = doctorQuestions; }

    public List<ChecklistItem> getChecklist() { return checklist; }
    public void setChecklist(List<ChecklistItem> checklist) { this.checklist = checklist; }
}
