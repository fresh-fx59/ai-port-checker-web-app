package com.example.geminiapp.service;

import com.example.geminiapp.config.MetricsConfig.ApplicationMetrics;
import com.example.geminiapp.entity.User;
import com.example.geminiapp.entity.RequestLog;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for application monitoring and metrics collection.
 * Integrates with the metrics system to track application performance and usage.
 */
@Service
public class MonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);

    @Autowired
    private ApplicationMetrics applicationMetrics;

    /**
     * Record a prompt request
     */
    public void recordPromptRequest(User user) {
        try {
            applicationMetrics.incrementPromptRequests();
            
            String userType = user.getIsAnonymous() ? "anonymous" : "registered";
            applicationMetrics.incrementPromptRequests(userType);
            
            logger.debug("Recorded prompt request for {} user: {}", userType, user.getId());
        } catch (Exception e) {
            logger.error("Error recording prompt request metrics", e);
        }
    }

    /**
     * Record authentication attempt
     */
    public void recordAuthentication(String method, boolean success) {
        try {
            String result = success ? "success" : "failure";
            applicationMetrics.incrementAuthentication(method, result);
            
            logger.debug("Recorded authentication attempt: method={}, result={}", method, result);
        } catch (Exception e) {
            logger.error("Error recording authentication metrics", e);
        }
    }

    /**
     * Record an error
     */
    public void recordError(String errorType, Exception exception) {
        try {
            applicationMetrics.incrementErrors(errorType);
            
            logger.warn("Recorded error: type={}, message={}", errorType, exception.getMessage());
        } catch (Exception e) {
            logger.error("Error recording error metrics", e);
        }
    }

    /**
     * Start timing a prompt processing operation
     */
    public Timer.Sample startPromptProcessingTimer() {
        try {
            return applicationMetrics.startPromptProcessingTimer();
        } catch (Exception e) {
            logger.error("Error starting prompt processing timer", e);
            return null;
        }
    }

    /**
     * Stop timing a prompt processing operation
     */
    public void stopPromptProcessingTimer(Timer.Sample sample) {
        try {
            if (sample != null) {
                applicationMetrics.recordPromptProcessingTime(sample);
            }
        } catch (Exception e) {
            logger.error("Error stopping prompt processing timer", e);
        }
    }

    /**
     * Record user session start
     */
    public void recordUserSessionStart() {
        try {
            applicationMetrics.incrementActiveUsers();
            logger.debug("User session started, active users incremented");
        } catch (Exception e) {
            logger.error("Error recording user session start", e);
        }
    }

    /**
     * Record user session end
     */
    public void recordUserSessionEnd() {
        try {
            applicationMetrics.decrementActiveUsers();
            logger.debug("User session ended, active users decremented");
        } catch (Exception e) {
            logger.error("Error recording user session end", e);
        }
    }

    /**
     * Record request completion
     */
    public void recordRequestCompletion(RequestLog requestLog) {
        try {
            String status = requestLog.getStatus().toString().toLowerCase();
            
            if ("error".equals(status)) {
                recordError("request_processing", new RuntimeException(requestLog.getErrorMessage()));
            }
            
            logger.debug("Recorded request completion: status={}, user={}", 
                        status, requestLog.getUser().getId());
        } catch (Exception e) {
            logger.error("Error recording request completion metrics", e);
        }
    }

    /**
     * Record quota exceeded event
     */
    public void recordQuotaExceeded(User user) {
        try {
            String userType = user.getIsAnonymous() ? "anonymous" : "registered";
            applicationMetrics.incrementErrors("quota_exceeded_" + userType);
            
            logger.info("Quota exceeded for {} user: {}", userType, user.getId());
        } catch (Exception e) {
            logger.error("Error recording quota exceeded metrics", e);
        }
    }

    /**
     * Record rate limit exceeded event
     */
    public void recordRateLimitExceeded(String identifier) {
        try {
            applicationMetrics.incrementErrors("rate_limit_exceeded");
            
            logger.warn("Rate limit exceeded for identifier: {}", identifier);
        } catch (Exception e) {
            logger.error("Error recording rate limit exceeded metrics", e);
        }
    }

    /**
     * Record API call to external service
     */
    public void recordExternalApiCall(String service, boolean success, long durationMs) {
        try {
            String result = success ? "success" : "failure";
            
            // Record the call
            applicationMetrics.incrementAuthentication(service + "_api_call", result);
            
            logger.debug("Recorded external API call: service={}, result={}, duration={}ms", 
                        service, result, durationMs);
        } catch (Exception e) {
            logger.error("Error recording external API call metrics", e);
        }
    }

    /**
     * Record security event
     */
    public void recordSecurityEvent(String eventType, String details) {
        try {
            applicationMetrics.incrementErrors("security_" + eventType);
            
            logger.warn("Security event recorded: type={}, details={}", eventType, details);
        } catch (Exception e) {
            logger.error("Error recording security event metrics", e);
        }
    }
}