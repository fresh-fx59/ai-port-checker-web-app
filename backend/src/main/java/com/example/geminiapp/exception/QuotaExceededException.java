package com.example.geminiapp.exception;

public class QuotaExceededException extends RuntimeException {
    
    private final int remainingRequests;
    private final boolean isAnonymous;
    
    public QuotaExceededException(String message, int remainingRequests, boolean isAnonymous) {
        super(message);
        this.remainingRequests = remainingRequests;
        this.isAnonymous = isAnonymous;
    }
    
    public QuotaExceededException(String message, Throwable cause, int remainingRequests, boolean isAnonymous) {
        super(message, cause);
        this.remainingRequests = remainingRequests;
        this.isAnonymous = isAnonymous;
    }
    
    public int getRemainingRequests() {
        return remainingRequests;
    }
    
    public boolean isAnonymous() {
        return isAnonymous;
    }
}