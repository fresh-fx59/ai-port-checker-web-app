package com.example.geminiapp.dto;

import com.example.geminiapp.validation.ValidPrompt;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PromptRequest {
    
    @NotBlank(message = "Prompt is required")
    @Size(min = 10, max = 10000, message = "Prompt must be between 10 and 10000 characters")
    @ValidPrompt
    private String prompt;
    
    private String fingerprint; // For anonymous user tracking
    
    // Default constructor
    public PromptRequest() {}
    
    // Constructor
    public PromptRequest(String prompt, String fingerprint) {
        this.prompt = prompt;
        this.fingerprint = fingerprint;
    }
    
    // Getters and Setters
    public String getPrompt() {
        return prompt;
    }
    
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    
    public String getFingerprint() {
        return fingerprint;
    }
    
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
}