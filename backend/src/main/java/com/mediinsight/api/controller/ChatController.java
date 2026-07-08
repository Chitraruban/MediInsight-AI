package com.mediinsight.api.controller;

import com.mediinsight.api.dto.ChatRequest;
import com.mediinsight.api.dto.ChatResponse;
import com.mediinsight.api.model.ChatMessage;
import com.mediinsight.api.model.Report;
import com.mediinsight.api.repository.ChatMessageRepository;
import com.mediinsight.api.repository.ReportRepository;
import com.mediinsight.api.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ReportRepository reportRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final GeminiService geminiService;

    public ChatController(ReportRepository reportRepository,
                          ChatMessageRepository chatMessageRepository,
                          GeminiService geminiService) {
        this.reportRepository = reportRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.geminiService = geminiService;
    }

    /**
     * POST /api/chat
     * Receives message and history, queries Gemini with report context (if provided),
     * stores the dialogue in db, and returns assistant reply.
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("Received chat query. Linked report ID: {}", request.getReportId());

        Report report = null;
        String contextSummary = null;
        if (request.getReportId() != null) {
            report = reportRepository.findById(request.getReportId())
                    .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + request.getReportId()));
            contextSummary = report.getSummary();
        }

        // 1. Generate reply from Gemini
        String reply = geminiService.generateChatReply(contextSummary, request.getMessage(), request.getHistory());

        // 2. Save dialogue history
        ChatMessage userMsg = new ChatMessage(report, "USER", request.getMessage());
        ChatMessage assistantMsg = new ChatMessage(report, "ASSISTANT", reply);
        
        chatMessageRepository.save(userMsg);
        chatMessageRepository.save(assistantMsg);

        return ResponseEntity.ok(new ChatResponse(reply));
    }

    /**
     * GET /api/chat/history/{reportId}
     * Helper endpoint to fetch previous chat history for a report, or global if reportId is null/absent.
     */
    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@RequestParam(name = "reportId", required = false) Long reportId) {
        log.info("Fetching chat history. Report ID: {}", reportId);
        List<ChatMessage> history;
        if (reportId != null) {
            history = chatMessageRepository.findByReportIdOrderByCreatedAtAsc(reportId);
        } else {
            history = chatMessageRepository.findByReportIsNullOrderByCreatedAtAsc();
        }
        return ResponseEntity.ok(history);
    }
}
