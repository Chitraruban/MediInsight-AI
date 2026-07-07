package com.mediinsight.api.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.mediinsight.api.model.DoctorQuestion;
import com.mediinsight.api.model.LifestyleSuggestion;
import com.mediinsight.api.model.Report;
import com.mediinsight.api.model.ReportRisk;
import com.mediinsight.api.model.ReportValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PdfGenerationService.class);

    // Sleek primary colors
    private static final DeviceRgb COLOR_PRIMARY = new DeviceRgb(11, 95, 165); // #0B5FA5
    private static final DeviceRgb COLOR_SECONDARY = new DeviceRgb(240, 244, 248); // Light gray-blue background
    private static final DeviceRgb COLOR_TEXT = new DeviceRgb(33, 37, 41); // Dark charcoal text
    
    // Status colors
    private static final DeviceRgb COLOR_RED = new DeviceRgb(220, 53, 69);
    private static final DeviceRgb COLOR_AMBER = new DeviceRgb(255, 193, 7);
    private static final DeviceRgb COLOR_GREEN = new DeviceRgb(40, 167, 69);
    private static final DeviceRgb COLOR_GRAY = new DeviceRgb(108, 117, 125);

    public byte[] generateReportPdf(Report report) {
        log.info("Generating PDF summary for report ID: {}", report.getId());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            
            // Set margins
            document.setMargins(36, 36, 36, 36);

            // Document Header
            Paragraph header = new Paragraph("MediInsight AI")
                    .setFontSize(24)
                    .setBold()
                    .setFontColor(COLOR_PRIMARY)
                    .setTextAlignment(TextAlignment.LEFT);
            document.add(header);

            Paragraph subtitle = new Paragraph("Educational Medical Report Analysis")
                    .setFontSize(12)
                    .setFontColor(COLOR_GRAY)
                    .setMarginBottom(20);
            document.add(subtitle);

            // Report Metadata Info Box
            Table metaTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);
            
            metaTable.addCell(createMetaCellLabel("Report File:"));
            metaTable.addCell(createMetaCellValue(report.getFileName()));
            
            metaTable.addCell(createMetaCellLabel("Analyzed At:"));
            metaTable.addCell(createMetaCellValue(report.getUploadedAt().toString()));
            
            metaTable.addCell(createMetaCellLabel("Overall Status:"));
            metaTable.addCell(createMetaCellValue(report.getOverallStatus() + " (Risk Level: " + report.getRiskLevel() + ")"));

            metaTable.addCell(createMetaCellLabel("Health Score:"));
            metaTable.addCell(new Cell().add(new Paragraph(String.valueOf(report.getHealthScore()))
                    .setBold().setFontColor(getScoreColor(report.getHealthScore()))));

            document.add(metaTable);

            // Summary Section
            document.add(new Paragraph("Executive Summary")
                    .setFontSize(16)
                    .setBold()
                    .setFontColor(COLOR_PRIMARY)
                    .setMarginBottom(5));
            
            document.add(new Paragraph(report.getSummary() != null ? report.getSummary() : "No summary available.")
                    .setFontSize(11)
                    .setFontColor(COLOR_TEXT)
                    .setMarginBottom(20));

            // Lab Values Section
            document.add(new Paragraph("Extracted Lab Values & Findings")
                    .setFontSize(16)
                    .setBold()
                    .setFontColor(COLOR_PRIMARY)
                    .setMarginBottom(5));

            if (report.getValues() == null || report.getValues().isEmpty()) {
                document.add(new Paragraph("No lab values extracted.")
                        .setFontSize(10)
                        .setItalic()
                        .setMarginBottom(20));
            } else {
                Table valuesTable = new Table(UnitValue.createPercentArray(new float[]{25, 12, 10, 15, 10, 28}))
                        .useAllAvailableWidth()
                        .setMarginBottom(20);

                // Table Headers
                valuesTable.addHeaderCell(createHeaderCell("Biomarker / Test"));
                valuesTable.addHeaderCell(createHeaderCell("Value"));
                valuesTable.addHeaderCell(createHeaderCell("Unit"));
                valuesTable.addHeaderCell(createHeaderCell("Normal Range"));
                valuesTable.addHeaderCell(createHeaderCell("Status"));
                valuesTable.addHeaderCell(createHeaderCell("Explanation"));

                for (ReportValue value : report.getValues()) {
                    valuesTable.addCell(new Cell().add(new Paragraph(value.getName()).setFontSize(9).setBold()));
                    valuesTable.addCell(new Cell().add(new Paragraph(value.getValue() != null ? value.getValue() : "").setFontSize(9)));
                    valuesTable.addCell(new Cell().add(new Paragraph(value.getUnit() != null ? value.getUnit() : "").setFontSize(9)));
                    valuesTable.addCell(new Cell().add(new Paragraph(value.getNormalRange() != null ? value.getNormalRange() : "").setFontSize(9)));
                    
                    // Status badge
                    String statusStr = value.getStatus() != null ? value.getStatus().toUpperCase() : "NORMAL";
                    Paragraph statusPara = new Paragraph(statusStr).setFontSize(9).setBold().setFontColor(getStatusColor(statusStr));
                    valuesTable.addCell(new Cell().add(statusPara));
                    
                    String explanation = value.getExplanation();
                    if (value.getTermMeaning() != null && !value.getTermMeaning().isEmpty()) {
                        explanation = value.getTermMeaning() + " - " + (explanation != null ? explanation : "");
                    }
                    valuesTable.addCell(new Cell().add(new Paragraph(explanation != null ? explanation : "").setFontSize(8)));
                }

                document.add(valuesTable);
            }

            // Health Risks Section
            if (report.getRisks() != null && !report.getRisks().isEmpty()) {
                document.add(new Paragraph("Potential Risks & Concerns")
                        .setFontSize(16)
                        .setBold()
                        .setFontColor(COLOR_PRIMARY)
                        .setMarginBottom(5));

                Table risksTable = new Table(UnitValue.createPercentArray(new float[]{30, 20, 15, 35}))
                        .useAllAvailableWidth()
                        .setMarginBottom(20);

                risksTable.addHeaderCell(createHeaderCell("Condition"));
                risksTable.addHeaderCell(createHeaderCell("Related To"));
                risksTable.addHeaderCell(createHeaderCell("Severity"));
                risksTable.addHeaderCell(createHeaderCell("Details"));

                for (ReportRisk risk : report.getRisks()) {
                    risksTable.addCell(new Cell().add(new Paragraph(risk.getConditionName()).setFontSize(9).setBold()));
                    risksTable.addCell(new Cell().add(new Paragraph(risk.getRelatedTo() != null ? risk.getRelatedTo() : "").setFontSize(9)));
                    
                    String severity = risk.getSeverity() != null ? risk.getSeverity().toUpperCase() : "LOW";
                    risksTable.addCell(new Cell().add(new Paragraph(severity).setFontSize(9).setBold().setFontColor(getStatusColor(severity))));
                    risksTable.addCell(new Cell().add(new Paragraph(risk.getExplanation() != null ? risk.getExplanation() : "").setFontSize(9)));
                }
                document.add(risksTable);
            }

            // Lifestyle Suggestions Section
            if (report.getLifestyleSuggestions() != null && !report.getLifestyleSuggestions().isEmpty()) {
                document.add(new Paragraph("Lifestyle Recommendations")
                        .setFontSize(16)
                        .setBold()
                        .setFontColor(COLOR_PRIMARY)
                        .setMarginBottom(5));

                for (LifestyleSuggestion sug : report.getLifestyleSuggestions()) {
                    Paragraph sugPara = new Paragraph()
                            .add(new Paragraph("[" + sug.getCategory().name() + "] ").setBold().setFontColor(COLOR_PRIMARY))
                            .add(new Paragraph(sug.getSuggestionText()))
                            .setFontSize(10)
                            .setMarginBottom(4);
                    document.add(sugPara);
                }
                document.add(new Paragraph("").setMarginBottom(10));
            }

            // Questions for Doctor Section
            if (report.getDoctorQuestions() != null && !report.getDoctorQuestions().isEmpty()) {
                document.add(new Paragraph("Recommended Questions for Your Doctor")
                        .setFontSize(16)
                        .setBold()
                        .setFontColor(COLOR_PRIMARY)
                        .setMarginBottom(5));

                for (DoctorQuestion q : report.getDoctorQuestions()) {
                    document.add(new Paragraph("• " + q.getQuestionText())
                            .setFontSize(10)
                            .setFontColor(COLOR_TEXT)
                            .setMarginLeft(15)
                            .setMarginBottom(4));
                }
                document.add(new Paragraph("").setMarginBottom(15));
            }

            // Footer Disclaimer
            document.add(new Paragraph("Disclaimer")
                    .setFontSize(10)
                    .setBold()
                    .setFontColor(COLOR_RED)
                    .setMarginBottom(3));
            
            document.add(new Paragraph(report.getDisclaimer() != null ? report.getDisclaimer() : 
                    "This analysis is for educational purposes only and is not a substitute for professional medical consultation. Always consult a licensed healthcare provider.")
                    .setFontSize(8)
                    .setFontColor(COLOR_GRAY)
                    .setTextAlignment(TextAlignment.JUSTIFIED));

            document.close();
            log.info("PDF summary generated successfully. File size: {} bytes", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF document: ", e);
            throw new RuntimeException("Could not generate PDF summary: " + e.getMessage(), e);
        }
    }

    private Cell createHeaderCell(String text) {
        return new Cell()
                .setBackgroundColor(COLOR_PRIMARY)
                .add(new Paragraph(text)
                        .setFontSize(9)
                        .setBold()
                        .setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER));
    }

    private Cell createMetaCellLabel(String label) {
        return new Cell()
                .setBackgroundColor(COLOR_SECONDARY)
                .add(new Paragraph(label)
                        .setFontSize(10)
                        .setBold()
                        .setFontColor(COLOR_PRIMARY));
    }

    private Cell createMetaCellValue(String value) {
        return new Cell()
                .add(new Paragraph(value != null ? value : "")
                        .setFontSize(10)
                        .setFontColor(COLOR_TEXT));
    }

    private DeviceRgb getScoreColor(int score) {
        if (score >= 80) return COLOR_GREEN;
        if (score >= 50) return COLOR_AMBER;
        return COLOR_RED;
    }

    private DeviceRgb getStatusColor(String status) {
        if (status == null) return COLOR_GRAY;
        String val = status.trim().toUpperCase();
        if (val.contains("HIGH") || val.contains("ABNORMAL") || val.contains("RED")) return COLOR_RED;
        if (val.contains("LOW") || val.contains("MILD") || val.contains("AMBER") || val.contains("MEDIUM")) return COLOR_AMBER;
        if (val.contains("NORMAL") || val.contains("GREEN")) return COLOR_GREEN;
        return COLOR_GRAY;
    }
}
