package com.example.geminiapp.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PromptValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPrompt {
    String message() default "Invalid prompt content";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}