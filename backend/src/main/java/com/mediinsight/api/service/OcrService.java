package com.mediinsight.api.service;

import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    /**
     * Extracts text from the uploaded file.
     * Detects PDF vs Image by content type or file extension.
     */
    public String extractText(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        
        boolean isPdf = false;
        if (contentType != null) {
            isPdf = contentType.equalsIgnoreCase("application/pdf");
        } else if (originalFilename != null) {
            isPdf = originalFilename.toLowerCase().endsWith(".pdf");
        }

        if (isPdf) {
            log.info("Processing PDF report: {}", originalFilename);
            return processPdf(file);
        } else {
            log.info("Processing Image report: {}", originalFilename);
            return processImage(file);
        }
    }

    private String processPdf(MultipartFile file) throws IOException {
        // Step 1: Try PDFBox text extraction
        String extractedText = "";
        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            extractedText = stripper.getText(document);
        } catch (Exception e) {
            log.warn("PDFBox text extraction failed: {}. Will attempt OCR fallback.", e.getMessage());
        }

        if (extractedText != null && extractedText.trim().length() >= 40) {
            log.info("Successfully extracted {} characters of text directly from PDF.", extractedText.trim().length());
            return extractedText;
        }

        log.info("Extracted text is too short ({} chars). Falling back to PDF rendering + OCR.", 
            extractedText == null ? 0 : extractedText.trim().length());

        // Step 2: Render PDF pages as images and run OCR
        // Create a temporary file to load into PDDocument
        Path tempPdfFile = Files.createTempFile("mediinsight-", ".pdf");
        try {
            Files.copy(file.getInputStream(), tempPdfFile, StandardCopyOption.REPLACE_EXISTING);
            return runOcrOnPdfFile(tempPdfFile.toFile());
        } finally {
            try {
                Files.deleteIfExists(tempPdfFile);
            } catch (Exception e) {
                log.warn("Failed to delete temporary PDF file: {}", e.getMessage());
            }
        }
    }

    private String runOcrOnPdfFile(File pdfFile) {
        StringBuilder result = new StringBuilder();
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            log.info("PDF has {} pages. Rendering and running OCR...", pageCount);
            
            for (int page = 0; page < pageCount; page++) {
                log.info("OCR processing page {} of {}...", page + 1, pageCount);
                BufferedImage image = renderer.renderImageWithDPI(page, 150); // 150 DPI is standard for OCR
                String pageText = runTess4jOcr(image);
                if (pageText.contains("Tesseract not installed")) {
                    return pageText; // Early exit if Tesseract is not installed
                }
                result.append(pageText).append("\n");
            }
            return result.toString();
        } catch (Throwable e) {
            log.error("Failed to process PDF rendering + OCR: ", e);
            return "OCR processing failed: " + e.getMessage();
        }
    }

    private String processImage(MultipartFile file) throws IOException {
        Path tempImageFile = Files.createTempFile("mediinsight-img-", file.getOriginalFilename());
        try {
            Files.copy(file.getInputStream(), tempImageFile, StandardCopyOption.REPLACE_EXISTING);
            return runTess4jOcr(tempImageFile.toFile());
        } finally {
            try {
                Files.deleteIfExists(tempImageFile);
            } catch (Exception e) {
                log.warn("Failed to delete temporary image file: {}", e.getMessage());
            }
        }
    }
    private String runTess4jOcr(File imageFile) {
        try {
            Tesseract tesseract = initTesseract();
            return tesseract.doOCR(imageFile);
        } catch (Throwable t) {
            log.warn("Tess4J OCR failed with native loader or runtime error: {}", t.getMessage());
            throw new IllegalStateException("Tesseract OCR engine is not installed on this server. " +
                "Analyzing images or scanned PDFs requires Tesseract. " +
                "Please install Tesseract on your system or upload a digital (text-based) PDF report.", t);
        }
    }

    private String runTess4jOcr(BufferedImage image) {
        try {
            Tesseract tesseract = initTesseract();
            return tesseract.doOCR(image);
        } catch (Throwable t) {
            log.warn("Tess4J OCR failed with native loader or runtime error: {}", t.getMessage());
            throw new IllegalStateException("Tesseract OCR engine is not installed on this server. " +
                "Analyzing images or scanned PDFs requires Tesseract. " +
                "Please install Tesseract on your system or upload a digital (text-based) PDF report.", t);
        }
    }

    private Tesseract initTesseract() {
        Tesseract tesseract = new Tesseract();
        
        // Configure datapath if env variable is set
        String tessdataPrefix = System.getenv("TESSDATA_PREFIX");
        if (tessdataPrefix != null && !tessdataPrefix.trim().isEmpty()) {
            tesseract.setDatapath(tessdataPrefix);
            log.debug("Setting Tesseract datapath from TESSDATA_PREFIX: {}", tessdataPrefix);
        } else {
            // Check common paths for macOS Homebrew installation
            File commonBrewData = new File("/opt/homebrew/share/tessdata");
            if (commonBrewData.exists()) {
                tesseract.setDatapath(commonBrewData.getAbsolutePath());
            } else {
                File localBrewData = new File("/usr/local/share/tessdata");
                if (localBrewData.exists()) {
                    tesseract.setDatapath(localBrewData.getAbsolutePath());
                }
            }
        }
        
        // Optional configuration properties can be added here
        tesseract.setLanguage("eng"); // default to English
        return tesseract;
    }
}
