package com.example.geminiapp.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, List<String>> fieldErrors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Input validation failed")
            .path(request.getRequestURI())
            .fieldErrors(fieldErrors)
            .build();

        logger.warn("Validation error on {}: {}", request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        Map<String, List<String>> fieldErrors = new HashMap<>();
        
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            fieldErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Constraint Violation")
            .message("Input constraint validation failed")
            .path(request.getRequestURI())
            .fieldErrors(fieldErrors)
            .build();

        logger.warn("Constraint violation on {}: {}", request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle custom validation exceptions
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Error")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .fieldErrors(ex.getFieldErrors())
            .build();

        logger.warn("Custom validation error on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle rate limit exceeded exceptions
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException ex, HttpServletRequest request) {
        
        Map<String, Object> details = Map.of(
            "identifier", ex.getIdentifier(),
            "limits", Map.of(
                "perMinute", ex.getLimitPerMinute(),
                "perHour", ex.getLimitPerHour(),
                "perDay", ex.getLimitPerDay()
            ),
            "current", Map.of(
                "lastMinute", ex.getRequestsInLastMinute(),
                "lastHour", ex.getRequestsInLastHour(),
                "lastDay", ex.getRequestsInLastDay()
            )
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.TOO_MANY_REQUESTS.value())
            .error("Rate Limit Exceeded")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .details(details)
            .build();

        logger.warn("Rate limit exceeded for {} on {}: {}", ex.getIdentifier(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    /**
     * Handle quota exceeded exceptions
     */
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceededException(
            QuotaExceededException ex, HttpServletRequest request) {
        
        Map<String, Object> details = Map.of(
            "remainingRequests", ex.getRemainingRequests(),
            "isAnonymous", ex.isAnonymous(),
            "suggestion", ex.isAnonymous() ? "Please register for more requests" : "Quota will reset in 24 hours"
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.TOO_MANY_REQUESTS.value())
            .error("Quota Exceeded")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .details(details)
            .build();

        logger.warn("Quota exceeded on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    /**
     * Handle Gemini API exceptions
     */
    @ExceptionHandler(GeminiApiException.class)
    public ResponseEntity<ErrorResponse> handleGeminiApiException(
            GeminiApiException ex, HttpServletRequest request) {
        
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode());
        
        Map<String, Object> details = Map.of(
            "errorCode", ex.getErrorCode(),
            "apiStatusCode", ex.getStatusCode()
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error("AI Service Error")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .details(details)
            .build();

        logger.error("Gemini API error on {}: {} (Status: {})", request.getRequestURI(), ex.getMessage(), ex.getStatusCode());
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handle invalid prompt exceptions
     */
    @ExceptionHandler(InvalidPromptException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPromptException(
            InvalidPromptException ex, HttpServletRequest request) {
        
        Map<String, Object> details = Map.of(
            "validationError", ex.getValidationError()
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Invalid Prompt")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .details(details)
            .build();

        logger.warn("Invalid prompt on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            Exception ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Authentication Failed")
            .message("Invalid credentials or authentication required")
            .path(request.getRequestURI())
            .build();

        logger.warn("Authentication failed on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Access Denied")
            .message("You don't have permission to access this resource")
            .path(request.getRequestURI())
            .build();

        logger.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle method not supported exceptions
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
        String supportedMethods = String.join(", ", ex.getSupportedMethods());
        
        Map<String, Object> details = Map.of(
            "supportedMethods", supportedMethods,
            "requestedMethod", ex.getMethod()
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.METHOD_NOT_ALLOWED.value())
            .error("Method Not Allowed")
            .message(String.format("Method '%s' not supported. Supported methods: %s", ex.getMethod(), supportedMethods))
            .path(request.getRequestURI())
            .details(details)
            .build();

        logger.warn("Method not allowed on {}: {} (Supported: {})", request.getRequestURI(), ex.getMethod(), supportedMethods);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    /**
     * Handle missing request parameter exceptions
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        Map<String, Object> details = Map.of(
            "parameterName", ex.getParameterName(),
            "parameterType", ex.getParameterType()
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Missing Parameter")
            .message(String.format("Required parameter '%s' is missing", ex.getParameterName()))
            .path(request.getRequestURI())
            .details(details)
            .build();

        logger.warn("Missing parameter on {}: {}", request.getRequestURI(), ex.getParameterName());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        Map<String, Object> details = Map.of(
            "parameterName", ex.getName(),
            "expectedType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
            "providedValue", ex.getValue()
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Type Mismatch")
            .message(String.format("Parameter '%s' has invalid type", ex.getName()))
            .path(request.getRequestURI())
            .details(details)
            .build();

        logger.warn("Type mismatch on {}: {} (Expected: {}, Got: {})", 
            request.getRequestURI(), ex.getName(), 
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown", 
            ex.getValue());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle malformed JSON exceptions
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Malformed Request")
            .message("Request body is malformed or unreadable")
            .path(request.getRequestURI())
            .build();

        logger.warn("Malformed request on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle 404 not found exceptions
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL()))
            .path(request.getRequestURI())
            .build();

        logger.warn("No handler found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .path(request.getRequestURI())
            .build();

        logger.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Standard error response structure
     */
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, List<String>> fieldErrors;
        private Map<String, Object> details;

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final ErrorResponse errorResponse = new ErrorResponse();

            public Builder timestamp(LocalDateTime timestamp) {
                errorResponse.timestamp = timestamp;
                return this;
            }

            public Builder status(int status) {
                errorResponse.status = status;
                return this;
            }

            public Builder error(String error) {
                errorResponse.error = error;
                return this;
            }

            public Builder message(String message) {
                errorResponse.message = message;
                return this;
            }

            public Builder path(String path) {
                errorResponse.path = path;
                return this;
            }

            public Builder fieldErrors(Map<String, List<String>> fieldErrors) {
                errorResponse.fieldErrors = fieldErrors;
                return this;
            }

            public Builder details(Map<String, Object> details) {
                errorResponse.details = details;
                return this;
            }

            public ErrorResponse build() {
                return errorResponse;
            }
        }

        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public int getStatus() { return status; }
        public String getError() { return error; }
        public String getMessage() { return message; }
        public String getPath() { return path; }
        public Map<String, List<String>> getFieldErrors() { return fieldErrors; }
        public Map<String, Object> getDetails() { return details; }
    }
}