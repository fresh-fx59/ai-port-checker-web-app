package com.example.geminiapp.dto;

public class PromptResponse {
    
    private boolean success;
    private String report;
    private String error;
    private int remainingRequests;
    private String requestId;
    
    // Default constructor
    public PromptResponse() {}
    
    // Constructor for success response
    public PromptResponse(boolean success, String report, int remainingRequests, String requestId) {
        this.success = success;
        this.report = report;
        this.remainingRequests = remainingRequests;
        this.requestId = requestId;
    }
    
    // Constructor for error response
    public PromptResponse(boolean success, String error) {
        this.success = success;
        this.error = error;
    }
    
    // Static factory methods
    public static PromptResponse success(String report, int remainingRequests, String requestId) {
        return new PromptResponse(true, report, remainingRequests, requestId);
    }
    
    public static PromptResponse error(String error) {
        return new PromptResponse(false, error);
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getReport() {
        return report;
    }
    
    public void setReport(String report) {
        this.report = report;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public int getRemainingRequests() {
        return remainingRequests;
    }
    
    public void setRemainingRequests(int remainingRequests) {
        this.remainingRequests = remainingRequests;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}