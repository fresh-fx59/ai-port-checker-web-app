package com.example.geminiapp.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.example.geminiapp.repository.UserRepository;
import com.example.geminiapp.repository.RequestLogRepository;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class MetricsConfig {

    @Bean
    public ApplicationMetrics applicationMetrics(MeterRegistry meterRegistry) {
        return new ApplicationMetrics(meterRegistry);
    }

    @Component
    public static class ApplicationMetrics {

        private final MeterRegistry meterRegistry;
        private final Counter promptRequestCounter;
        private final Counter authenticationCounter;
        private final Counter errorCounter;
        private final Timer promptProcessingTimer;
        private final AtomicInteger activeUsers;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RequestLogRepository requestLogRepository;

        public ApplicationMetrics(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            this.activeUsers = new AtomicInteger(0);

            // Counters
            this.promptRequestCounter = Counter.builder("gemini.requests.total")
                    .description("Total number of prompt requests")
                    .register(meterRegistry);

            this.authenticationCounter = Counter.builder("gemini.authentication.total")
                    .description("Total number of authentication attempts")
                    .register(meterRegistry);

            this.errorCounter = Counter.builder("gemini.errors.total")
                    .description("Total number of errors")
                    .register(meterRegistry);

            // Timer
            this.promptProcessingTimer = Timer.builder("gemini.processing.duration")
                    .description("Time taken to process prompts")
                    .register(meterRegistry);

            // Gauges
            Gauge.builder("gemini.users.total", this, ApplicationMetrics::getTotalUsers)
                    .description("Total number of registered users")
                    .register(meterRegistry);

            Gauge.builder("gemini.users.anonymous", this, ApplicationMetrics::getAnonymousUsers)
                    .description("Total number of anonymous users")
                    .register(meterRegistry);

            Gauge.builder("gemini.requests.today", this, ApplicationMetrics::getTodayRequests)
                    .description("Number of requests made today")
                    .register(meterRegistry);

            Gauge.builder("gemini.users.active", this, ApplicationMetrics::getActiveUsers)
                    .description("Number of currently active users")
                    .register(meterRegistry);
        }

        // Counter methods
        public void incrementPromptRequests() {
            promptRequestCounter.increment();
        }

        public void incrementPromptRequests(String userType) {
            Counter.builder("gemini.requests.by_user_type")
                    .tag("user_type", userType)
                    .description("Prompt requests by user type")
                    .register(meterRegistry)
                    .increment();
        }

        public void incrementAuthentication(String method, String result) {
            Counter.builder("gemini.authentication.attempts")
                    .tag("method", method)
                    .tag("result", result)
                    .description("Authentication attempts by method and result")
                    .register(meterRegistry)
                    .increment();
        }

        public void incrementErrors(String errorType) {
            Counter.builder("gemini.errors.by_type")
                    .tag("error_type", errorType)
                    .description("Errors by type")
                    .register(meterRegistry)
                    .increment();
        }

        // Timer methods
        public Timer.Sample startPromptProcessingTimer() {
            return Timer.start(meterRegistry);
        }

        public void recordPromptProcessingTime(Timer.Sample sample) {
            sample.stop(promptProcessingTimer);
        }

        // Gauge methods
        private double getTotalUsers() {
            try {
                return userRepository.count();
            } catch (Exception e) {
                return 0;
            }
        }

        private double getAnonymousUsers() {
            try {
                return userRepository.countByIsAnonymousTrue();
            } catch (Exception e) {
                return 0;
            }
        }

        private double getTodayRequests() {
            try {
                return requestLogRepository.countTodayRequests();
            } catch (Exception e) {
                return 0;
            }
        }

        private double getActiveUsers() {
            return activeUsers.get();
        }

        // Active user management
        public void incrementActiveUsers() {
            activeUsers.incrementAndGet();
        }

        public void decrementActiveUsers() {
            activeUsers.decrementAndGet();
        }
    }
}