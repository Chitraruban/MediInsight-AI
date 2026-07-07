package com.mediinsight.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "doctor_questions")
public class DoctorQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    @JsonIgnore
    private Report report;

    @Lob
    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    // Constructors
    public DoctorQuestion() {}

    public DoctorQuestion(Report report, String questionText) {
        this.report = report;
        this.questionText = questionText;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
}
