package com.example.geminiapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration for the Gemini Web Application.
 * Configures JPA repositories and transaction management.
 * DataSource configuration is handled by Spring Boot auto-configuration.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.example.geminiapp.repository")
@EnableTransactionManagement
public class DatabaseConfig {
    // Spring Boot auto-configuration handles DataSource, JPA, and Flyway setup
    // Configuration is driven by application.yml properties
}