package com.example.geminiapp.service;

import com.example.geminiapp.entity.RequestLog;
import com.example.geminiapp.entity.RequestStatus;
import com.example.geminiapp.entity.User;
import com.example.geminiapp.repository.RequestLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class RequestLoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingService.class);
    
    private final RequestLogRepository requestLogRepository;
    
    @Autowired
    public RequestLoggingService(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }
    
    /**
     * Creates a new request log entry with PENDING status
     * 
     * @param user The user making the request
     * @param userPrompt The original user prompt
     * @param systemPrompt The combined system + user prompt
     * @param ipAddress The client IP address
     * @param userAgent The client user agent
     * @return The created RequestLog entity
     */
    public RequestLog createPendingRequest(User user, String userPrompt, String systemPrompt, 
                                         String ipAddress, String userAgent) {
        logger.debug("Creating pending request log for user: {} (anonymous: {})", 
            user.getId(), user.getIsAnonymous());
        
        RequestLog requestLog = new RequestLog(user, userPrompt, systemPrompt, ipAddress, userAgent);
        RequestLog savedLog = requestLogRepository.save(requestLog);
        
        logger.info("Created request log with ID: {} for user: {}", savedLog.getId(), user.getId());
        return savedLog;
    }
    
    /**
     * Updates a request log with successful AI response
     * 
     * @param requestLog The request log to update
     * @param aiResponse The AI-generated response
     */
    public void updateRequestSuccess(RequestLog requestLog, String aiResponse) {
        logger.debug("Updating request log {} with successful response", requestLog.getId());
        
        requestLog.markAsSuccess(aiResponse);
        requestLogRepository.save(requestLog);
        
        logger.info("Request log {} marked as successful", requestLog.getId());
    }
    
    /**
     * Updates a request log with error information
     * 
     * @param requestLog The request log to update
     * @param errorMessage The error message
     */
    public void updateRequestError(RequestLog requestLog, String errorMessage) {
        logger.debug("Updating request log {} with error: {}", requestLog.getId(), errorMessage);
        
        requestLog.markAsError(errorMessage);
        requestLogRepository.save(requestLog);
        
        logger.warn("Request log {} marked as error: {}", requestLog.getId(), errorMessage);
    }
    
    /**
     * Gets the count of successful requests for a user
     * 
     * @param user The user to count requests for
     * @return The number of successful requests
     */
    @Transactional(readOnly = true)
    public int getSuccessfulRequestCount(User user) {
        List<RequestLog> successfulRequests = requestLogRepository.findSuccessfulRequestsByUser(user);
        int count = successfulRequests.size();
        
        logger.debug("User {} has {} successful requests", user.getId(), count);
        return count;
    }
    
    /**
     * Gets the total request count for a user (all statuses)
     * 
     * @param user The user to count requests for
     * @return The total number of requests
     */
    @Transactional(readOnly = true)
    public long getTotalRequestCount(User user) {
        long count = requestLogRepository.countByUser(user);
        logger.debug("User {} has {} total requests", user.getId(), count);
        return count;
    }
    
    /**
     * Gets request history for a user with pagination
     * 
     * @param user The user to get history for
     * @param pageable Pagination parameters
     * @return Page of request logs
     */
    @Transactional(readOnly = true)
    public Page<RequestLog> getRequestHistory(User user, Pageable pageable) {
        logger.debug("Getting request history for user: {} with pagination", user.getId());
        return requestLogRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
    
    /**
     * Gets all request history for a user
     * 
     * @param user The user to get history for
     * @return List of all request logs for the user
     */
    @Transactional(readOnly = true)
    public List<RequestLog> getAllRequestHistory(User user) {
        logger.debug("Getting all request history for user: {}", user.getId());
        return requestLogRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    /**
     * Gets request history for a user by user ID with pagination
     * 
     * @param userId The user ID to get history for
     * @param page The page number (0-based)
     * @param size The page size
     * @return List of request logs for the user
     */
    @Transactional(readOnly = true)
    public List<RequestLog> getUserRequestHistory(UUID userId, int page, int size) {
        logger.debug("Getting request history for user ID: {} with page: {}, size: {}", userId, page, size);
        
        // For simplicity, we'll get all requests and manually paginate
        // In a production system, you'd want to use proper pagination with Pageable
        List<RequestLog> allRequests = requestLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        int start = page * size;
        int end = Math.min(start + size, allRequests.size());
        
        if (start >= allRequests.size()) {
            return List.of(); // Empty list if page is beyond available data
        }
        
        return allRequests.subList(start, end);
    }
    
    /**
     * Gets successful requests for a user
     * 
     * @param user The user to get successful requests for
     * @return List of successful request logs
     */
    @Transactional(readOnly = true)
    public List<RequestLog> getSuccessfulRequests(User user) {
        logger.debug("Getting successful requests for user: {}", user.getId());
        return requestLogRepository.findSuccessfulRequestsByUser(user);
    }
    
    /**
     * Gets failed requests for a user
     * 
     * @param user The user to get failed requests for
     * @return List of failed request logs
     */
    @Transactional(readOnly = true)
    public List<RequestLog> getFailedRequests(User user) {
        logger.debug("Getting failed requests for user: {}", user.getId());
        return requestLogRepository.findFailedRequestsByUser(user);
    }
    
    /**
     * Gets a specific request log by ID
     * 
     * @param requestId The request log ID
     * @return Optional containing the request log if found
     */
    @Transactional(readOnly = true)
    public Optional<RequestLog> getRequestById(UUID requestId) {
        logger.debug("Getting request log by ID: {}", requestId);
        return requestLogRepository.findById(requestId);
    }
    
    /**
     * Gets the most recent request for a user
     * 
     * @param user The user to get the most recent request for
     * @return The most recent request log, or null if none exists
     */
    @Transactional(readOnly = true)
    public RequestLog getMostRecentRequest(User user) {
        logger.debug("Getting most recent request for user: {}", user.getId());
        return requestLogRepository.findMostRecentRequestByUser(user);
    }
    
    /**
     * Gets request count by IP address within a time window (for rate limiting)
     * 
     * @param ipAddress The IP address to check
     * @param since The time window start
     * @return The number of requests from this IP since the given time
     */
    @Transactional(readOnly = true)
    public long getRequestCountByIpSince(String ipAddress, LocalDateTime since) {
        long count = requestLogRepository.countRequestsByIpAddressSince(ipAddress, since);
        logger.debug("IP {} has {} requests since {}", ipAddress, count, since);
        return count;
    }
    
    /**
     * Gets request count by user within a time window
     * 
     * @param user The user to check
     * @param since The time window start
     * @return The number of requests from this user since the given time
     */
    @Transactional(readOnly = true)
    public long getRequestCountByUserSince(User user, LocalDateTime since) {
        long count = requestLogRepository.countRequestsByUserSince(user, since);
        logger.debug("User {} has {} requests since {}", user.getId(), count, since);
        return count;
    }
    
    /**
     * Gets requests within a date range
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @return List of request logs within the date range
     */
    @Transactional(readOnly = true)
    public List<RequestLog> getRequestsBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Getting requests between {} and {}", startDate, endDate);
        return requestLogRepository.findRequestsBetweenDates(startDate, endDate);
    }
    
    /**
     * Gets requests for a user within a date range
     * 
     * @param user The user to get requests for
     * @param startDate The start date
     * @param endDate The end date
     * @return List of request logs for the user within the date range
     */
    @Transactional(readOnly = true)
    public List<RequestLog> getRequestsByUserBetweenDates(User user, LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Getting requests for user {} between {} and {}", user.getId(), startDate, endDate);
        return requestLogRepository.findRequestsByUserBetweenDates(user, startDate, endDate);
    }
    
    /**
     * Gets daily request statistics for the last N days
     * 
     * @param days The number of days to look back
     * @return List of daily statistics
     */
    @Transactional(readOnly = true)
    public List<Object[]> getDailyStatistics(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        logger.debug("Getting daily statistics for the last {} days", days);
        return requestLogRepository.getDailyRequestStatistics(since);
    }
    
    /**
     * Finds stuck pending requests (requests that have been pending too long)
     * 
     * @param timeoutMinutes The timeout in minutes
     * @return List of stuck pending requests
     */
    @Transactional(readOnly = true)
    public List<RequestLog> findStuckPendingRequests(int timeoutMinutes) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(timeoutMinutes);
        logger.debug("Finding stuck pending requests older than {} minutes", timeoutMinutes);
        return requestLogRepository.findStuckPendingRequests(cutoffTime);
    }
    
    /**
     * Cleans up stuck pending requests by marking them as errors
     * 
     * @param timeoutMinutes The timeout in minutes
     * @return The number of requests cleaned up
     */
    public int cleanupStuckPendingRequests(int timeoutMinutes) {
        List<RequestLog> stuckRequests = findStuckPendingRequests(timeoutMinutes);
        
        for (RequestLog request : stuckRequests) {
            updateRequestError(request, "Request timed out - no response received within " + timeoutMinutes + " minutes");
        }
        
        logger.info("Cleaned up {} stuck pending requests", stuckRequests.size());
        return stuckRequests.size();
    }
    
    /**
     * Gets system-wide statistics
     * 
     * @return RequestStatistics object with system metrics
     */
    @Transactional(readOnly = true)
    public RequestStatistics getSystemStatistics() {
        long totalRequests = requestLogRepository.count();
        long successfulRequests = requestLogRepository.countSuccessfulRequests();
        long failedRequests = requestLogRepository.countFailedRequests();
        long pendingRequests = requestLogRepository.countByStatus(RequestStatus.PENDING);
        
        logger.debug("System statistics - Total: {}, Success: {}, Failed: {}, Pending: {}", 
            totalRequests, successfulRequests, failedRequests, pendingRequests);
        
        return new RequestStatistics(totalRequests, successfulRequests, failedRequests, pendingRequests);
    }
    
    /**
     * Data class for system statistics
     */
    public static class RequestStatistics {
        private final long totalRequests;
        private final long successfulRequests;
        private final long failedRequests;
        private final long pendingRequests;
        
        public RequestStatistics(long totalRequests, long successfulRequests, long failedRequests, long pendingRequests) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.failedRequests = failedRequests;
            this.pendingRequests = pendingRequests;
        }
        
        public long getTotalRequests() { return totalRequests; }
        public long getSuccessfulRequests() { return successfulRequests; }
        public long getFailedRequests() { return failedRequests; }
        public long getPendingRequests() { return pendingRequests; }
        public double getSuccessRate() { 
            return totalRequests > 0 ? (double) successfulRequests / totalRequests * 100 : 0.0; 
        }
        public double getFailureRate() { 
            return totalRequests > 0 ? (double) failedRequests / totalRequests * 100 : 0.0; 
        }
    }
}