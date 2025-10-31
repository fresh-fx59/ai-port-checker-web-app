package com.example.geminiapp.service;


import com.example.geminiapp.repository.UserRepository;
import com.example.geminiapp.repository.RequestLogRepository;
import com.example.geminiapp.entity.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled monitoring service for periodic health checks and metrics collection.
 */
@Service
public class ScheduledMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledMonitoringService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private MonitoringService monitoringService;

    /**
     * Collect and log system metrics every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void collectSystemMetrics() {
        try {
            // Memory metrics
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            logger.info("JVM memory metrics - used: {}, total: {}, usage: {}%", 
                       usedMemory, totalMemory, (double) usedMemory / totalMemory * 100);

            // Thread metrics
            logger.info("JVM threads active: {}", Thread.activeCount());

            // User metrics
            long totalUsers = userRepository.count();
            long anonymousUsers = userRepository.countByIsAnonymousTrue();
            long registeredUsers = totalUsers - anonymousUsers;
            
            logger.info("User metrics - total: {}, anonymous: {}, registered: {}", 
                       totalUsers, anonymousUsers, registeredUsers);

            // Request metrics
            long totalRequests = requestLogRepository.count();
            long successfulRequests = requestLogRepository.countByStatus(RequestStatus.SUCCESS);
            long failedRequests = requestLogRepository.countByStatus(RequestStatus.ERROR);
            long pendingRequests = requestLogRepository.countByStatus(RequestStatus.PENDING);
            
            logger.info("Request metrics - total: {}, successful: {}, failed: {}, pending: {}", 
                       totalRequests, successfulRequests, failedRequests, pendingRequests);

            if (totalRequests > 0) {
                logger.info("Request success rate: {}%", 
                           (double) successfulRequests / totalRequests * 100);
            }

            logger.debug("System metrics collected successfully");

        } catch (Exception e) {
            logger.error("Error collecting system metrics", e);
            monitoringService.recordError("metrics_collection", e);
        }
    }

    /**
     * Check for stuck pending requests every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void checkStuckRequests() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minus(30, ChronoUnit.MINUTES);
            var stuckRequests = requestLogRepository.findStuckPendingRequests(cutoffTime);
            
            if (!stuckRequests.isEmpty()) {
                logger.warn("Found {} stuck pending requests older than 30 minutes", stuckRequests.size());
                
                logger.warn("Stuck requests metric: {}", stuckRequests.size());
                
                // Log security event for investigation
                logger.warn("Security event - stuck requests detected: Found {} stuck requests", 
                           stuckRequests.size());
                
                monitoringService.recordError("stuck_requests", 
                                            new RuntimeException("Found " + stuckRequests.size() + " stuck requests"));
            }

        } catch (Exception e) {
            logger.error("Error checking for stuck requests", e);
            monitoringService.recordError("stuck_request_check", e);
        }
    }

    /**
     * Log quota usage statistics every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void logQuotaStatistics() {
        try {
            long anonymousUsers = userRepository.countByIsAnonymousTrue();
            long registeredUsers = userRepository.count() - anonymousUsers;
            
            // Calculate quota usage
            long totalRequests = requestLogRepository.count();
            long maxPossibleRequests = (anonymousUsers * 1) + (registeredUsers * 5);
            
            if (maxPossibleRequests > 0) {
                double quotaUtilization = (double) totalRequests / maxPossibleRequests * 100;
                logger.info("Quota usage - overall: {}/{} requests", totalRequests, maxPossibleRequests);
                
                logger.info("Quota utilization: {}% ({}/{} requests)", 
                           quotaUtilization, totalRequests, maxPossibleRequests);
            }

            // Log today's usage
            long todayRequests = requestLogRepository.countTodayRequests();
            logger.info("Requests today: {}", todayRequests);

        } catch (Exception e) {
            logger.error("Error logging quota statistics", e);
            monitoringService.recordError("quota_statistics", e);
        }
    }

    /**
     * Perform health checks every 2 minutes
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    public void performHealthChecks() {
        try {
            // Check database connectivity
            long userCount = userRepository.count();
            logger.info("Health check database metric: {}", userCount >= 0 ? 1 : 0);

            // Check memory health
            Runtime runtime = Runtime.getRuntime();
            double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory() * 100;
            
            if (memoryUsage > 90) {
                logger.warn("High memory usage detected: {}%", memoryUsage);
                logger.warn("Security event - high memory usage: Memory usage: {}%", memoryUsage);
            }
            
            logger.info("Health check memory metric: {}", memoryUsage < 90 ? 1 : 0);

            logger.debug("Health checks completed successfully");

        } catch (Exception e) {
            logger.error("Error performing health checks", e);
            monitoringService.recordError("health_check", e);
            logger.error("Health check overall metric: 0");
        }
    }

    /**
     * Clean up old metrics logs daily
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupOldLogs() {
        try {
            // This is a placeholder for log cleanup logic
            // In a real implementation, you might want to:
            // 1. Archive old log files
            // 2. Clean up old database records
            // 3. Compress historical data
            
            logger.info("Daily log cleanup task executed");
            logger.info("Audit event - log cleanup: system completed logs cleanup");

        } catch (Exception e) {
            logger.error("Error during log cleanup", e);
            monitoringService.recordError("log_cleanup", e);
        }
    }

    /**
     * Generate daily summary report
     */
    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    public void generateDailySummary() {
        try {
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            LocalDateTime today = LocalDateTime.now();
            
            // Get yesterday's statistics
            var yesterdayRequests = requestLogRepository.findRequestsBetweenDates(
                yesterday.withHour(0).withMinute(0).withSecond(0),
                yesterday.withHour(23).withMinute(59).withSecond(59)
            );
            
            long successfulRequests = yesterdayRequests.stream()
                .mapToLong(r -> r.getStatus() == RequestStatus.SUCCESS ? 1 : 0)
                .sum();
            
            long failedRequests = yesterdayRequests.stream()
                .mapToLong(r -> r.getStatus() == RequestStatus.ERROR ? 1 : 0)
                .sum();
            
            logger.info("Daily summary for {}: {} total requests, {} successful, {} failed", 
                       yesterday.toLocalDate(), 
                       yesterdayRequests.size(), 
                       successfulRequests, 
                       failedRequests);
            
            logger.info("Audit event - daily summary: system statistics total={},success={},failed={}", 
                       yesterdayRequests.size(), successfulRequests, failedRequests);

        } catch (Exception e) {
            logger.error("Error generating daily summary", e);
            monitoringService.recordError("daily_summary", e);
        }
    }
}