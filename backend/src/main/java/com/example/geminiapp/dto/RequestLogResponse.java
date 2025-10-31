package com.example.geminiapp.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class RequestLogResponse {
    
    private UUID id;
    private String userPrompt;
    private String systemPrompt;
    private String aiResponse;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    
    // Default constructor
    public RequestLogResponse() {}
    
    // Constructor
    public RequestLogResponse(UUID id, String userPrompt, String systemPrompt, 
                            String aiResponse, String status, String errorMessage, 
                            LocalDateTime createdAt) {
        this.id = id;
        this.userPrompt = userPrompt;
        this.systemPrompt = systemPrompt;
        this.aiResponse = aiResponse;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}