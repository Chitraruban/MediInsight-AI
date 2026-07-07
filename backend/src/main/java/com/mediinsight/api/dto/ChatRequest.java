package com.mediinsight.api.dto;

import java.util.List;
import java.util.Map;

public class ChatRequest {
    private Long reportId;
    private String message;
    private List<Map<String, String>> history;

    public ChatRequest() {}

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Map<String, String>> getHistory() { return history; }
    public void setHistory(List<Map<String, String>> history) { this.history = history; }
}
