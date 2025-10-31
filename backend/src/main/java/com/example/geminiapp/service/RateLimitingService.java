package com.example.geminiapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RateLimitingService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);

    @Value("${app.rate-limit.requests-per-minute:10}")
    private int requestsPerMinute;

    @Value("${app.rate-limit.requests-per-hour:100}")
    private int requestsPerHour;

    @Value("${app.rate-limit.requests-per-day:1000}")
    private int requestsPerDay;

    // In-memory storage for rate limiting (in production, use Redis)
    private final ConcurrentMap<String, UserRateLimit> rateLimitMap = new ConcurrentHashMap<>();

    /**
     * Check if a request is allowed for the given identifier (IP address or user ID)
     */
    public boolean isRequestAllowed(String identifier) {
        UserRateLimit userLimit = rateLimitMap.computeIfAbsent(identifier, k -> new UserRateLimit());
        
        LocalDateTime now = LocalDateTime.now();
        
        // Clean up old entries
        userLimit.cleanupOldEntries(now);
        
        // Check rate limits
        if (userLimit.getRequestsInLastMinute(now) >= requestsPerMinute) {
            logger.warn("Rate limit exceeded for {}: {} requests in last minute", identifier, userLimit.getRequestsInLastMinute(now));
            return false;
        }
        
        if (userLimit.getRequestsInLastHour(now) >= requestsPerHour) {
            logger.warn("Rate limit exceeded for {}: {} requests in last hour", identifier, userLimit.getRequestsInLastHour(now));
            return false;
        }
        
        if (userLimit.getRequestsInLastDay(now) >= requestsPerDay) {
            logger.warn("Rate limit exceeded for {}: {} requests in last day", identifier, userLimit.getRequestsInLastDay(now));
            return false;
        }
        
        // Record the request
        userLimit.recordRequest(now);
        return true;
    }

    /**
     * Get rate limit status for an identifier
     */
    public RateLimitStatus getRateLimitStatus(String identifier) {
        UserRateLimit userLimit = rateLimitMap.get(identifier);
        if (userLimit == null) {
            return new RateLimitStatus(0, 0, 0, requestsPerMinute, requestsPerHour, requestsPerDay);
        }

        LocalDateTime now = LocalDateTime.now();
        userLimit.cleanupOldEntries(now);

        return new RateLimitStatus(
            userLimit.getRequestsInLastMinute(now),
            userLimit.getRequestsInLastHour(now),
            userLimit.getRequestsInLastDay(now),
            requestsPerMinute,
            requestsPerHour,
            requestsPerDay
        );
    }

    /**
     * Reset rate limit for an identifier (admin function)
     */
    public void resetRateLimit(String identifier) {
        rateLimitMap.remove(identifier);
        logger.info("Rate limit reset for identifier: {}", identifier);
    }

    /**
     * Inner class to track user rate limits
     */
    private static class UserRateLimit {
        private final ConcurrentMap<LocalDateTime, Integer> requestTimestamps = new ConcurrentHashMap<>();

        void recordRequest(LocalDateTime timestamp) {
            // Round to the nearest second for grouping
            LocalDateTime roundedTime = timestamp.truncatedTo(ChronoUnit.SECONDS);
            requestTimestamps.merge(roundedTime, 1, Integer::sum);
        }

        int getRequestsInLastMinute(LocalDateTime now) {
            LocalDateTime oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
            return requestTimestamps.entrySet().stream()
                .filter(entry -> entry.getKey().isAfter(oneMinuteAgo))
                .mapToInt(entry -> entry.getValue())
                .sum();
        }

        int getRequestsInLastHour(LocalDateTime now) {
            LocalDateTime oneHourAgo = now.minus(1, ChronoUnit.HOURS);
            return requestTimestamps.entrySet().stream()
                .filter(entry -> entry.getKey().isAfter(oneHourAgo))
                .mapToInt(entry -> entry.getValue())
                .sum();
        }

        int getRequestsInLastDay(LocalDateTime now) {
            LocalDateTime oneDayAgo = now.minus(1, ChronoUnit.DAYS);
            return requestTimestamps.entrySet().stream()
                .filter(entry -> entry.getKey().isAfter(oneDayAgo))
                .mapToInt(entry -> entry.getValue())
                .sum();
        }

        void cleanupOldEntries(LocalDateTime now) {
            LocalDateTime oneDayAgo = now.minus(1, ChronoUnit.DAYS);
            requestTimestamps.entrySet().removeIf(entry -> entry.getKey().isBefore(oneDayAgo));
        }
    }

    /**
     * Rate limit status information
     */
    public static class RateLimitStatus {
        private final int requestsInLastMinute;
        private final int requestsInLastHour;
        private final int requestsInLastDay;
        private final int limitPerMinute;
        private final int limitPerHour;
        private final int limitPerDay;

        public RateLimitStatus(int requestsInLastMinute, int requestsInLastHour, int requestsInLastDay,
                              int limitPerMinute, int limitPerHour, int limitPerDay) {
            this.requestsInLastMinute = requestsInLastMinute;
            this.requestsInLastHour = requestsInLastHour;
            this.requestsInLastDay = requestsInLastDay;
            this.limitPerMinute = limitPerMinute;
            this.limitPerHour = limitPerHour;
            this.limitPerDay = limitPerDay;
        }

        public boolean isLimitExceeded() {
            return requestsInLastMinute >= limitPerMinute ||
                   requestsInLastHour >= limitPerHour ||
                   requestsInLastDay >= limitPerDay;
        }

        // Getters
        public int getRequestsInLastMinute() { return requestsInLastMinute; }
        public int getRequestsInLastHour() { return requestsInLastHour; }
        public int getRequestsInLastDay() { return requestsInLastDay; }
        public int getLimitPerMinute() { return limitPerMinute; }
        public int getLimitPerHour() { return limitPerHour; }
        public int getLimitPerDay() { return limitPerDay; }
        public int getRemainingMinute() { return Math.max(0, limitPerMinute - requestsInLastMinute); }
        public int getRemainingHour() { return Math.max(0, limitPerHour - requestsInLastHour); }
        public int getRemainingDay() { return Math.max(0, limitPerDay - requestsInLastDay); }
    }
}