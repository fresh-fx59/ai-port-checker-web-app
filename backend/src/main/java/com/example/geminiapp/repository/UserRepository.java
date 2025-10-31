package com.example.geminiapp.repository;

import com.example.geminiapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity operations.
 * Provides methods for user lookup, quota tracking, and user management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email address
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by Google OAuth ID
     */
    Optional<User> findByGoogleId(String googleId);

    /**
     * Find user by fingerprint (for anonymous users)
     */
    Optional<User> findByFingerprint(String fingerprint);

    /**
     * Find user by email or Google ID (for authentication)
     */
    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.googleId = :identifier")
    Optional<User> findByEmailOrGoogleId(@Param("identifier") String identifier);

    /**
     * Check if email already exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if Google ID already exists
     */
    boolean existsByGoogleId(String googleId);

    /**
     * Check if fingerprint already exists
     */
    boolean existsByFingerprint(String fingerprint);

    /**
     * Find all anonymous users
     */
    List<User> findByIsAnonymousTrue();

    /**
     * Find all registered users
     */
    List<User> findByIsAnonymousFalse();

    /**
     * Find users created after a specific date
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find users with request count using custom query
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.requestLogs WHERE u.id = :userId")
    Optional<User> findByIdWithRequestLogs(@Param("userId") UUID userId);

    /**
     * Count total number of anonymous users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isAnonymous = true")
    long countAnonymousUsers();

    /**
     * Count users by anonymous status (for metrics)
     */
    long countByIsAnonymousTrue();

    /**
     * Count total number of registered users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isAnonymous = false")
    long countRegisteredUsers();

    /**
     * Find users who have exceeded their quota
     * Anonymous users: > 1 request, Registered users: > 5 requests
     */
    @Query("SELECT u FROM User u WHERE " +
           "(u.isAnonymous = true AND SIZE(u.requestLogs) >= 1) OR " +
           "(u.isAnonymous = false AND SIZE(u.requestLogs) >= 5)")
    List<User> findUsersWhoExceededQuota();

    /**
     * Find users with remaining quota
     */
    @Query("SELECT u FROM User u WHERE " +
           "(u.isAnonymous = true AND SIZE(u.requestLogs) < 1) OR " +
           "(u.isAnonymous = false AND SIZE(u.requestLogs) < 5)")
    List<User> findUsersWithRemainingQuota();

    /**
     * Get user request count
     */
    @Query("SELECT COUNT(rl) FROM RequestLog rl WHERE rl.user.id = :userId")
    long getRequestCountByUserId(@Param("userId") UUID userId);

    /**
     * Find users by fingerprint and check if they can be converted to registered
     */
    @Query("SELECT u FROM User u WHERE u.fingerprint = :fingerprint AND u.isAnonymous = true")
    Optional<User> findAnonymousUserByFingerprint(@Param("fingerprint") String fingerprint);

    /**
     * Find anonymous user by fingerprint (alternative method name)
     */
    Optional<User> findByFingerprintAndIsAnonymousTrue(String fingerprint);

    /**
     * Find users created within a date range
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findUsersCreatedBetween(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Delete anonymous users older than specified date (for cleanup)
     */
    @Query("DELETE FROM User u WHERE u.isAnonymous = true AND u.createdAt < :cutoffDate")
    void deleteAnonymousUsersOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}