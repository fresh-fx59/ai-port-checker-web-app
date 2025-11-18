package com.example.geminiapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password Encoder Configuration
 *
 * Separated from SecurityConfig to avoid circular dependency issues.
 * This allows UserService to inject PasswordEncoder without creating a cycle.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Creates a BCrypt password encoder bean
     * BCrypt is a secure one-way hashing algorithm with built-in salt
     *
     * @return PasswordEncoder instance using BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
