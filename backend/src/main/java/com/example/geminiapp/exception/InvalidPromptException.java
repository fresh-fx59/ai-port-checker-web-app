package com.example.geminiapp.exception;

public class InvalidPromptException extends RuntimeException {
    
    private final String validationError;
    
    public InvalidPromptException(String message) {
        super(message);
        this.validationError = message;
    }
    
    public InvalidPromptException(String message, String validationError) {
        super(message);
        this.validationError = validationError;
    }
    
    public InvalidPromptException(String message, Throwable cause) {
        super(message, cause);
        this.validationError = message;
    }
    
    public String getValidationError() {
        return validationError;
    }
}