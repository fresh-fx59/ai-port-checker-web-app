package com.example.geminiapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class SecurityMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityMonitoringService.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");

    // Track suspicious activities
    private final ConcurrentMap<String, SuspiciousActivity> suspiciousActivities = new ConcurrentHashMap<>();

    /**
     * Log security-related request information
     */
    public void logSecurityEvent(HttpServletRequest request, String eventType, String details) {
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        Map<String, Object> securityEvent = new HashMap<>();
        securityEvent.put("timestamp", LocalDateTime.now());
        securityEvent.put("eventType", eventType);
        securityEvent.put("clientIp", clientIp);
        securityEvent.put("userAgent", userAgent);
        securityEvent.put("method", method);
        securityEvent.put("uri", requestUri);
        securityEvent.put("details", details);

        // Log to security logger
        securityLogger.info("Security Event: {} | IP: {} | URI: {} {} | Details: {} | UserAgent: {}", 
            eventType, clientIp, method, requestUri, details, userAgent);

        // Check for suspicious patterns
        checkSuspiciousActivity(clientIp, eventType, details);
    }

    /**
     * Log authentication events
     */
    public void logAuthenticationEvent(HttpServletRequest request, String eventType, String username, boolean success) {
        String clientIp = getClientIpAddress(request);
        String details = String.format("User: %s, Success: %s", username != null ? username : "anonymous", success);
        
        logSecurityEvent(request, "AUTH_" + eventType, details);

        if (!success) {
            trackFailedAuthentication(clientIp);
        }
    }

    /**
     * Log validation failures
     */
    public void logValidationFailure(HttpServletRequest request, String validationType, String details) {
        logSecurityEvent(request, "VALIDATION_FAILURE", validationType + ": " + details);
        
        String clientIp = getClientIpAddress(request);
        trackSuspiciousActivity(clientIp, "VALIDATION_FAILURE");
    }

    /**
     * Log rate limiting events
     */
    public void logRateLimitEvent(HttpServletRequest request, String identifier, String limitType) {
        String details = String.format("Identifier: %s, LimitType: %s", identifier, limitType);
        logSecurityEvent(request, "RATE_LIMIT_EXCEEDED", details);
        
        String clientIp = getClientIpAddress(request);
        trackSuspiciousActivity(clientIp, "RATE_LIMIT_EXCEEDED");
    }

    /**
     * Log potential security threats
     */
    public void logSecurityThreat(HttpServletRequest request, String threatType, String details) {
        String clientIp = getClientIpAddress(request);
        
        logSecurityEvent(request, "SECURITY_THREAT", threatType + ": " + details);
        
        // Log request headers for analysis
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        
        securityLogger.warn("Security Threat Detected - IP: {} | Type: {} | Details: {} | Headers: {}", 
            clientIp, threatType, details, headers);
        
        trackSuspiciousActivity(clientIp, "SECURITY_THREAT");
    }

    /**
     * Check if an IP address has suspicious activity
     */
    public boolean isSuspiciousIp(String ipAddress) {
        SuspiciousActivity activity = suspiciousActivities.get(ipAddress);
        if (activity == null) {
            return false;
        }

        // Clean up old entries
        activity.cleanupOldEntries();
        
        // Consider suspicious if multiple different types of violations in short time
        return activity.getViolationCount() > 10 || activity.getUniqueViolationTypes() > 3;
    }

    /**
     * Get security statistics for monitoring
     */
    public SecurityStats getSecurityStats() {
        int totalSuspiciousIps = suspiciousActivities.size();
        int activeSuspiciousIps = (int) suspiciousActivities.values().stream()
            .filter(activity -> {
                activity.cleanupOldEntries();
                return activity.getViolationCount() > 0;
            })
            .count();

        return new SecurityStats(totalSuspiciousIps, activeSuspiciousIps);
    }

    /**
     * Track failed authentication attempts
     */
    private void trackFailedAuthentication(String ipAddress) {
        SuspiciousActivity activity = suspiciousActivities.computeIfAbsent(ipAddress, k -> new SuspiciousActivity());
        activity.addViolation("FAILED_AUTH");
        
        if (activity.getViolationCount() > 5) {
            securityLogger.warn("Multiple failed authentication attempts from IP: {} (Count: {})", 
                ipAddress, activity.getViolationCount());
        }
    }

    /**
     * Track suspicious activity patterns
     */
    private void trackSuspiciousActivity(String ipAddress, String violationType) {
        SuspiciousActivity activity = suspiciousActivities.computeIfAbsent(ipAddress, k -> new SuspiciousActivity());
        activity.addViolation(violationType);
    }

    /**
     * Check for suspicious activity patterns
     */
    private void checkSuspiciousActivity(String ipAddress, String eventType, String details) {
        // Check for common attack patterns
        if (details != null) {
            String lowerDetails = details.toLowerCase();
            
            if (lowerDetails.contains("sql") || lowerDetails.contains("union") || lowerDetails.contains("select")) {
                logSecurityThreat(null, "POTENTIAL_SQL_INJECTION", details);
            }
            
            if (lowerDetails.contains("script") || lowerDetails.contains("javascript") || lowerDetails.contains("xss")) {
                logSecurityThreat(null, "POTENTIAL_XSS", details);
            }
            
            if (lowerDetails.contains("../") || lowerDetails.contains("..\\") || lowerDetails.contains("path traversal")) {
                logSecurityThreat(null, "POTENTIAL_PATH_TRAVERSAL", details);
            }
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

    /**
     * Inner class to track suspicious activity for an IP
     */
    private static class SuspiciousActivity {
        private final Map<String, Integer> violationCounts = new ConcurrentHashMap<>();
        private final Map<String, LocalDateTime> lastViolationTimes = new ConcurrentHashMap<>();

        void addViolation(String violationType) {
            violationCounts.merge(violationType, 1, Integer::sum);
            lastViolationTimes.put(violationType, LocalDateTime.now());
        }

        int getViolationCount() {
            return violationCounts.values().stream().mapToInt(Integer::intValue).sum();
        }

        int getUniqueViolationTypes() {
            return violationCounts.size();
        }

        void cleanupOldEntries() {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
            lastViolationTimes.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
            violationCounts.keySet().retainAll(lastViolationTimes.keySet());
        }
    }

    /**
     * Security statistics
     */
    public static class SecurityStats {
        private final int totalSuspiciousIps;
        private final int activeSuspiciousIps;

        public SecurityStats(int totalSuspiciousIps, int activeSuspiciousIps) {
            this.totalSuspiciousIps = totalSuspiciousIps;
            this.activeSuspiciousIps = activeSuspiciousIps;
        }

        public int getTotalSuspiciousIps() { return totalSuspiciousIps; }
        public int getActiveSuspiciousIps() { return activeSuspiciousIps; }
    }
}