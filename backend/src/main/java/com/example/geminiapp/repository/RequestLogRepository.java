package com.example.geminiapp.repository;

import com.example.geminiapp.entity.RequestLog;
import com.example.geminiapp.entity.RequestStatus;
import com.example.geminiapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for RequestLog entity operations.
 * Provides methods for request history, filtering, and analytics.
 */
@Repository
public interface RequestLogRepository extends JpaRepository<RequestLog, UUID> {

    /**
     * Find all request logs for a specific user, ordered by creation date (newest first)
     */
    List<RequestLog> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find all request logs for a specific user with pagination
     */
    Page<RequestLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Find request logs by user ID
     */
    List<RequestLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find request logs by user ID with pagination
     */
    Page<RequestLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find request logs by status
     */
    List<RequestLog> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    /**
     * Find request logs by status with pagination
     */
    Page<RequestLog> findByStatusOrderByCreatedAtDesc(RequestStatus status, Pageable pageable);

    /**
     * Count total requests by user
     */
    long countByUser(User user);

    /**
     * Count total requests by user ID
     */
    long countByUserId(UUID userId);

    /**
     * Count requests by status
     */
    long countByStatus(RequestStatus status);

    /**
     * Find successful requests for a user
     */
    @Query("SELECT rl FROM RequestLog rl WHERE rl.user = :user AND rl.status = 'SUCCESS' ORDER BY rl.createdAt DESC")
    List<RequestLog> findSuccessfulRequestsByUser(@Param("user") User user);

    /**
     * Find failed requests for a user
     */
    @Query("SELECT rl FROM RequestLog rl WHERE rl.user = :user AND rl.status = 'ERROR' ORDER BY rl.createdAt DESC")
    List<RequestLog> findFailedRequestsByUser(@Param("user") User user);

    /**
     * Find requests within a date range
     */
    @Query("SELECT rl FROM RequestLog rl WHERE rl.createdAt BETWEEN :startDate AND :endDate ORDER BY rl.createdAt DESC")
    List<RequestLog> findRequestsBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Find requests by user within a date range
     */
    @Query("SELECT rl FROM RequestLog rl WHERE rl.user = :user AND rl.createdAt BETWEEN :startDate AND :endDate ORDER BY rl.createdAt DESC")
    List<RequestLog> findRequestsByUserBetweenDates(@Param("user") User user,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Find requests by IP address (for tracking and security)
     */
    List<RequestLog> findByIpAddressOrderByCreatedAtDesc(String ipAddress);

    /**
     * Count requests by IP address within a time window (for rate limiting)
     */
    @Query("SELECT COUNT(rl) FROM RequestLog rl WHERE rl.ipAddress = :ipAddress AND rl.createdAt >= :since")
    long countRequestsByIpAddressSince(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);

    /**
     * Count requests by user within a time window
     */
    @Query("SELECT COUNT(rl) FROM RequestLog rl WHERE rl.user = :user AND rl.createdAt >= :since")
    long countRequestsByUserSince(@Param("user") User user, @Param("since") LocalDateTime since);

    /**
     * Find most recent request by user
     */
    @Query("SELECT rl FROM RequestLog rl WHERE rl.user = :user ORDER BY rl.createdAt DESC LIMIT 1")
    RequestLog findMostRecentRequestByUser(@Param("user") User user);

    /**
     * Find requests with errors containing specific text
     */
    @Query("SELECT rl FROM RequestLog rl WHERE rl.status = 'ERROR' AND rl.errorMessage LIKE %:errorText% ORDER BY rl.createdAt DESC")
    List<RequestLog> findRequestsWithErrorContaining(@Param("errorText") String errorText);

    /**
     * Get daily request statistics - H2 compatible version
     */
    @Query("SELECT CAST(rl.createdAt AS date) as date, COUNT(rl) as count, " +
           "SUM(CASE WHEN rl.status = 'SUCCESS' THEN 1 ELSE 0 END) as successCount, " +
           "SUM(CASE WHEN rl.status = 'ERROR' THEN 1 ELSE 0 END) as errorCount " +
           "FROM RequestLog rl WHERE rl.createdAt >= :since " +
           "GROUP BY CAST(rl.createdAt AS date) ORDER BY CAST(rl.createdAt AS date) DESC")
    List<Object[]> getDailyRequestStatistics(@Param("since") LocalDateTime since);

    /**
     * Find requests by anonymous users
     */
    @Query("SELECT rl FROM RequestLog rl WHERE rl.user.isAnonymous = true ORDER BY rl.createdAt DESC")
    List<RequestLog> findRequestsByAnonymousUsers();

    /**
     * Find requests by registered users
     */
    @Query("SELECT rl FROM RequestLog rl WHERE rl.user.isAnonymous = false ORDER BY rl.createdAt DESC")
    List<RequestLog> findRequestsByRegisteredUsers();

    /**
     * Count total successful requests
     */
    @Query("SELECT COUNT(rl) FROM RequestLog rl WHERE rl.status = 'SUCCESS'")
    long countSuccessfulRequests();

    /**
     * Count total failed requests
     */
    @Query("SELECT COUNT(rl) FROM RequestLog rl WHERE rl.status = 'ERROR'")
    long countFailedRequests();

    /**
     * Find pending requests (for monitoring stuck requests)
     */
    @Query("SELECT rl FROM RequestLog rl WHERE rl.status = 'PENDING' AND rl.createdAt < :cutoffTime ORDER BY rl.createdAt ASC")
    List<RequestLog> findStuckPendingRequests(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Delete old request logs (for data retention)
     */
    @Query("DELETE FROM RequestLog rl WHERE rl.createdAt < :cutoffDate")
    void deleteRequestLogsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find requests with specific user agent patterns (for analytics)
     */
    @Query("SELECT rl FROM RequestLog rl WHERE rl.userAgent LIKE %:pattern% ORDER BY rl.createdAt DESC")
    List<RequestLog> findRequestsByUserAgentPattern(@Param("pattern") String pattern);

    /**
     * Count requests made today - H2 compatible version
     */
    @Query("SELECT COUNT(rl) FROM RequestLog rl WHERE CAST(rl.createdAt AS date) = CURRENT_DATE")
    long countTodayRequests();
}