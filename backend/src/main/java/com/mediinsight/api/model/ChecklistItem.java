package com.mediinsight.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "checklist_items")
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    @JsonIgnore
    private Report report;

    @Column(name = "item_text", columnDefinition = "TEXT", nullable = false)
    private String itemText;

    @Column(name = "is_checked", nullable = false)
    private boolean isChecked;

    // Constructors
    public ChecklistItem() {}

    public ChecklistItem(Report report, String itemText, boolean isChecked) {
        this.report = report;
        this.itemText = itemText;
        this.isChecked = isChecked;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }

    public String getItemText() { return itemText; }
    public void setItemText(String itemText) { this.itemText = itemText; }

    public boolean getIsChecked() { return isChecked; }
    public void setIsChecked(boolean isChecked) { this.isChecked = isChecked; }
}
