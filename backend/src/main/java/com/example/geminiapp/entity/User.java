package com.example.geminiapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * User entity representing both anonymous and registered users in the system.
 * Anonymous users are tracked by fingerprint, while registered users have email/OAuth credentials.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_google_id", columnList = "google_id"),
    @Index(name = "idx_user_fingerprint", columnList = "fingerprint")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Column(unique = true, length = 255)
    private String email;

    @Size(max = 255, message = "Google ID must not exceed 255 characters")
    @Column(name = "google_id", unique = true, length = 255)
    private String googleId;

    @Size(max = 255, message = "Password must not exceed 255 characters")
    @Column(length = 255)
    private String password;

    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous = true;

    @Size(max = 255, message = "Fingerprint must not exceed 255 characters")
    @Column(length = 255)
    private String fingerprint;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RequestLog> requestLogs = new ArrayList<>();

    // Default constructor
    public User() {}

    // Constructor for anonymous user
    public User(String fingerprint) {
        this.fingerprint = fingerprint;
        this.isAnonymous = true;
    }

    // Constructor for registered user with email
    public User(String email, String fingerprint) {
        this.email = email;
        this.fingerprint = fingerprint;
        this.isAnonymous = false;
    }

    // Constructor for registered user with email and password
    public User(String email, String password, String fingerprint) {
        this.email = email;
        this.password = password;
        this.fingerprint = fingerprint;
        this.isAnonymous = false;
    }

    // Constructor for Google OAuth user
    public static User createOAuthUser(String email, String googleId, String fingerprint) {
        User user = new User();
        user.email = email;
        user.googleId = googleId;
        user.fingerprint = fingerprint;
        user.isAnonymous = false;
        return user;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getIsAnonymous() {
        return isAnonymous;
    }

    public void setIsAnonymous(Boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<RequestLog> getRequestLogs() {
        return requestLogs;
    }

    public void setRequestLogs(List<RequestLog> requestLogs) {
        this.requestLogs = requestLogs;
    }

    // Helper methods
    public void addRequestLog(RequestLog requestLog) {
        requestLogs.add(requestLog);
        requestLog.setUser(this);
    }

    public void removeRequestLog(RequestLog requestLog) {
        requestLogs.remove(requestLog);
        requestLog.setUser(null);
    }

    /**
     * Get the count of requests made by this user
     */
    public long getRequestCount() {
        return requestLogs.size();
    }

    /**
     * Check if user has reached their quota limit
     * Anonymous users: 1 request, Registered users: 5 requests (1 + 4 additional)
     */
    public boolean hasReachedQuota() {
        long maxRequests = isAnonymous ? 1L : 5L;
        return getRequestCount() >= maxRequests;
    }

    /**
     * Get remaining requests for this user
     */
    public long getRemainingRequests() {
        long maxRequests = isAnonymous ? 1L : 5L;
        return Math.max(0, maxRequests - getRequestCount());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id != null && id.equals(user.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", isAnonymous=" + isAnonymous +
                ", createdAt=" + createdAt +
                '}';
    }
}