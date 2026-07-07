package com.mediinsight.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "lifestyle_suggestions")
public class LifestyleSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    @JsonIgnore
    private Report report;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LifestyleCategory category;

    @Lob
    @Column(name = "suggestion_text", columnDefinition = "TEXT", nullable = false)
    private String suggestionText;

    // Constructors
    public LifestyleSuggestion() {}

    public LifestyleSuggestion(Report report, LifestyleCategory category, String suggestionText) {
        this.report = report;
        this.category = category;
        this.suggestionText = suggestionText;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }

    public LifestyleCategory getCategory() { return category; }
    public void setCategory(LifestyleCategory category) { this.category = category; }

    public String getSuggestionText() { return suggestionText; }
    public void setSuggestionText(String suggestionText) { this.suggestionText = suggestionText; }
}
