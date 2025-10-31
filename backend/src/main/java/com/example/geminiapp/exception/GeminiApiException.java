package com.example.geminiapp.exception;

public class GeminiApiException extends RuntimeException {
    
    private final int statusCode;
    private final String errorCode;
    
    public GeminiApiException(String message) {
        super(message);
        this.statusCode = 500;
        this.errorCode = "GEMINI_API_ERROR";
    }
    
    public GeminiApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = "GEMINI_API_ERROR";
    }
    
    public GeminiApiException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
    
    public GeminiApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
        this.errorCode = "GEMINI_API_ERROR";
    }
    
    public GeminiApiException(String message, Throwable cause, int statusCode, String errorCode) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}