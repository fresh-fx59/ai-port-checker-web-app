package com.example.geminiapp.dto;

public class UserQuotaResponse {
    
    private int remainingRequests;
    private int totalRequests;
    private boolean isAnonymous;
    
    // Default constructor
    public UserQuotaResponse() {}
    
    // Constructor
    public UserQuotaResponse(int remainingRequests, int totalRequests, boolean isAnonymous) {
        this.remainingRequests = remainingRequests;
        this.totalRequests = totalRequests;
        this.isAnonymous = isAnonymous;
    }
    
    // Getters and Setters
    public int getRemainingRequests() {
        return remainingRequests;
    }
    
    public void setRemainingRequests(int remainingRequests) {
        this.remainingRequests = remainingRequests;
    }
    
    public int getTotalRequests() {
        return totalRequests;
    }
    
    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }
    
    public boolean isAnonymous() {
        return isAnonymous;
    }
    
    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }
}