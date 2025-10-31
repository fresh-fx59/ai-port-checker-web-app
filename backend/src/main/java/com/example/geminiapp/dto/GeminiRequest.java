package com.example.geminiapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GeminiRequest {
    
    @JsonProperty("contents")
    private List<Content> contents;
    
    @JsonProperty("generationConfig")
    private GenerationConfig generationConfig;
    
    @JsonProperty("safetySettings")
    private List<SafetySetting> safetySettings;
    
    public GeminiRequest() {}
    
    public GeminiRequest(List<Content> contents) {
        this.contents = contents;
        this.generationConfig = new GenerationConfig();
        this.safetySettings = List.of(); // Default empty safety settings
    }
    
    // Getters and setters
    public List<Content> getContents() {
        return contents;
    }
    
    public void setContents(List<Content> contents) {
        this.contents = contents;
    }
    
    public GenerationConfig getGenerationConfig() {
        return generationConfig;
    }
    
    public void setGenerationConfig(GenerationConfig generationConfig) {
        this.generationConfig = generationConfig;
    }
    
    public List<SafetySetting> getSafetySettings() {
        return safetySettings;
    }
    
    public void setSafetySettings(List<SafetySetting> safetySettings) {
        this.safetySettings = safetySettings;
    }
    
    public static class Content {
        @JsonProperty("parts")
        private List<Part> parts;
        
        public Content() {}
        
        public Content(String text) {
            this.parts = List.of(new Part(text));
        }
        
        public List<Part> getParts() {
            return parts;
        }
        
        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }
    
    public static class Part {
        @JsonProperty("text")
        private String text;
        
        public Part() {}
        
        public Part(String text) {
            this.text = text;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
    }
    
    public static class GenerationConfig {
        @JsonProperty("temperature")
        private Double temperature = 0.7;
        
        @JsonProperty("topK")
        private Integer topK = 40;
        
        @JsonProperty("topP")
        private Double topP = 0.95;
        
        @JsonProperty("maxOutputTokens")
        private Integer maxOutputTokens = 2048;
        
        // Getters and setters
        public Double getTemperature() {
            return temperature;
        }
        
        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }
        
        public Integer getTopK() {
            return topK;
        }
        
        public void setTopK(Integer topK) {
            this.topK = topK;
        }
        
        public Double getTopP() {
            return topP;
        }
        
        public void setTopP(Double topP) {
            this.topP = topP;
        }
        
        public Integer getMaxOutputTokens() {
            return maxOutputTokens;
        }
        
        public void setMaxOutputTokens(Integer maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }
    }
    
    public static class SafetySetting {
        @JsonProperty("category")
        private String category;
        
        @JsonProperty("threshold")
        private String threshold;
        
        public SafetySetting() {}
        
        public SafetySetting(String category, String threshold) {
            this.category = category;
            this.threshold = threshold;
        }
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
        
        public String getThreshold() {
            return threshold;
        }
        
        public void setThreshold(String threshold) {
            this.threshold = threshold;
        }
    }
}