package com.example.geminiapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RequestLog entity for storing all user requests and AI responses.
 * Maintains audit trail and enables request history functionality.
 */
@Entity
@Table(name = "request_logs", indexes = {
    @Index(name = "idx_request_log_user_id", columnList = "user_id"),
    @Index(name = "idx_request_log_status", columnList = "status"),
    @Index(name = "idx_request_log_created_at", columnList = "created_at"),
    @Index(name = "idx_request_log_ip_address", columnList = "ip_address")
})
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @NotBlank(message = "User prompt cannot be blank")
    @Column(name = "user_prompt", columnDefinition = "TEXT", nullable = false)
    private String userPrompt;

    @NotBlank(message = "System prompt cannot be blank")
    @Column(name = "system_prompt", columnDefinition = "TEXT", nullable = false)
    private String systemPrompt;

    @Column(name = "ai_response", columnDefinition = "TEXT")
    private String aiResponse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @NotNull(message = "Status is required")
    private RequestStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Size(max = 45, message = "IP address must not exceed 45 characters")
    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Default constructor
    public RequestLog() {}

    // Constructor for new request
    public RequestLog(User user, String userPrompt, String systemPrompt, String ipAddress, String userAgent) {
        this.user = user;
        this.userPrompt = userPrompt;
        this.systemPrompt = systemPrompt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.status = RequestStatus.PENDING;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Helper methods
    public void markAsSuccess(String aiResponse) {
        this.status = RequestStatus.SUCCESS;
        this.aiResponse = aiResponse;
        this.errorMessage = null;
    }

    public void markAsError(String errorMessage) {
        this.status = RequestStatus.ERROR;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccessful() {
        return RequestStatus.SUCCESS.equals(this.status);
    }

    public boolean isPending() {
        return RequestStatus.PENDING.equals(this.status);
    }

    public boolean hasError() {
        return RequestStatus.ERROR.equals(this.status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RequestLog)) return false;
        RequestLog that = (RequestLog) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "RequestLog{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}

