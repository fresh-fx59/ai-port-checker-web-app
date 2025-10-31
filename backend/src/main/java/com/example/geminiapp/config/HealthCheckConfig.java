package com.example.geminiapp.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class HealthCheckConfig {

    @Bean
    public InfoContributor applicationInfoContributor() {
        return new ApplicationInfoContributor();
    }

    @Component
    public static class ApplicationInfoContributor implements InfoContributor {
        
        @Value("${spring.application.name:gemini-web-app}")
        private String applicationName;
        
        @Value("${app.quota.anonymous-requests:1}")
        private int anonymousQuota;
        
        @Value("${app.quota.registered-requests:5}")
        private int registeredQuota;

        @Override
        public void contribute(Info.Builder builder) {
            Map<String, Object> appDetails = new HashMap<>();
            appDetails.put("name", applicationName);
            appDetails.put("version", "1.0.0");
            appDetails.put("description", "Gemini AI Web Application");
            appDetails.put("startup-time", LocalDateTime.now().toString());
            
            Map<String, Object> quotaInfo = new HashMap<>();
            quotaInfo.put("anonymous-requests", anonymousQuota);
            quotaInfo.put("registered-requests", registeredQuota);
            
            builder.withDetail("application", appDetails);
            builder.withDetail("quota-configuration", quotaInfo);
        }
    }

    @Component
    public static class DatabaseHealthIndicator implements HealthIndicator {

        @Autowired
        private DataSource dataSource;

        @Override
        public Health health() {
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(1)) {
                    return Health.up()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("status", "Connection successful")
                            .withDetail("validation-query", "SELECT 1")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("status", "Connection validation failed")
                            .build();
                }
            } catch (Exception e) {
                return Health.down()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("status", "Connection failed")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
    }

    @Component
    @ConditionalOnProperty(name = "management.health.redis.enabled", havingValue = "true", matchIfMissing = true)
    public static class RedisHealthIndicator implements HealthIndicator {

        @Autowired
        private RedisConnectionFactory redisConnectionFactory;

        @Override
        public Health health() {
            try {
                redisConnectionFactory.getConnection().ping();
                return Health.up()
                        .withDetail("redis", "Available")
                        .withDetail("status", "Connection successful")
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("redis", "Unavailable")
                        .withDetail("status", "Connection failed")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
    }

    @Component
    public static class GeminiApiHealthIndicator implements HealthIndicator {

        @Value("${gemini.api.base-url}")
        private String geminiApiBaseUrl;

        @Value("${gemini.api.key}")
        private String geminiApiKey;

        private final WebClient webClient;

        public GeminiApiHealthIndicator() {
            this.webClient = WebClient.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                    .build();
        }

        @Override
        public Health health() {
            try {
                // Simple connectivity check to Gemini API base URL
                String response = webClient.get()
                        .uri(geminiApiBaseUrl)
                        .header("x-goog-api-key", geminiApiKey)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(5))
                        .block();

                return Health.up()
                        .withDetail("gemini-api", "Available")
                        .withDetail("base-url", geminiApiBaseUrl)
                        .withDetail("status", "API accessible")
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("gemini-api", "Unavailable")
                        .withDetail("base-url", geminiApiBaseUrl)
                        .withDetail("status", "API not accessible")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
    }
}