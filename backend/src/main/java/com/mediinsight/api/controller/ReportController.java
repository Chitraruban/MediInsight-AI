package com.mediinsight.api.controller;

import com.mediinsight.api.model.*;
import com.mediinsight.api.repository.*;
import org.springframework.transaction.annotation.Transactional;
import com.mediinsight.api.service.GeminiService;
import com.mediinsight.api.service.OcrService;
import com.mediinsight.api.service.PdfGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportRepository reportRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final OcrService ocrService;
    private final GeminiService geminiService;
    private final PdfGenerationService pdfGenerationService;

    public ReportController(ReportRepository reportRepository,
                            ChecklistItemRepository checklistItemRepository,
                            ChatMessageRepository chatMessageRepository,
                            OcrService ocrService,
                            GeminiService geminiService,
                            PdfGenerationService pdfGenerationService) {
        this.reportRepository = reportRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.ocrService = ocrService;
        this.geminiService = geminiService;
        this.pdfGenerationService = pdfGenerationService;
    }

    /**
     * POST /api/reports/upload
     * Handles file upload, OCR, Gemini analysis, saves report, and returns it.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Report> uploadReport(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("Received report upload request: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        // Detect if the file is an image (not a PDF)
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        boolean isImage = false;
        if (contentType != null) {
            isImage = contentType.startsWith("image/");
        } else if (originalFilename != null) {
            String lower = originalFilename.toLowerCase();
            isImage = lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp");
        }

        Map<String, Object> analysisMap;
        if (isImage) {
            // Use Gemini Vision directly for images — much more accurate than OCR
            log.info("Image detected. Using Gemini Vision API directly for: {}", originalFilename);
            String mimeType = contentType != null ? contentType : "image/png";
            analysisMap = geminiService.analyzeReportImage(file.getBytes(), mimeType);
        } else {
            // Use OCR for PDFs, then send text to Gemini
            log.info("PDF detected. Using OCR + text analysis for: {}", originalFilename);
            String extractedText = ocrService.extractText(file);
            log.info("Text extraction completed. Extracted length: {} characters.", extractedText.length());
            analysisMap = geminiService.analyzeReport(extractedText);
        }
        log.info("Gemini analysis successful. Parsing values...");

        // 3. Parse JSON response & Save entities
        Report report = new Report();
        report.setFileName(file.getOriginalFilename());
        report.setUploadedAt(LocalDateTime.now());
        
        // Parse healthScore safely
        Object healthScoreObj = analysisMap.get("healthScore");
        int healthScore = 0;
        if (healthScoreObj instanceof Number) {
            healthScore = ((Number) healthScoreObj).intValue();
        } else if (healthScoreObj instanceof String) {
            try {
                healthScore = Integer.parseInt((String) healthScoreObj);
            } catch (Exception e) {
                log.warn("Failed to parse healthScore string: {}", healthScoreObj);
            }
        }
        report.setHealthScore(healthScore);
        
        report.setOverallStatus((String) analysisMap.getOrDefault("overallStatus", "Unclear"));
        report.setRiskLevel((String) analysisMap.getOrDefault("riskLevel", "Low"));
        report.setSummary((String) analysisMap.getOrDefault("summary", ""));
        report.setDisclaimer((String) analysisMap.getOrDefault("disclaimer", ""));

        // Value list mapping
        List<ReportValue> reportValues = new ArrayList<>();
        List<Map<String, Object>> valuesList = (List<Map<String, Object>>) analysisMap.get("values");
        if (valuesList != null) {
            for (Map<String, Object> valMap : valuesList) {
                ReportValue val = new ReportValue(
                    report,
                    (String) valMap.getOrDefault("name", "Unknown"),
                    String.valueOf(valMap.getOrDefault("value", "")),
                    (String) valMap.getOrDefault("unit", ""),
                    (String) valMap.getOrDefault("normalRange", ""),
                    (String) valMap.getOrDefault("status", "normal"),
                    (String) valMap.getOrDefault("termMeaning", ""),
                    (String) valMap.getOrDefault("explanation", "")
                );
                reportValues.add(val);
            }
        }
        report.setValues(reportValues);

        // Risks list mapping
        List<ReportRisk> reportRisks = new ArrayList<>();
        List<Map<String, Object>> risksList = (List<Map<String, Object>>) analysisMap.get("risks");
        if (risksList != null) {
            for (Map<String, Object> riskMap : risksList) {
                ReportRisk risk = new ReportRisk(
                    report,
                    (String) riskMap.getOrDefault("condition", "Unknown"),
                    (String) riskMap.getOrDefault("relatedTo", ""),
                    (String) riskMap.getOrDefault("severity", "Low"),
                    (String) riskMap.getOrDefault("explanation", "")
                );
                reportRisks.add(risk);
            }
        }
        report.setRisks(reportRisks);

        // Lifestyle suggestions mapping
        List<LifestyleSuggestion> lifestyleSuggestions = new ArrayList<>();
        Map<String, Object> lifestyleMap = (Map<String, Object>) analysisMap.get("lifestyle");
        if (lifestyleMap != null) {
            // FOOD (array)
            Object foodObj = lifestyleMap.get("food");
            if (foodObj instanceof List) {
                for (Object item : (List) foodObj) {
                    lifestyleSuggestions.add(new LifestyleSuggestion(report, LifestyleCategory.FOOD, String.valueOf(item)));
                }
            }
            // EXERCISE (array)
            Object exerciseObj = lifestyleMap.get("exercise");
            if (exerciseObj instanceof List) {
                for (Object item : (List) exerciseObj) {
                    lifestyleSuggestions.add(new LifestyleSuggestion(report, LifestyleCategory.EXERCISE, String.valueOf(item)));
                }
            }
            // HYDRATION (string or array)
            Object hydrationObj = lifestyleMap.get("hydration");
            if (hydrationObj instanceof List) {
                for (Object item : (List) hydrationObj) {
                    lifestyleSuggestions.add(new LifestyleSuggestion(report, LifestyleCategory.HYDRATION, String.valueOf(item)));
                }
            } else if (hydrationObj != null) {
                lifestyleSuggestions.add(new LifestyleSuggestion(report, LifestyleCategory.HYDRATION, String.valueOf(hydrationObj)));
            }
            // SLEEP (string or array)
            Object sleepObj = lifestyleMap.get("sleep");
            if (sleepObj instanceof List) {
                for (Object item : (List) sleepObj) {
                    lifestyleSuggestions.add(new LifestyleSuggestion(report, LifestyleCategory.SLEEP, String.valueOf(item)));
                }
            } else if (sleepObj != null) {
                lifestyleSuggestions.add(new LifestyleSuggestion(report, LifestyleCategory.SLEEP, String.valueOf(sleepObj)));
            }
            // STRESS (array)
            Object stressObj = lifestyleMap.get("stress");
            if (stressObj instanceof List) {
                for (Object item : (List) stressObj) {
                    lifestyleSuggestions.add(new LifestyleSuggestion(report, LifestyleCategory.STRESS, String.valueOf(item)));
                }
            }
        }
        report.setLifestyleSuggestions(lifestyleSuggestions);

        // Doctor questions mapping
        List<DoctorQuestion> doctorQuestions = new ArrayList<>();
        Object dqObj = analysisMap.get("doctorQuestions");
        if (dqObj instanceof List) {
            for (Object q : (List) dqObj) {
                doctorQuestions.add(new DoctorQuestion(report, String.valueOf(q)));
            }
        }
        report.setDoctorQuestions(doctorQuestions);

        // Checklist mapping
        List<ChecklistItem> checklistItems = new ArrayList<>();
        Object clObj = analysisMap.get("checklist");
        if (clObj instanceof List) {
            for (Object item : (List) clObj) {
                checklistItems.add(new ChecklistItem(report, String.valueOf(item), false));
            }
        }
        report.setChecklist(checklistItems);

        // Save complete report tree via cascade
        Report savedReport = reportRepository.save(report);
        log.info("Report successfully stored with ID: {}", savedReport.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(savedReport);
    }

    /**
     * GET /api/reports
     * Returns a lightweight list of reports, newest first.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listReports() {
        log.info("Listing all reports (lightweight)");
        List<Report> reports = reportRepository.findAllByOrderByUploadedAtDesc();
        
        List<Map<String, Object>> responseList = reports.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("fileName", r.getFileName());
            map.put("uploadedAt", r.getUploadedAt());
            map.put("healthScore", r.getHealthScore());
            map.put("overallStatus", r.getOverallStatus());
            map.put("riskLevel", r.getRiskLevel());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    /**
     * GET /api/reports/{id}
     * Returns full details of a specific report.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Report> getReport(@PathVariable("id") Long id) {
        log.info("Fetching report detail for ID: {}", id);
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + id));
        return ResponseEntity.ok(report);
    }

    /**
     * DELETE /api/reports/{id}
     */
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteReport(@PathVariable("id") Long id) {
        log.info("Deleting report ID: {}", id);
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + id));
        
        // Delete related chat messages first to avoid foreign key constraint
        chatMessageRepository.deleteByReportId(id);
        reportRepository.delete(report);
        return ResponseEntity.ok(Map.of("message", "Report deleted successfully"));
    }

    /**
     * PATCH /api/reports/{id}/checklist/{itemId}
     * Toggles the isChecked status of a checklist item.
     */
    @PatchMapping("/{id}/checklist/{itemId}")
    public ResponseEntity<ChecklistItem> toggleChecklistItem(@PathVariable("id") Long id,
                                                             @PathVariable("itemId") Long itemId) {
        log.info("Toggling checklist item ID: {} for report ID: {}", itemId, id);
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Checklist item not found with ID: " + itemId));
        
        if (!item.getReport().getId().equals(id)) {
            throw new IllegalArgumentException("Checklist item does not belong to report ID: " + id);
        }

        item.setIsChecked(!item.getIsChecked());
        ChecklistItem updatedItem = checklistItemRepository.save(item);
        return ResponseEntity.ok(updatedItem);
    }

    /**
     * GET /api/reports/{id}/pdf
     * Streams the generated PDF summary back to the client.
     */
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadReportPdf(@PathVariable("id") Long id) {
        log.info("Downloading PDF summary for report ID: {}", id);
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + id));

        byte[] pdfBytes = pdfGenerationService.generateReportPdf(report);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String safeFileName = report.getFileName().replaceAll("[^a-zA-Z0-9.-]", "_");
        if (!safeFileName.toLowerCase().endsWith(".pdf")) {
            safeFileName += ".pdf";
        }
        headers.setContentDispositionFormData("attachment", "MediInsight_Summary_" + safeFileName);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
