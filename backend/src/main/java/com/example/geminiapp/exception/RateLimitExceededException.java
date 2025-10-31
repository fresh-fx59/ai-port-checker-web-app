package com.example.geminiapp.exception;

public class RateLimitExceededException extends RuntimeException {
    
    private final String identifier;
    private final int requestsInLastMinute;
    private final int requestsInLastHour;
    private final int requestsInLastDay;
    private final int limitPerMinute;
    private final int limitPerHour;
    private final int limitPerDay;
    
    public RateLimitExceededException(String message, String identifier, 
                                   int requestsInLastMinute, int requestsInLastHour, int requestsInLastDay,
                                   int limitPerMinute, int limitPerHour, int limitPerDay) {
        super(message);
        this.identifier = identifier;
        this.requestsInLastMinute = requestsInLastMinute;
        this.requestsInLastHour = requestsInLastHour;
        this.requestsInLastDay = requestsInLastDay;
        this.limitPerMinute = limitPerMinute;
        this.limitPerHour = limitPerHour;
        this.limitPerDay = limitPerDay;
    }
    
    // Getters
    public String getIdentifier() { return identifier; }
    public int getRequestsInLastMinute() { return requestsInLastMinute; }
    public int getRequestsInLastHour() { return requestsInLastHour; }
    public int getRequestsInLastDay() { return requestsInLastDay; }
    public int getLimitPerMinute() { return limitPerMinute; }
    public int getLimitPerHour() { return limitPerHour; }
    public int getLimitPerDay() { return limitPerDay; }
}