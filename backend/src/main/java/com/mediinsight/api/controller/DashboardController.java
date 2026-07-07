package com.mediinsight.api.controller;

import com.mediinsight.api.dto.DashboardSummary;
import com.mediinsight.api.dto.ScoreTrendItem;
import com.mediinsight.api.model.Report;
import com.mediinsight.api.model.ReportValue;
import com.mediinsight.api.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReportRepository reportRepository;

    public DashboardController(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    /**
     * GET /api/dashboard/summary
     * Computes dashboard statistics from the saved reports:
     * - latestHealthScore
     * - totalAbnormalValues (high or low status in the latest report)
     * - latestRiskLevel
     * - scoreTrend (chronological list of dates and health scores)
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> getDashboardSummary() {
        log.info("Computing dashboard summary stats");
        
        List<Report> reports = reportRepository.findAllByOrderByUploadedAtDesc();
        
        if (reports.isEmpty()) {
            return ResponseEntity.ok(new DashboardSummary(0, 0, "Low", Collections.emptyList()));
        }

        // Latest report is the first element
        Report latestReport = reports.get(0);
        
        int latestHealthScore = latestReport.getHealthScore();
        String latestRiskLevel = latestReport.getRiskLevel() != null ? latestReport.getRiskLevel() : "Low";
        
        // Count abnormal values (status high or low)
        long totalAbnormalValues = 0;
        if (latestReport.getValues() != null) {
            totalAbnormalValues = latestReport.getValues().stream()
                    .filter(val -> val.getStatus() != null && 
                            (val.getStatus().equalsIgnoreCase("high") || val.getStatus().equalsIgnoreCase("low")))
                    .count();
        }

        // Score trend chronologically (oldest first, so reverse the list of reports or load it differently)
        List<ScoreTrendItem> scoreTrend = new ArrayList<>();
        // We iterate backwards to get chronological order (oldest to newest)
        for (int i = reports.size() - 1; i >= 0; i--) {
            Report r = reports.get(i);
            String formattedDate = r.getUploadedAt().format(DATE_FORMATTER);
            scoreTrend.add(new ScoreTrendItem(formattedDate, r.getHealthScore()));
        }

        return ResponseEntity.ok(new DashboardSummary(
                latestHealthScore,
                totalAbnormalValues,
                latestRiskLevel,
                scoreTrend
        ));
    }
}
