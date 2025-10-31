package com.example.geminiapp.service;

import com.example.geminiapp.dto.PromptRequest;
import com.example.geminiapp.dto.PromptResponse;
import com.example.geminiapp.entity.User;
import com.example.geminiapp.entity.RequestLog;
import com.example.geminiapp.entity.RequestStatus;
import com.example.geminiapp.exception.InvalidPromptException;
import com.example.geminiapp.exception.QuotaExceededException;
import com.example.geminiapp.exception.GeminiApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PromptProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(PromptProcessingService.class);
    
    // System prompt template
    private static final String SYSTEM_PROMPT = """
            You are an AI assistant that helps users generate comprehensive reports based on their input.
            Please provide detailed, well-structured, and informative responses.
            Format your response in a clear and readable manner with appropriate sections and bullet points where helpful.
            
            User Request:
            """;
    
    // Input validation patterns
    private static final Pattern MALICIOUS_PATTERN = Pattern.compile(
        "(?i)(script|javascript|<script|</script|onclick|onerror|onload|eval\\(|exec\\(|system\\()", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|sp_|xp_)", 
        Pattern.CASE_INSENSITIVE
    );
    
    private final GeminiApiService geminiApiService;
    private final UserService userService;
    private final RequestLoggingService requestLoggingService;
    
    @Value("${app.quota.anonymous-requests:1}")
    private int anonymousRequestLimit;
    
    @Value("${app.quota.registered-requests:5}")
    private int registeredRequestLimit;
    
    @Autowired
    public PromptProcessingService(
            GeminiApiService geminiApiService,
            UserService userService,
            RequestLoggingService requestLoggingService) {
        this.geminiApiService = geminiApiService;
        this.userService = userService;
        this.requestLoggingService = requestLoggingService;
    }
    
    /**
     * Processes a user prompt through the complete pipeline:
     * 1. Input validation and sanitization
     * 2. User identification and quota checking
     * 3. System prompt combination
     * 4. Gemini API call
     * 5. Response logging
     * 
     * @param request The prompt request from the user
     * @param userPrincipal The authenticated user (null for anonymous)
     * @param ipAddress The client IP address
     * @param userAgent The client user agent
     * @return PromptResponse with the generated report or error
     */
    @Transactional
    public PromptResponse processPrompt(PromptRequest request, Object userPrincipal, String ipAddress, String userAgent) {
        logger.info("Processing prompt request from IP: {}", ipAddress);
        
        try {
            // Step 1: Validate and sanitize input
            String sanitizedPrompt = validateAndSanitizePrompt(request.getPrompt());
            
            // Step 2: Identify or create user and check quota
            User user = identifyUser(userPrincipal, request.getFingerprint());
            validateQuota(user);
            
            // Step 3: Combine system prompt with user input
            String fullPrompt = combinePrompts(sanitizedPrompt);
            
            // Step 4: Create request log entry (pending status)
            RequestLog requestLog = requestLoggingService.createPendingRequest(
                user, sanitizedPrompt, fullPrompt, ipAddress, userAgent
            );
            
            try {
                // Step 5: Call Gemini API
                String aiResponse = geminiApiService.generateContent(fullPrompt);
                
                // Step 6: Update request log with success
                requestLoggingService.updateRequestSuccess(requestLog, aiResponse);
                
                // Step 7: Calculate remaining requests
                int remainingRequests = calculateRemainingRequests(user);
                
                logger.info("Successfully processed prompt for user: {} (anonymous: {})", 
                    user.getId(), user.getIsAnonymous());
                
                return PromptResponse.success(aiResponse, remainingRequests, requestLog.getId().toString());
                
            } catch (GeminiApiException e) {
                // Update request log with error
                requestLoggingService.updateRequestError(requestLog, e.getMessage());
                logger.error("Gemini API error for user {}: {}", user.getId(), e.getMessage());
                throw e;
            }
            
        } catch (InvalidPromptException e) {
            logger.warn("Invalid prompt from IP {}: {}", ipAddress, e.getMessage());
            return PromptResponse.error("Invalid prompt: " + e.getValidationError());
        } catch (QuotaExceededException e) {
            logger.warn("Quota exceeded for user from IP {}: {}", ipAddress, e.getMessage());
            return PromptResponse.error(e.getMessage());
        } catch (GeminiApiException e) {
            logger.error("Gemini API error from IP {}: {}", ipAddress, e.getMessage());
            return PromptResponse.error("AI service temporarily unavailable. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error processing prompt from IP {}", ipAddress, e);
            return PromptResponse.error("An unexpected error occurred. Please try again later.");
        }
    }
    
    /**
     * Validates and sanitizes the user prompt
     */
    private String validateAndSanitizePrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new InvalidPromptException("Prompt cannot be empty");
        }
        
        String trimmedPrompt = prompt.trim();
        
        // Check length constraints
        if (trimmedPrompt.length() < 10) {
            throw new InvalidPromptException("Prompt must be at least 10 characters long");
        }
        
        if (trimmedPrompt.length() > 10000) {
            throw new InvalidPromptException("Prompt must not exceed 10,000 characters");
        }
        
        // Check for malicious content
        if (MALICIOUS_PATTERN.matcher(trimmedPrompt).find()) {
            throw new InvalidPromptException("Prompt contains potentially malicious content");
        }
        
        if (SQL_INJECTION_PATTERN.matcher(trimmedPrompt).find()) {
            throw new InvalidPromptException("Prompt contains potentially harmful SQL patterns");
        }
        
        // Basic sanitization - remove control characters but preserve newlines and tabs
        String sanitized = trimmedPrompt.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        
        // Remove excessive whitespace
        sanitized = sanitized.replaceAll("\\s{3,}", "  ");
        
        logger.debug("Prompt validated and sanitized. Original length: {}, Sanitized length: {}", 
            prompt.length(), sanitized.length());
        
        return sanitized;
    }
    
    /**
     * Identifies the user (authenticated or anonymous) and ensures they exist in the database
     */
    private User identifyUser(Object userPrincipal, String fingerprint) {
        if (userPrincipal != null) {
            // Authenticated user - extract from UserPrincipal
            if (userPrincipal instanceof com.example.geminiapp.security.UserPrincipal) {
                com.example.geminiapp.security.UserPrincipal principal = 
                    (com.example.geminiapp.security.UserPrincipal) userPrincipal;
                
                Optional<User> userOpt = userService.findById(principal.getId());
                if (userOpt.isEmpty()) {
                    throw new InvalidPromptException("Authenticated user not found in database");
                }
                return userOpt.get();
            } else {
                throw new InvalidPromptException("Invalid user principal type");
            }
        } else {
            // Anonymous user - find or create by fingerprint
            if (fingerprint == null || fingerprint.trim().isEmpty()) {
                throw new InvalidPromptException("Anonymous users must provide a browser fingerprint");
            }
            
            return userService.findOrCreateAnonymousUser(fingerprint);
        }
    }
    
    /**
     * Validates that the user has not exceeded their request quota
     */
    private void validateQuota(User user) {
        int requestCount = requestLoggingService.getSuccessfulRequestCount(user);
        int limit = user.getIsAnonymous() ? anonymousRequestLimit : registeredRequestLimit;
        
        if (requestCount >= limit) {
            String message = user.getIsAnonymous() 
                ? "Anonymous users are limited to " + anonymousRequestLimit + " request(s). Please register for more requests."
                : "You have exceeded your quota of " + registeredRequestLimit + " requests.";
            
            throw new QuotaExceededException(message, 0, user.getIsAnonymous());
        }
        
        logger.debug("Quota check passed for user {}: {}/{} requests used", 
            user.getId(), requestCount, limit);
    }
    
    /**
     * Combines the system prompt with the user prompt
     */
    private String combinePrompts(String userPrompt) {
        return SYSTEM_PROMPT + userPrompt;
    }
    
    /**
     * Calculates remaining requests for the user
     */
    private int calculateRemainingRequests(User user) {
        int requestCount = requestLoggingService.getSuccessfulRequestCount(user);
        int limit = user.getIsAnonymous() ? anonymousRequestLimit : registeredRequestLimit;
        return Math.max(0, limit - requestCount - 1); // -1 because we just used one
    }
    
    /**
     * Gets the current quota status for a user
     */
    public QuotaStatus getQuotaStatus(User user) {
        int requestCount = requestLoggingService.getSuccessfulRequestCount(user);
        int limit = user.getIsAnonymous() ? anonymousRequestLimit : registeredRequestLimit;
        int remaining = Math.max(0, limit - requestCount);
        
        return new QuotaStatus(requestCount, limit, remaining, user.getIsAnonymous());
    }
    
    /**
     * DTO for quota status information
     */
    public static class QuotaStatus {
        private final int used;
        private final int limit;
        private final int remaining;
        private final boolean isAnonymous;
        
        public QuotaStatus(int used, int limit, int remaining, boolean isAnonymous) {
            this.used = used;
            this.limit = limit;
            this.remaining = remaining;
            this.isAnonymous = isAnonymous;
        }
        
        public int getUsed() { return used; }
        public int getLimit() { return limit; }
        public int getRemaining() { return remaining; }
        public boolean isAnonymous() { return isAnonymous; }
        public boolean isExceeded() { return remaining <= 0; }
    }
}