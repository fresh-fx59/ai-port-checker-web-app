package com.example.geminiapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GeminiResponse {
    
    @JsonProperty("candidates")
    private List<Candidate> candidates;
    
    @JsonProperty("promptFeedback")
    private PromptFeedback promptFeedback;
    
    public GeminiResponse() {}
    
    // Getters and setters
    public List<Candidate> getCandidates() {
        return candidates;
    }
    
    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }
    
    public PromptFeedback getPromptFeedback() {
        return promptFeedback;
    }
    
    public void setPromptFeedback(PromptFeedback promptFeedback) {
        this.promptFeedback = promptFeedback;
    }
    
    public String getGeneratedText() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate candidate = candidates.get(0);
            if (candidate.getContent() != null && 
                candidate.getContent().getParts() != null && 
                !candidate.getContent().getParts().isEmpty()) {
                return candidate.getContent().getParts().get(0).getText();
            }
        }
        return null;
    }
    
    public static class Candidate {
        @JsonProperty("content")
        private Content content;
        
        @JsonProperty("finishReason")
        private String finishReason;
        
        @JsonProperty("index")
        private Integer index;
        
        @JsonProperty("safetyRatings")
        private List<SafetyRating> safetyRatings;
        
        public Candidate() {}
        
        // Getters and setters
        public Content getContent() {
            return content;
        }
        
        public void setContent(Content content) {
            this.content = content;
        }
        
        public String getFinishReason() {
            return finishReason;
        }
        
        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
        
        public Integer getIndex() {
            return index;
        }
        
        public void setIndex(Integer index) {
            this.index = index;
        }
        
        public List<SafetyRating> getSafetyRatings() {
            return safetyRatings;
        }
        
        public void setSafetyRatings(List<SafetyRating> safetyRatings) {
            this.safetyRatings = safetyRatings;
        }
    }
    
    public static class Content {
        @JsonProperty("parts")
        private List<Part> parts;
        
        @JsonProperty("role")
        private String role;
        
        public Content() {}
        
        // Getters and setters
        public List<Part> getParts() {
            return parts;
        }
        
        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
    }
    
    public static class Part {
        @JsonProperty("text")
        private String text;
        
        public Part() {}
        
        // Getters and setters
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
    }
    
    public static class PromptFeedback {
        @JsonProperty("safetyRatings")
        private List<SafetyRating> safetyRatings;
        
        public PromptFeedback() {}
        
        // Getters and setters
        public List<SafetyRating> getSafetyRatings() {
            return safetyRatings;
        }
        
        public void setSafetyRatings(List<SafetyRating> safetyRatings) {
            this.safetyRatings = safetyRatings;
        }
    }
    
    public static class SafetyRating {
        @JsonProperty("category")
        private String category;
        
        @JsonProperty("probability")
        private String probability;
        
        public SafetyRating() {}
        
        // Getters and setters
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
        
        public String getProbability() {
            return probability;
        }
        
        public void setProbability(String probability) {
            this.probability = probability;
        }
    }
}