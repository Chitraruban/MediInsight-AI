package com.mediinsight.api.dto;

import java.util.List;

public class DashboardSummary {
    private int latestHealthScore;
    private long totalAbnormalValues;
    private String latestRiskLevel;
    private List<ScoreTrendItem> scoreTrend;

    public DashboardSummary() {}

    public DashboardSummary(int latestHealthScore, long totalAbnormalValues, String latestRiskLevel, List<ScoreTrendItem> scoreTrend) {
        this.latestHealthScore = latestHealthScore;
        this.totalAbnormalValues = totalAbnormalValues;
        this.latestRiskLevel = latestRiskLevel;
        this.scoreTrend = scoreTrend;
    }

    public int getLatestHealthScore() { return latestHealthScore; }
    public void setLatestHealthScore(int latestHealthScore) { this.latestHealthScore = latestHealthScore; }

    public long getTotalAbnormalValues() { return totalAbnormalValues; }
    public void setTotalAbnormalValues(long totalAbnormalValues) { this.totalAbnormalValues = totalAbnormalValues; }

    public String getLatestRiskLevel() { return latestRiskLevel; }
    public void setLatestRiskLevel(String latestRiskLevel) { this.latestRiskLevel = latestRiskLevel; }

    public List<ScoreTrendItem> getScoreTrend() { return scoreTrend; }
    public void setScoreTrend(List<ScoreTrendItem> scoreTrend) { this.scoreTrend = scoreTrend; }
}
