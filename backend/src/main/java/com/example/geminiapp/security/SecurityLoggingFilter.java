package com.example.geminiapp.security;

import com.example.geminiapp.service.SecurityMonitoringService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class SecurityLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityLoggingFilter.class);

    @Autowired
    private SecurityMonitoringService securityMonitoringService;

    // Paths that should be monitored for security
    private static final List<String> MONITORED_PATHS = Arrays.asList(
        "/api/auth/",
        "/api/prompt/",
        "/oauth2/"
    );

    // Suspicious patterns in requests
    private static final List<String> SUSPICIOUS_PATTERNS = Arrays.asList(
        "script", "javascript", "vbscript", "onload", "onerror",
        "union", "select", "insert", "update", "delete", "drop",
        "../", "..\\", "cmd", "exec", "system"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = getClientIpAddress(request);
        
        try {
            // Check if this is a monitored path
            boolean isMonitoredPath = MONITORED_PATHS.stream()
                .anyMatch(path -> requestUri.startsWith(path));
            
            if (isMonitoredPath) {
                // Log the request
                logSecureRequest(request);
                
                // Check for suspicious patterns
                checkSuspiciousPatterns(request);
                
                // Check if IP is already flagged as suspicious
                if (securityMonitoringService.isSuspiciousIp(clientIp)) {
                    securityMonitoringService.logSecurityEvent(request, "SUSPICIOUS_IP_ACCESS", 
                        "Request from flagged IP address");
                }
            }
            
            // Continue with the filter chain
            filterChain.doFilter(request, response);
            
            // Log response information for monitored paths
            if (isMonitoredPath) {
                long duration = System.currentTimeMillis() - startTime;
                logSecureResponse(request, response, duration);
            }
            
        } catch (Exception e) {
            // Log any exceptions during security filtering
            securityMonitoringService.logSecurityEvent(request, "SECURITY_FILTER_ERROR", 
                "Error in security filter: " + e.getMessage());
            
            // Re-throw the exception to maintain normal error handling
            throw e;
        }
    }

    /**
     * Log secure request information
     */
    private void logSecureRequest(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");
        String contentType = request.getContentType();
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Secure Request - ");
        logMessage.append("IP: ").append(clientIp).append(" | ");
        logMessage.append("Method: ").append(request.getMethod()).append(" | ");
        logMessage.append("URI: ").append(request.getRequestURI()).append(" | ");
        logMessage.append("UserAgent: ").append(userAgent != null ? userAgent : "N/A").append(" | ");
        logMessage.append("Referer: ").append(referer != null ? referer : "N/A").append(" | ");
        logMessage.append("ContentType: ").append(contentType != null ? contentType : "N/A");
        
        logger.info(logMessage.toString());
    }

    /**
     * Log secure response information
     */
    private void logSecureResponse(HttpServletRequest request, HttpServletResponse response, long duration) {
        String clientIp = getClientIpAddress(request);
        int statusCode = response.getStatus();
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Secure Response - ");
        logMessage.append("IP: ").append(clientIp).append(" | ");
        logMessage.append("Status: ").append(statusCode).append(" | ");
        logMessage.append("Duration: ").append(duration).append("ms | ");
        logMessage.append("URI: ").append(request.getRequestURI());
        
        logger.info(logMessage.toString());
        
        // Log security events for certain status codes
        if (statusCode == 401) {
            securityMonitoringService.logSecurityEvent(request, "UNAUTHORIZED_ACCESS", 
                "Unauthorized access attempt");
        } else if (statusCode == 403) {
            securityMonitoringService.logSecurityEvent(request, "FORBIDDEN_ACCESS", 
                "Forbidden access attempt");
        } else if (statusCode == 429) {
            securityMonitoringService.logSecurityEvent(request, "RATE_LIMIT_HIT", 
                "Rate limit exceeded");
        } else if (statusCode >= 500) {
            securityMonitoringService.logSecurityEvent(request, "SERVER_ERROR", 
                "Server error occurred: " + statusCode);
        }
    }

    /**
     * Check for suspicious patterns in the request
     */
    private void checkSuspiciousPatterns(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();
        String userAgent = request.getHeader("User-Agent");
        
        // Check URI for suspicious patterns
        if (containsSuspiciousPattern(requestUri)) {
            securityMonitoringService.logSecurityThreat(request, "SUSPICIOUS_URI", 
                "Suspicious pattern in URI: " + requestUri);
        }
        
        // Check query string for suspicious patterns
        if (queryString != null && containsSuspiciousPattern(queryString)) {
            securityMonitoringService.logSecurityThreat(request, "SUSPICIOUS_QUERY", 
                "Suspicious pattern in query string: " + queryString);
        }
        
        // Check user agent for suspicious patterns
        if (userAgent != null && (userAgent.length() < 10 || containsSuspiciousPattern(userAgent))) {
            securityMonitoringService.logSecurityThreat(request, "SUSPICIOUS_USER_AGENT", 
                "Suspicious user agent: " + userAgent);
        }
        
        // Check for missing or suspicious headers
        checkSuspiciousHeaders(request);
    }

    /**
     * Check for suspicious headers
     */
    private void checkSuspiciousHeaders(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String accept = request.getHeader("Accept");
        String acceptLanguage = request.getHeader("Accept-Language");
        
        // Flag requests with no user agent
        if (userAgent == null || userAgent.trim().isEmpty()) {
            securityMonitoringService.logSecurityThreat(request, "MISSING_USER_AGENT", 
                "Request without User-Agent header");
        }
        
        // Flag requests with suspicious accept headers
        if (accept != null && accept.contains("*/*") && !accept.contains("text/html")) {
            // This might be an automated tool
            securityMonitoringService.logSecurityEvent(request, "POTENTIAL_BOT", 
                "Request with bot-like Accept header: " + accept);
        }
        
        // Check for common bot patterns
        if (userAgent != null) {
            String lowerUserAgent = userAgent.toLowerCase();
            if (lowerUserAgent.contains("bot") || lowerUserAgent.contains("crawler") || 
                lowerUserAgent.contains("spider") || lowerUserAgent.contains("scraper")) {
                securityMonitoringService.logSecurityEvent(request, "BOT_DETECTED", 
                    "Bot detected: " + userAgent);
            }
        }
    }

    /**
     * Check if text contains suspicious patterns
     */
    private boolean containsSuspiciousPattern(String text) {
        if (text == null) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        return SUSPICIOUS_PATTERNS.stream()
            .anyMatch(pattern -> lowerText.contains(pattern.toLowerCase()));
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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Don't filter static resources
        return path.startsWith("/static/") || 
               path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/images/") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg") ||
               path.endsWith(".gif") ||
               path.endsWith(".ico") ||
               path.equals("/actuator/health");
    }
}