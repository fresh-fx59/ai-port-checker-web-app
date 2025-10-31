package com.example.geminiapp.service;

import com.example.geminiapp.config.GeminiApiConfig;
import com.example.geminiapp.dto.GeminiRequest;
import com.example.geminiapp.dto.GeminiResponse;
import com.example.geminiapp.exception.GeminiApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
public class GeminiApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiApiService.class);
    
    private final WebClient webClient;
    private final GeminiApiConfig config;
    
    @Autowired
    public GeminiApiService(WebClient geminiWebClient, GeminiApiConfig config) {
        this.webClient = geminiWebClient;
        this.config = config;
    }
    
    /**
     * Generates content using the Gemini API
     * 
     * @param prompt The combined system and user prompt
     * @return The generated text response
     * @throws GeminiApiException if the API call fails
     */
    public String generateContent(String prompt) {
        logger.info("Sending request to Gemini API with prompt length: {}", prompt.length());
        
        try {
            // Create the request payload
            GeminiRequest request = createGeminiRequest(prompt);
            
            // Make the API call with retry logic
            GeminiResponse response = webClient
                    .post()
                    .uri("/models/{model}:generateContent", config.getModel())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .timeout(config.getTimeout())
                    .retryWhen(Retry.backoff(config.getMaxRetries(), Duration.ofSeconds(1))
                            .filter(this::isRetryableException)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                logger.error("Retry exhausted after {} attempts", config.getMaxRetries());
                                return new GeminiApiException(
                                    "Failed to get response from Gemini API after " + config.getMaxRetries() + " retries",
                                    retrySignal.failure(),
                                    503,
                                    "GEMINI_API_RETRY_EXHAUSTED"
                                );
                            }))
                    .doOnError(error -> logger.error("Gemini API call failed: {}", error.getMessage()))
                    .block();
            
            if (response == null) {
                throw new GeminiApiException("Received null response from Gemini API", 500, "NULL_RESPONSE");
            }
            
            String generatedText = response.getGeneratedText();
            if (generatedText == null || generatedText.trim().isEmpty()) {
                logger.warn("Gemini API returned empty or null content");
                throw new GeminiApiException("Gemini API returned empty content", 500, "EMPTY_RESPONSE");
            }
            
            logger.info("Successfully received response from Gemini API with length: {}", generatedText.length());
            return generatedText;
            
        } catch (WebClientResponseException e) {
            logger.error("Gemini API HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw mapWebClientException(e);
        } catch (GeminiApiException e) {
            // Re-throw our custom exceptions
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error calling Gemini API", e);
            throw new GeminiApiException("Unexpected error calling Gemini API: " + e.getMessage(), e);
        }
    }
    
    /**
     * Creates a GeminiRequest object from the prompt text
     */
    private GeminiRequest createGeminiRequest(String prompt) {
        GeminiRequest.Content content = new GeminiRequest.Content(prompt);
        GeminiRequest request = new GeminiRequest(List.of(content));
        
        // Configure generation settings
        GeminiRequest.GenerationConfig config = new GeminiRequest.GenerationConfig();
        config.setTemperature(0.7);
        config.setTopK(40);
        config.setTopP(0.95);
        config.setMaxOutputTokens(2048);
        request.setGenerationConfig(config);
        
        // Set safety settings to be permissive for business use cases
        List<GeminiRequest.SafetySetting> safetySettings = List.of(
            new GeminiRequest.SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_ONLY_HIGH"),
            new GeminiRequest.SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_ONLY_HIGH"),
            new GeminiRequest.SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_ONLY_HIGH"),
            new GeminiRequest.SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_ONLY_HIGH")
        );
        request.setSafetySettings(safetySettings);
        
        return request;
    }
    
    /**
     * Determines if an exception is retryable
     */
    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException webEx = (WebClientResponseException) throwable;
            HttpStatus status = HttpStatus.resolve(webEx.getStatusCode().value());
            
            // Retry on server errors (5xx) and rate limiting (429)
            return status != null && (
                status.is5xxServerError() || 
                status == HttpStatus.TOO_MANY_REQUESTS ||
                status == HttpStatus.REQUEST_TIMEOUT
            );
        }
        
        // Retry on timeout and connection issues
        return throwable instanceof java.util.concurrent.TimeoutException ||
               throwable instanceof java.net.ConnectException ||
               throwable instanceof java.io.IOException;
    }
    
    /**
     * Maps WebClientResponseException to appropriate GeminiApiException
     */
    private GeminiApiException mapWebClientException(WebClientResponseException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String responseBody = e.getResponseBodyAsString();
        
        if (status == null) {
            return new GeminiApiException("Unknown HTTP error: " + e.getMessage(), e, e.getStatusCode().value(), "HTTP_ERROR");
        }
        
        switch (status) {
            case BAD_REQUEST:
                return new GeminiApiException("Invalid request to Gemini API: " + responseBody, e, 400, "INVALID_REQUEST");
            case UNAUTHORIZED:
                return new GeminiApiException("Unauthorized access to Gemini API. Check API key.", e, 401, "UNAUTHORIZED");
            case FORBIDDEN:
                return new GeminiApiException("Forbidden access to Gemini API. Check permissions.", e, 403, "FORBIDDEN");
            case NOT_FOUND:
                return new GeminiApiException("Gemini API endpoint not found: " + responseBody, e, 404, "NOT_FOUND");
            case TOO_MANY_REQUESTS:
                return new GeminiApiException("Rate limit exceeded for Gemini API", e, 429, "RATE_LIMIT_EXCEEDED");
            case INTERNAL_SERVER_ERROR:
                return new GeminiApiException("Gemini API internal server error", e, 500, "SERVER_ERROR");
            case BAD_GATEWAY:
                return new GeminiApiException("Bad gateway error from Gemini API", e, 502, "BAD_GATEWAY");
            case SERVICE_UNAVAILABLE:
                return new GeminiApiException("Gemini API service unavailable", e, 503, "SERVICE_UNAVAILABLE");
            case GATEWAY_TIMEOUT:
                return new GeminiApiException("Gateway timeout from Gemini API", e, 504, "GATEWAY_TIMEOUT");
            default:
                return new GeminiApiException("HTTP error " + status.value() + ": " + responseBody, e, e.getStatusCode().value(), "HTTP_ERROR");
        }
    }
    
    /**
     * Health check method to verify Gemini API connectivity
     */
    public boolean isHealthy() {
        try {
            // Send a simple test request
            String testResponse = generateContent("Hello, this is a test. Please respond with 'OK'.");
            return testResponse != null && !testResponse.trim().isEmpty();
        } catch (Exception e) {
            logger.warn("Gemini API health check failed: {}", e.getMessage());
            return false;
        }
    }
}