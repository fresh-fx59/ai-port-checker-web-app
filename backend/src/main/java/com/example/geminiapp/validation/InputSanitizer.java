package com.example.geminiapp.validation;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class InputSanitizer {

    // Patterns for sanitization
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?i)<script[^>]*>.*?</script>");
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("(?i)javascript:");
    private static final Pattern VBSCRIPT_PATTERN = Pattern.compile("(?i)vbscript:");
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile("(?i)on\\w+\\s*=");
    private static final Pattern EXCESSIVE_WHITESPACE_PATTERN = Pattern.compile("\\s{3,}");

    /**
     * Sanitize user input by removing potentially harmful content
     */
    public String sanitizePrompt(String input) {
        if (input == null) {
            return null;
        }

        String sanitized = input;

        // Remove script tags and their content
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");

        // Remove HTML tags
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");

        // Remove javascript: and vbscript: protocols
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = VBSCRIPT_PATTERN.matcher(sanitized).replaceAll("");

        // Remove event handlers
        sanitized = EVENT_HANDLER_PATTERN.matcher(sanitized).replaceAll("");

        // Normalize whitespace
        sanitized = EXCESSIVE_WHITESPACE_PATTERN.matcher(sanitized).replaceAll(" ");

        // Trim and normalize line breaks
        sanitized = sanitized.trim();
        sanitized = sanitized.replaceAll("\\r\\n", "\n");
        sanitized = sanitized.replaceAll("\\r", "\n");

        return sanitized;
    }

    /**
     * Sanitize email input
     */
    public String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }

        // Remove any HTML tags and normalize
        String sanitized = HTML_TAG_PATTERN.matcher(email).replaceAll("");
        return sanitized.trim().toLowerCase();
    }

    /**
     * Sanitize general text input
     */
    public String sanitizeText(String text) {
        if (text == null) {
            return null;
        }

        String sanitized = text;

        // Remove HTML tags
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");

        // Remove script content
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");

        // Normalize whitespace
        sanitized = EXCESSIVE_WHITESPACE_PATTERN.matcher(sanitized).replaceAll(" ");

        return sanitized.trim();
    }

    /**
     * Check if input contains potentially dangerous content
     */
    public boolean containsDangerousContent(String input) {
        if (input == null) {
            return false;
        }

        String lowerInput = input.toLowerCase();

        // Check for common attack patterns
        return lowerInput.contains("<script") ||
               lowerInput.contains("javascript:") ||
               lowerInput.contains("vbscript:") ||
               lowerInput.contains("onload=") ||
               lowerInput.contains("onerror=") ||
               lowerInput.contains("onclick=") ||
               lowerInput.contains("eval(") ||
               lowerInput.contains("system(") ||
               lowerInput.contains("exec(") ||
               lowerInput.contains("union select") ||
               lowerInput.contains("drop table") ||
               lowerInput.contains("insert into") ||
               lowerInput.contains("update set") ||
               lowerInput.contains("delete from");
    }
}