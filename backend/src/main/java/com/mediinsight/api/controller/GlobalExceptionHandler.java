package com.mediinsight.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllExceptions(Exception ex) {
        log.error("Unhandled exception caught in REST layer: ", ex);
        
        String message = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        
        // Detect rate limit / quota exceeded issues from external service calls
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof HttpStatusCodeException) {
                HttpStatusCodeException httpEx = (HttpStatusCodeException) cause;
                if (httpEx.getStatusCode().value() == 429) {
                    message = "Gemini API Rate Limit / Quota Exceeded. Under the free tier, requests are limited. Please wait 1 minute before trying again.";
                    status = HttpStatus.TOO_MANY_REQUESTS;
                    break;
                }
            }
            String causeMsg = cause.getMessage();
            if (causeMsg != null && (causeMsg.contains("429") || causeMsg.contains("RESOURCE_EXHAUSTED"))) {
                message = "Gemini API Rate Limit / Quota Exceeded. Under the free tier, requests are limited. Please wait 1 minute before trying again.";
                status = HttpStatus.TOO_MANY_REQUESTS;
                break;
            }
            cause = cause.getCause();
        }
        
        return new ResponseEntity<>(
            Map.of("error", message),
            status
        );
    }
}
