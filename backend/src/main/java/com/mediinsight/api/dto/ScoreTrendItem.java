package com.mediinsight.api.dto;

import java.time.LocalDateTime;

public class ScoreTrendItem {
    private String date;
    private int score;

    public ScoreTrendItem() {}

    public ScoreTrendItem(String date, int score) {
        this.date = date;
        this.score = score;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
}
