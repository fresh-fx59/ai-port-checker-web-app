package com.example.geminiapp.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

/**
 * Configuration for application logging with monitoring integration.
 */
@Configuration
public class LoggingConfig {

    @Value("${logging.file.name:logs/gemini-web-app.log}")
    private String logFileName;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Configure structured logging for monitoring systems
     */
    @PostConstruct
    public void configureLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // Configure security logger
        configureSecurityLogger(context);
        
        // Configure metrics logger
        configureMetricsLogger(context);
        
        // Configure audit logger
        configureAuditLogger(context);
    }

    private void configureSecurityLogger(LoggerContext context) {
        Logger securityLogger = context.getLogger("SECURITY");
        securityLogger.setAdditive(false);

        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(context);
        appender.setName("SECURITY_FILE");
        appender.setFile("logs/security.log");

        TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
        policy.setContext(context);
        policy.setParent(appender);
        policy.setFileNamePattern("logs/security.%d{yyyy-MM-dd}.%i.log.gz");
        policy.setMaxHistory(30);
        policy.start();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [SECURITY] %logger{36} - %msg%n");
        encoder.start();

        appender.setRollingPolicy(policy);
        appender.setEncoder(encoder);
        appender.start();

        securityLogger.addAppender(appender);
    }

    private void configureMetricsLogger(LoggerContext context) {
        Logger metricsLogger = context.getLogger("METRICS");
        metricsLogger.setAdditive(false);

        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(context);
        appender.setName("METRICS_FILE");
        appender.setFile("logs/metrics.log");

        TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
        policy.setContext(context);
        policy.setParent(appender);
        policy.setFileNamePattern("logs/metrics.%d{yyyy-MM-dd}.%i.log.gz");
        policy.setMaxHistory(7);
        policy.start();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} [METRICS] %msg%n");
        encoder.start();

        appender.setRollingPolicy(policy);
        appender.setEncoder(encoder);
        appender.start();

        metricsLogger.addAppender(appender);
    }

    private void configureAuditLogger(LoggerContext context) {
        Logger auditLogger = context.getLogger("AUDIT");
        auditLogger.setAdditive(false);

        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(context);
        appender.setName("AUDIT_FILE");
        appender.setFile("logs/audit.log");

        TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
        policy.setContext(context);
        policy.setParent(appender);
        policy.setFileNamePattern("logs/audit.%d{yyyy-MM-dd}.%i.log.gz");
        policy.setMaxHistory(90); // Keep audit logs longer
        policy.start();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} [AUDIT] %msg%n");
        encoder.start();

        appender.setRollingPolicy(policy);
        appender.setEncoder(encoder);
        appender.start();

        auditLogger.addAppender(appender);
    }
}

