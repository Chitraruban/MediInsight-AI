package com.mediinsight.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    @Value("${GEMINI_API_KEY:}")
    private String apiKey;

    @Value("${GEMINI_MODEL:gemini-2.5-pro}")
    private String geminiModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String getGeminiUrl() {
        String model = (geminiModel == null || geminiModel.trim().isEmpty()) ? "gemini-2.5-pro" : geminiModel;
        return "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=";
    }

    private static final String SYSTEM_PROMPT = 
        "You are a clinical-language engine for MediInsight AI, an educational app. Never diagnose. " +
        "Analyze the extracted lab/health report text and respond with ONLY raw JSON, no markdown, no commentary, matching exactly: " +
        "{ healthScore: integer 0-100, overallStatus: one of Normal/Mild Concerns/Abnormal/Unclear, summary: 2-3 plain-English sentences, " +
        "riskLevel: Low/Medium/High, values: array of up to 9 items {name, value, unit, normalRange, status: normal/high/low, " +
        "termMeaning: max 14 words, explanation: max 22 words}, risks: array up to 4 {condition, relatedTo, severity: Low/Medium/High, " +
        "explanation: max 22 words} (empty array if nothing abnormal), lifestyle: {food: array 3-5 short items, exercise: array 2-4, " +
        "hydration: one sentence, sleep: one sentence, stress: array 2-3}, doctorQuestions: array of 4-6 short questions, " +
        "checklist: array of 4-6 short reminders, disclaimer: 'This analysis is for educational purposes only and is not a substitute " +
        "for professional medical consultation.' }. If the text doesn't look like a medical report, set overallStatus to Unclear, " +
        "healthScore 0, empty arrays, and explain in summary.";

    public GeminiService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            // Retrieve directly from env var if Spring configuration property was not populated
            apiKey = System.getenv("GEMINI_API_KEY");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("=========================================================================");
            log.warn("WARNING: GEMINI_API_KEY environment variable is not set!");
            log.warn("API calls to Gemini will fail. Please set the environment variable.");
            log.warn("=========================================================================");
        } else {
            log.info("Gemini AI API Key found and configured.");
        }
    }

    public Map<String, Object> analyzeReport(String reportText) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Gemini API key is missing. Please set the GEMINI_API_KEY environment variable.");
        }

        try {
            // Make the first call
            String rawResponseText = callGemini(reportText, null, null);
            String cleanJson = cleanJsonResponse(rawResponseText);
            try {
                return objectMapper.readValue(cleanJson, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse Gemini response as JSON. Retrying. Error: {}", e.getMessage());
                log.debug("Raw bad response content: {}", rawResponseText);

                // Retry once with history
                String retryResponseText = callGemini(reportText, rawResponseText, "Return ONLY valid JSON, no other text.");
                String cleanRetryJson = cleanJsonResponse(retryResponseText);
                return objectMapper.readValue(cleanRetryJson, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.error("Error invoking Gemini API: ", e);
            throw new RuntimeException("AI Analysis failed: " + e.getMessage(), e);
        }
    }

    /**
     * Analyzes a medical report image directly using Gemini's vision capability.
     * This produces far better results than OCR + text analysis for image uploads.
     */
    public Map<String, Object> analyzeReportImage(byte[] imageBytes, String mimeType) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Gemini API key is missing. Please set the GEMINI_API_KEY environment variable.");
        }

        try {
            String rawResponseText = callGeminiWithImage(imageBytes, mimeType);
            String cleanJson = cleanJsonResponse(rawResponseText);
            try {
                return objectMapper.readValue(cleanJson, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse Gemini vision response as JSON. Retrying. Error: {}", e.getMessage());
                // Retry with text-only follow-up
                String retryResponseText = callGemini(
                    "The previous image analysis produced invalid JSON. Please re-analyze and return ONLY valid JSON.",
                    rawResponseText, "Return ONLY valid JSON matching the required schema, no other text.");
                String cleanRetryJson = cleanJsonResponse(retryResponseText);
                return objectMapper.readValue(cleanRetryJson, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.error("Error invoking Gemini Vision API: ", e);
            throw new RuntimeException("AI Image Analysis failed: " + e.getMessage(), e);
        }
    }

    public String generateChatReply(String contextSummary, String currentMessage, List<Map<String, String>> chatHistory) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Gemini API key is missing. Please set the GEMINI_API_KEY environment variable.");
        }

        try {
            // System prompt for chat
            String systemInstruction = "You are MediInsight AI, an empathetic health education assistant. " +
                "You help users understand their health and medical reports. Always prioritize safety. " +
                "Provide educational information, never formal medical diagnosis. Under no circumstances should you " +
                "recommend specific prescription drug dosages or override a doctor's advice. Always keep explanations plain and accessible. " +
                "Use the following medical report summary as context if available:\n" + 
                (contextSummary != null ? contextSummary : "No medical report uploaded yet.");

            // Build request contents
            List<Map<String, Object>> contents = new ArrayList<>();
            
            // Add history
            if (chatHistory != null) {
                for (Map<String, String> msg : chatHistory) {
                    String role = msg.get("role");
                    String content = msg.get("content");
                    if (role != null && content != null) {
                        // Gemini roles: user, model
                        String geminiRole = role.equalsIgnoreCase("assistant") ? "model" : "user";
                        contents.add(buildContentPart(geminiRole, content));
                    }
                }
            }

            // Add current message
            contents.add(buildContentPart("user", currentMessage));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
            requestBody.put("contents", contents);
            requestBody.put("generationConfig", Map.of("maxOutputTokens", 1000, "temperature", 0.7));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = getGeminiUrl() + apiKey;
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            return extractTextFromResponse(response.getBody());
        } catch (Exception e) {
            log.error("Error calling Gemini Chat: ", e);
            throw new RuntimeException("AI chat response failed: " + e.getMessage(), e);
        }
    }

    private String callGemini(String userPrompt, String previousResponse, String followUpMessage) {
        List<Map<String, Object>> contents = new ArrayList<>();

        if (previousResponse == null) {
            // First attempt
            contents.add(buildContentPart("user", "Here is the medical report text:\n\n" + userPrompt));
        } else {
            // Retry attempt
            contents.add(buildContentPart("user", "Here is the medical report text:\n\n" + userPrompt));
            contents.add(buildContentPart("model", previousResponse));
            contents.add(buildContentPart("user", followUpMessage));
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("system_instruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))));
        requestBody.put("contents", contents);
        
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("maxOutputTokens", 8192);
        generationConfig.put("temperature", 0.2);
        generationConfig.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = getGeminiUrl() + apiKey;
        
        // Retry up to 5 times with delay on rate limit or service unavailable errors
        int maxRetries = 5;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
                return extractTextFromResponse(response.getBody());
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().value() == 429 || e.getStatusCode().value() == 503) {
                    if (attempt < maxRetries) {
                        log.warn("API overloaded (429/503) from Gemini API (attempt {}/{}). Waiting 5 seconds before retry...", attempt, maxRetries);
                        try {
                            Thread.sleep(5000); // Wait 5 seconds to provide better UX
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while waiting for API retry", ie);
                        }
                    } else {
                        log.error("API retry limit exceeded after {} attempts.", maxRetries);
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("Failed to call Gemini API after " + maxRetries + " attempts");
    }

    private Map<String, Object> buildContentPart(String role, String text) {
        return Map.of(
            "role", role,
            "parts", List.of(Map.of("text", text))
        );
    }

    /**
     * Calls Gemini API with an image (inline_data) for vision-based analysis.
     */
    private String callGeminiWithImage(byte[] imageBytes, String mimeType) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Build parts: image + text prompt
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("inline_data", Map.of("mime_type", mimeType, "data", base64Image)));
        parts.add(Map.of("text", "Analyze this medical report image. Extract all lab values, biomarkers, and health data visible in the image."));

        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("parts", parts);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("system_instruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))));
        requestBody.put("contents", List.of(content));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("maxOutputTokens", 8192);
        generationConfig.put("temperature", 0.2);
        generationConfig.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = getGeminiUrl() + apiKey;

        // Retry up to 5 times with delay on rate limit or service unavailable errors
        int maxRetries = 5;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
                return extractTextFromResponse(response.getBody());
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().value() == 429 || e.getStatusCode().value() == 503) {
                    if (attempt < maxRetries) {
                        log.warn("API overloaded (429/503) by Gemini Vision API (attempt {}/{}). Waiting 5 seconds...", attempt, maxRetries);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while waiting for API retry", ie);
                        }
                    } else {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("Failed to call Gemini Vision API after " + maxRetries + " attempts");
    }

    private String extractTextFromResponse(Map responseBody) {
        try {
            if (responseBody == null) return "";
            List candidates = (List) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) return "";
            Map candidate = (Map) candidates.get(0);
            Map content = (Map) candidate.get("content");
            List parts = (List) content.get("parts");
            if (parts == null || parts.isEmpty()) return "";
            
            StringBuilder sb = new StringBuilder();
            for (Object partObj : parts) {
                if (partObj instanceof Map) {
                    Map part = (Map) partObj;
                    // Skip internal "thought" parts produced by thinking models
                    Object thought = part.get("thought");
                    if (Boolean.TRUE.equals(thought)) continue;
                    String text = (String) part.get("text");
                    if (text != null) {
                        sb.append(text);
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to parse response body structure: ", e);
            throw new RuntimeException("Invalid response structure from Gemini API", e);
        }
    }

    private String cleanJsonResponse(String rawResponse) {
        if (rawResponse == null) return "";
        String clean = rawResponse.trim();
        // Strip markdown code fences
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        clean = clean.trim();
        // Find the first '{' to skip any leading garbage (e.g. thought signatures)
        int braceIdx = clean.indexOf('{');
        if (braceIdx > 0) {
            clean = clean.substring(braceIdx);
        }
        return clean.trim();
    }
}
