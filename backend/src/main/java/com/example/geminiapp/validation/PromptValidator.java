package com.example.geminiapp.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PromptValidator implements ConstraintValidator<ValidPrompt, String> {

    // Patterns for detecting potentially malicious content
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|onload|onerror)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|</script|javascript:|vbscript:|onload=|onerror=|onclick=|onmouseover=|<iframe|</iframe)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "(?i)(\\||&&|;|`|\\$\\(|\\$\\{|\\\\x|\\\\u|eval\\(|system\\(|exec\\()",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public void initialize(ValidPrompt constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String prompt, ConstraintValidatorContext context) {
        if (prompt == null || prompt.trim().isEmpty()) {
            addViolation(context, "Prompt cannot be empty");
            return false;
        }

        // Check for excessive whitespace or control characters
        if (containsExcessiveWhitespace(prompt)) {
            addViolation(context, "Prompt contains excessive whitespace or control characters");
            return false;
        }

        // Check for potential SQL injection
        if (SQL_INJECTION_PATTERN.matcher(prompt).find()) {
            addViolation(context, "Prompt contains potentially malicious SQL content");
            return false;
        }

        // Check for potential XSS
        if (XSS_PATTERN.matcher(prompt).find()) {
            addViolation(context, "Prompt contains potentially malicious script content");
            return false;
        }

        // Check for potential command injection
        if (COMMAND_INJECTION_PATTERN.matcher(prompt).find()) {
            addViolation(context, "Prompt contains potentially malicious command content");
            return false;
        }

        // Check for reasonable character distribution (not all special characters)
        if (!hasReasonableCharacterDistribution(prompt)) {
            addViolation(context, "Prompt has unusual character distribution");
            return false;
        }

        return true;
    }

    private boolean containsExcessiveWhitespace(String prompt) {
        // Check for excessive consecutive whitespace
        if (prompt.matches(".*\\s{10,}.*")) {
            return true;
        }
        
        // Check for control characters (except common ones like newline, tab)
        for (char c : prompt.toCharArray()) {
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                return true;
            }
        }
        
        return false;
    }

    private boolean hasReasonableCharacterDistribution(String prompt) {
        int alphanumericCount = 0;
        int totalCount = prompt.length();
        
        for (char c : prompt.toCharArray()) {
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
                alphanumericCount++;
            }
        }
        
        // At least 50% should be alphanumeric or whitespace
        return (double) alphanumericCount / totalCount >= 0.5;
    }

    private void addViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}