package com.example.geminiapp.controller;

import com.example.geminiapp.dto.PromptRequest;
import com.example.geminiapp.dto.PromptResponse;
import com.example.geminiapp.dto.UserQuotaResponse;
import com.example.geminiapp.dto.RequestLogResponse;
import com.example.geminiapp.entity.User;
import com.example.geminiapp.entity.RequestLog;
import com.example.geminiapp.exception.RateLimitExceededException;
import com.example.geminiapp.security.UserPrincipal;
import com.example.geminiapp.service.PromptProcessingService;
import com.example.geminiapp.service.RateLimitingService;
import com.example.geminiapp.service.UserService;
import com.example.geminiapp.service.RequestLoggingService;
import com.example.geminiapp.validation.InputSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class PromptController {

    private static final Logger logger = LoggerFactory.getLogger(PromptController.class);

    @Autowired
    private PromptProcessingService promptProcessingService;

    @Autowired
    private UserService userService;

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private InputSanitizer inputSanitizer;

    @Autowired
    private RequestLoggingService requestLoggingService;

    /**
     * Main prompt submission endpoint
     * Handles both authenticated and anonymous users
     */
    @PostMapping("/prompt")
    public ResponseEntity<PromptResponse> submitPrompt(@Valid @RequestBody PromptRequest promptRequest,
                                                     Authentication authentication,
                                                     HttpServletRequest request) {
        try {
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            logger.info("Received prompt submission from IP: {}", ipAddress);

            // Check rate limiting first
            String rateLimitIdentifier = ipAddress;
            if (authentication != null && authentication.isAuthenticated()) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                rateLimitIdentifier = "user:" + userPrincipal.getId();
            }

            if (!rateLimitingService.isRequestAllowed(rateLimitIdentifier)) {
                RateLimitingService.RateLimitStatus status = rateLimitingService.getRateLimitStatus(rateLimitIdentifier);
                throw new RateLimitExceededException(
                    "Rate limit exceeded. Please try again later.",
                    rateLimitIdentifier,
                    status.getRequestsInLastMinute(),
                    status.getRequestsInLastHour(),
                    status.getRequestsInLastDay(),
                    status.getLimitPerMinute(),
                    status.getLimitPerHour(),
                    status.getLimitPerDay()
                );
            }

            // Sanitize input
            String sanitizedPrompt = inputSanitizer.sanitizePrompt(promptRequest.getPrompt());
            if (inputSanitizer.containsDangerousContent(sanitizedPrompt)) {
                logger.warn("Dangerous content detected in prompt from IP: {}", ipAddress);
                return ResponseEntity.badRequest()
                    .body(PromptResponse.error("Prompt contains potentially harmful content"));
            }
            promptRequest.setPrompt(sanitizedPrompt);

            // Handle authenticated vs anonymous users
            User user = null;
            if (authentication != null && authentication.isAuthenticated()) {
                // Authenticated user
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                Optional<User> userOpt = userService.findById(userPrincipal.getId());
                if (userOpt.isEmpty()) {
                    logger.error("Authenticated user not found: {}", userPrincipal.getId());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(PromptResponse.error("User not found"));
                }
                user = userOpt.get();
            } else {
                // Anonymous user - find or create by fingerprint
                String fingerprint = promptRequest.getFingerprint();
                if (fingerprint == null || fingerprint.trim().isEmpty()) {
                    fingerprint = generateFingerprint(request);
                }
                user = userService.findOrCreateAnonymousUser(fingerprint);
            }

            // Check quota before processing
            PromptProcessingService.QuotaStatus quotaStatus = promptProcessingService.getQuotaStatus(user);
            if (quotaStatus.isExceeded()) {
                String message = user.getIsAnonymous() 
                    ? "Anonymous users are limited to " + quotaStatus.getLimit() + " request(s). Please register for more requests."
                    : "You have exceeded your quota of " + quotaStatus.getLimit() + " requests.";
                
                logger.warn("Quota exceeded for user {}: {}/{}", user.getId(), quotaStatus.getUsed(), quotaStatus.getLimit());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(PromptResponse.error(message));
            }

            // Process the prompt using the service
            PromptResponse response = processPromptForUser(promptRequest, user, ipAddress, userAgent);
            
            if (response.isSuccess()) {
                logger.info("Prompt processed successfully for user: {} (anonymous: {})", 
                    user.getId(), user.getIsAnonymous());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Prompt processing failed for user {}: {}", user.getId(), response.getError());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Error processing prompt submission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PromptResponse.error("An unexpected error occurred. Please try again later."));
        }
    }

    /**
     * Process prompt for a specific user (extracted method to handle the service call properly)
     */
    private PromptResponse processPromptForUser(PromptRequest request, User user, String ipAddress, String userAgent) {
        try {
            // The service expects userPrincipal for authenticated users, null for anonymous
            Object userPrincipal = null;
            if (!user.getIsAnonymous()) {
                // For authenticated users, create a UserPrincipal
                userPrincipal = UserPrincipal.create(user);
            }
            
            // Call the service with the appropriate userPrincipal
            return promptProcessingService.processPrompt(request, userPrincipal, ipAddress, userAgent);
            
        } catch (Exception e) {
            logger.error("Error in prompt processing for user {}", user.getId(), e);
            return PromptResponse.error("Failed to process prompt: " + e.getMessage());
        }
    }



    /**
     * Generate fingerprint from request
     */
    private String generateFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String acceptLanguage = request.getHeader("Accept-Language");
        String remoteAddr = getClientIpAddress(request);
        
        return String.valueOf((userAgent + acceptLanguage + remoteAddr).hashCode());
    }

    /**
     * Get user quota status
     */
    @GetMapping("/user/quota")
    public ResponseEntity<UserQuotaResponse> getUserQuota(Authentication authentication, HttpServletRequest request) {
        try {
            User user = getUserFromAuthentication(authentication, request);
            if (user == null) {
                // For anonymous users, create a temporary user to get quota info
                String fingerprint = generateFingerprint(request);
                user = userService.findOrCreateAnonymousUser(fingerprint);
            }

            PromptProcessingService.QuotaStatus quotaStatus = promptProcessingService.getQuotaStatus(user);
            
            UserQuotaResponse response = new UserQuotaResponse(
                quotaStatus.getRemaining(),
                quotaStatus.getLimit(),
                quotaStatus.isAnonymous()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting user quota", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user request history
     */
    @GetMapping("/user/history")
    public ResponseEntity<List<RequestLogResponse>> getUserHistory(Authentication authentication, 
                                          HttpServletRequest request,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        try {
            User user = getUserFromAuthentication(authentication, request);
            if (user == null) {
                // For anonymous users, create a temporary user to get history
                String fingerprint = generateFingerprint(request);
                user = userService.findOrCreateAnonymousUser(fingerprint);
            }

            // Get user's request history from the logging service
            List<RequestLog> requestLogs = requestLoggingService.getUserRequestHistory(user.getId(), page, size);
            
            List<RequestLogResponse> response = requestLogs.stream()
                .map(log -> new RequestLogResponse(
                    log.getId(),
                    log.getUserPrompt(),
                    log.getSystemPrompt(),
                    log.getAiResponse(),
                    log.getStatus().name(),
                    log.getErrorMessage(),
                    log.getCreatedAt()
                ))
                .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting user history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user profile information
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication, HttpServletRequest request) {
        try {
            User user = getUserFromAuthentication(authentication, request);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
            }

            UserService.UserStats userStats = userService.getUserStats(user);
            PromptProcessingService.QuotaStatus quotaStatus = promptProcessingService.getQuotaStatus(user);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "profile", Map.of(
                    "id", userStats.getId(),
                    "email", userStats.getEmail() != null ? userStats.getEmail() : "",
                    "isAnonymous", userStats.isAnonymous(),
                    "createdAt", userStats.getCreatedAt(),
                    "quota", Map.of(
                        "used", quotaStatus.getUsed(),
                        "limit", quotaStatus.getLimit(),
                        "remaining", quotaStatus.getRemaining()
                    )
                )
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get user profile"));
        }
    }

    /**
     * Helper method to get user from authentication or create anonymous user
     */
    private User getUserFromAuthentication(Authentication authentication, HttpServletRequest request) {
        if (authentication != null && authentication.isAuthenticated()) {
            // Authenticated user
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Optional<User> userOpt = userService.findById(userPrincipal.getId());
            return userOpt.orElse(null);
        } else {
            // Anonymous user - try to find by fingerprint
            String fingerprint = generateFingerprint(request);
            Optional<User> userOpt = userService.findByFingerprint(fingerprint);
            return userOpt.orElse(null);
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}