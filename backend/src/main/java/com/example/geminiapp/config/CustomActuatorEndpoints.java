package com.example.geminiapp.config;

import com.example.geminiapp.repository.UserRepository;
import com.example.geminiapp.repository.RequestLogRepository;
import com.example.geminiapp.entity.RequestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Actuator endpoints for application-specific monitoring data.
 */
@Component
@Endpoint(id = "gemini-stats")
public class CustomActuatorEndpoints {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequestLogRepository requestLogRepository;

    /**
     * Provides comprehensive application statistics
     */
    @ReadOperation
    public Map<String, Object> getApplicationStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // User statistics
            Map<String, Object> userStats = new HashMap<>();
            userStats.put("total", userRepository.count());
            userStats.put("anonymous", userRepository.countByIsAnonymousTrue());
            userStats.put("registered", userRepository.count() - userRepository.countByIsAnonymousTrue());
            stats.put("users", userStats);

            // Request statistics
            Map<String, Object> requestStats = new HashMap<>();
            requestStats.put("total", requestLogRepository.count());
            requestStats.put("successful", requestLogRepository.countByStatus(RequestStatus.SUCCESS));
            requestStats.put("failed", requestLogRepository.countByStatus(RequestStatus.ERROR));
            requestStats.put("pending", requestLogRepository.countByStatus(RequestStatus.PENDING));
            requestStats.put("today", requestLogRepository.countTodayRequests());
            stats.put("requests", requestStats);

            // System information
            Map<String, Object> systemStats = new HashMap<>();
            Runtime runtime = Runtime.getRuntime();
            systemStats.put("memory_total_mb", runtime.totalMemory() / 1024 / 1024);
            systemStats.put("memory_free_mb", runtime.freeMemory() / 1024 / 1024);
            systemStats.put("memory_used_mb", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
            systemStats.put("processors", runtime.availableProcessors());
            stats.put("system", systemStats);

            // Application status
            Map<String, Object> appStatus = new HashMap<>();
            appStatus.put("status", "healthy");
            appStatus.put("timestamp", LocalDateTime.now().toString());
            appStatus.put("uptime_ms", System.currentTimeMillis() - getStartTime());
            stats.put("application", appStatus);

        } catch (Exception e) {
            Map<String, Object> errorStats = new HashMap<>();
            errorStats.put("status", "error");
            errorStats.put("message", "Failed to collect statistics: " + e.getMessage());
            errorStats.put("timestamp", LocalDateTime.now().toString());
            stats.put("error", errorStats);
        }

        return stats;
    }

    private long getStartTime() {
        // This is a simplified approach - in a real application, you might want to
        // store the actual start time in a more sophisticated way
        return System.currentTimeMillis() - 
               java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
    }
}

/**
 * Custom endpoint for quota information
 */
@Component
@Endpoint(id = "quota-info")
class QuotaInfoEndpoint {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @ReadOperation
    public Map<String, Object> getQuotaInfo() {
        Map<String, Object> quotaInfo = new HashMap<>();
        
        try {
            // Anonymous user quota usage
            long anonymousUsers = userRepository.countByIsAnonymousTrue();
            Map<String, Object> anonymousQuota = new HashMap<>();
            anonymousQuota.put("total_users", anonymousUsers);
            anonymousQuota.put("quota_per_user", 1);
            anonymousQuota.put("total_quota_available", anonymousUsers * 1);
            quotaInfo.put("anonymous", anonymousQuota);

            // Registered user quota usage
            long registeredUsers = userRepository.count() - anonymousUsers;
            Map<String, Object> registeredQuota = new HashMap<>();
            registeredQuota.put("total_users", registeredUsers);
            registeredQuota.put("quota_per_user", 5);
            registeredQuota.put("total_quota_available", registeredUsers * 5);
            quotaInfo.put("registered", registeredQuota);

            // Overall quota usage
            Map<String, Object> overall = new HashMap<>();
            overall.put("total_requests_made", requestLogRepository.count());
            overall.put("total_quota_available", (anonymousUsers * 1) + (registeredUsers * 5));
            overall.put("quota_utilization_percent", 
                       calculateQuotaUtilization(requestLogRepository.count(), 
                                                (anonymousUsers * 1) + (registeredUsers * 5)));
            quotaInfo.put("overall", overall);

            quotaInfo.put("timestamp", LocalDateTime.now().toString());

        } catch (Exception e) {
            quotaInfo.put("error", "Failed to collect quota information: " + e.getMessage());
            quotaInfo.put("timestamp", LocalDateTime.now().toString());
        }

        return quotaInfo;
    }

    private double calculateQuotaUtilization(long requestsMade, long totalQuota) {
        if (totalQuota == 0) return 0.0;
        return Math.min(100.0, (double) requestsMade / totalQuota * 100.0);
    }
}

/**
 * Custom endpoint for system health details
 */
@Component
@Endpoint(id = "system-health")
class SystemHealthEndpoint {

    @ReadOperation
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // JVM Health
            Map<String, Object> jvmHealth = new HashMap<>();
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            jvmHealth.put("memory_usage_percent", (double) usedMemory / totalMemory * 100);
            jvmHealth.put("memory_total_mb", totalMemory / 1024 / 1024);
            jvmHealth.put("memory_used_mb", usedMemory / 1024 / 1024);
            jvmHealth.put("memory_free_mb", freeMemory / 1024 / 1024);
            jvmHealth.put("processors", runtime.availableProcessors());
            
            // Memory health status
            double memoryUsagePercent = (double) usedMemory / totalMemory * 100;
            if (memoryUsagePercent > 90) {
                jvmHealth.put("status", "critical");
            } else if (memoryUsagePercent > 75) {
                jvmHealth.put("status", "warning");
            } else {
                jvmHealth.put("status", "healthy");
            }
            
            health.put("jvm", jvmHealth);

            // Thread information
            Map<String, Object> threadInfo = new HashMap<>();
            threadInfo.put("active_threads", Thread.activeCount());
            threadInfo.put("daemon_threads", Thread.getAllStackTraces().keySet().stream()
                          .mapToInt(t -> t.isDaemon() ? 1 : 0).sum());
            health.put("threads", threadInfo);

            // Overall system status
            String overallStatus = "healthy";
            if (memoryUsagePercent > 90) {
                overallStatus = "critical";
            } else if (memoryUsagePercent > 75) {
                overallStatus = "warning";
            }
            
            health.put("overall_status", overallStatus);
            health.put("timestamp", LocalDateTime.now().toString());

        } catch (Exception e) {
            health.put("status", "error");
            health.put("error", "Failed to collect system health: " + e.getMessage());
            health.put("timestamp", LocalDateTime.now().toString());
        }

        return health;
    }
}