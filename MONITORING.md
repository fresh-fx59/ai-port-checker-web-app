# Application Monitoring Guide

This document describes the monitoring and observability features implemented in the Gemini Web Application.

## Overview

The application includes comprehensive monitoring capabilities through Spring Boot Actuator, custom metrics, health checks, and structured logging.

## Monitoring Endpoints

### Standard Actuator Endpoints

#### Health Check
- **URL**: `/actuator/health`
- **Description**: Overall application health status
- **Components**: Database, Redis, Gemini API connectivity

```bash
curl http://localhost:8080/actuator/health
```

#### Application Info
- **URL**: `/actuator/info`
- **Description**: Application metadata and configuration
- **Includes**: Version, quota settings, startup time

```bash
curl http://localhost:8080/actuator/info
```

#### Metrics
- **URL**: `/actuator/metrics`
- **Description**: Application and JVM metrics
- **Format**: Micrometer format, Prometheus compatible

```bash
# List all available metrics
curl http://localhost:8080/actuator/metrics

# Get specific metric
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

#### Prometheus Metrics
- **URL**: `/actuator/prometheus`
- **Description**: Metrics in Prometheus format for scraping
- **Usage**: Configure Prometheus to scrape this endpoint

### Custom Endpoints

#### Application Statistics
- **URL**: `/actuator/gemini-stats`
- **Description**: Comprehensive application statistics
- **Includes**: User counts, request statistics, system metrics

```bash
curl http://localhost:8080/actuator/gemini-stats
```

**Response Example**:
```json
{
  "users": {
    "total": 150,
    "anonymous": 45,
    "registered": 105
  },
  "requests": {
    "total": 1250,
    "successful": 1180,
    "failed": 70,
    "pending": 0,
    "today": 45
  },
  "system": {
    "memory_total_mb": 512,
    "memory_free_mb": 128,
    "memory_used_mb": 384,
    "processors": 4
  },
  "application": {
    "status": "healthy",
    "timestamp": "2024-01-15T10:30:00",
    "uptime_ms": 3600000
  }
}
```

#### Quota Information
- **URL**: `/actuator/quota-info`
- **Description**: Detailed quota usage statistics
- **Includes**: Per-user-type quotas and utilization

```bash
curl http://localhost:8080/actuator/quota-info
```

**Response Example**:
```json
{
  "anonymous": {
    "total_users": 45,
    "quota_per_user": 1,
    "total_quota_available": 45
  },
  "registered": {
    "total_users": 105,
    "quota_per_user": 5,
    "total_quota_available": 525
  },
  "overall": {
    "total_requests_made": 1250,
    "total_quota_available": 570,
    "quota_utilization_percent": 219.3
  }
}
```

#### System Health Details
- **URL**: `/actuator/system-health`
- **Description**: Detailed system health information
- **Includes**: JVM health, memory usage, thread information

```bash
curl http://localhost:8080/actuator/system-health
```

## Custom Metrics

### Application Metrics

The application tracks the following custom metrics:

#### Request Metrics
- `gemini.requests.total` - Total number of prompt requests
- `gemini.requests.by_user_type` - Requests by user type (anonymous/registered)
- `gemini.processing.duration` - Time taken to process prompts

#### Authentication Metrics
- `gemini.authentication.total` - Total authentication attempts
- `gemini.authentication.attempts` - Authentication attempts by method and result

#### Error Metrics
- `gemini.errors.total` - Total number of errors
- `gemini.errors.by_type` - Errors categorized by type

#### User Metrics
- `gemini.users.total` - Total number of users (gauge)
- `gemini.users.anonymous` - Number of anonymous users (gauge)
- `gemini.users.active` - Currently active users (gauge)

#### Request Statistics
- `gemini.requests.today` - Number of requests made today (gauge)

### JVM Metrics

Standard JVM metrics are automatically collected:
- Memory usage (heap, non-heap)
- Garbage collection statistics
- Thread information
- Class loading statistics

### HTTP Metrics

HTTP request metrics include:
- Request duration percentiles (50th, 95th, 99th)
- Request count by endpoint and status code
- Response size distribution

## Health Indicators

### Custom Health Indicators

#### Database Health
- **Component**: `DatabaseHealthIndicator`
- **Checks**: PostgreSQL connection validity
- **Status**: UP/DOWN with connection details

#### Redis Health
- **Component**: `RedisHealthIndicator`
- **Checks**: Redis connectivity via ping
- **Status**: UP/DOWN with connection status

#### Gemini API Health
- **Component**: `GeminiApiHealthIndicator`
- **Checks**: Gemini API accessibility
- **Status**: UP/DOWN with API status
- **Timeout**: 5 seconds

### Health Check Configuration

Health checks can be configured per environment:

**Development**:
```yaml
management:
  endpoint:
    health:
      show-details: always
      show-components: always
```

**Production**:
```yaml
management:
  endpoint:
    health:
      show-details: when-authorized
      show-components: when-authorized
```

## Logging

### Log Categories

#### Application Logs
- **File**: `logs/gemini-web-app.log`
- **Level**: INFO (production), DEBUG (development)
- **Rotation**: Daily, 30 days retention

#### Security Logs
- **File**: `logs/security.log`
- **Content**: Authentication events, security violations
- **Format**: Structured logging for SIEM integration
- **Rotation**: Daily, 30 days retention

#### Metrics Logs
- **File**: `logs/metrics.log`
- **Content**: Periodic metrics snapshots
- **Format**: Key-value pairs for parsing
- **Rotation**: Daily, 7 days retention

#### Audit Logs
- **File**: `logs/audit.log`
- **Content**: User actions, system events
- **Format**: Structured audit trail
- **Rotation**: Daily, 90 days retention

### Structured Logging

The application uses structured logging for better observability:

```java
// Security event logging
StructuredLogging.logSecurityEvent("login_attempt", userId, ipAddress, "success");

// Metrics logging
StructuredLogging.logMetric("request_duration", 150.5, "endpoint", "/api/prompt");

// Audit logging
StructuredLogging.logAuditEvent("prompt_submitted", userId, "gemini_api", "success");
```

### Log Levels

- **ERROR**: System errors, exceptions
- **WARN**: Warnings, degraded performance
- **INFO**: Normal operations, important events
- **DEBUG**: Detailed debugging information (dev only)

## Scheduled Monitoring

### System Metrics Collection
- **Frequency**: Every 5 minutes
- **Metrics**: Memory, threads, users, requests
- **Action**: Log structured metrics

### Stuck Request Detection
- **Frequency**: Every 10 minutes
- **Check**: Requests pending > 30 minutes
- **Action**: Alert and log security event

### Quota Statistics
- **Frequency**: Every hour
- **Metrics**: Quota utilization by user type
- **Action**: Log usage statistics

### Health Checks
- **Frequency**: Every 2 minutes
- **Checks**: Database, memory, system health
- **Action**: Log health status, alert on issues

### Daily Summary
- **Frequency**: Daily at 1 AM
- **Content**: Previous day's statistics
- **Action**: Generate summary report

## Alerting

### Memory Alerts
- **Trigger**: Memory usage > 90%
- **Action**: Log security event, record metric
- **Severity**: Warning (75-90%), Critical (>90%)

### Stuck Requests
- **Trigger**: Requests pending > 30 minutes
- **Action**: Log security event, investigate
- **Severity**: Warning

### Error Rate
- **Trigger**: Error rate > 10% over 5 minutes
- **Action**: Record metric, investigate
- **Severity**: Critical

## Integration with External Systems

### Prometheus Integration

Configure Prometheus to scrape metrics:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'gemini-web-app'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s
```

### Grafana Dashboards

Key metrics to monitor in Grafana:
- Request rate and latency
- Error rate by endpoint
- Memory and CPU usage
- User registration trends
- Quota utilization

### Log Aggregation

For centralized logging (ELK Stack, Splunk):
- Configure log forwarding from `/logs/` directory
- Use structured log format for parsing
- Set up alerts based on log patterns

## Monitoring Best Practices

### Development Environment
- Enable all actuator endpoints
- Use DEBUG logging level
- Monitor application startup and shutdown
- Test health check endpoints

### Production Environment
- Restrict actuator endpoints access
- Use INFO logging level
- Set up external monitoring
- Configure alerting thresholds
- Regular health check monitoring

### Security Considerations
- Secure actuator endpoints with authentication
- Monitor security logs for suspicious activity
- Set up alerts for authentication failures
- Regular security audit of monitoring data

## Troubleshooting

### Common Issues

#### High Memory Usage
1. Check `/actuator/system-health` for memory details
2. Review application logs for memory leaks
3. Monitor GC metrics in `/actuator/metrics`

#### Database Connection Issues
1. Check `/actuator/health` for database status
2. Review connection pool metrics
3. Verify database connectivity

#### API Performance Issues
1. Monitor request duration metrics
2. Check external API health indicators
3. Review error logs for patterns

### Monitoring Commands

```bash
# Check overall health
curl http://localhost:8080/actuator/health

# Get memory metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Check request statistics
curl http://localhost:8080/actuator/gemini-stats

# Monitor logs in real-time
tail -f logs/gemini-web-app.log

# Check security events
tail -f logs/security.log
```

## Performance Tuning

### JVM Tuning
- Monitor heap usage patterns
- Adjust garbage collection settings
- Set appropriate memory limits

### Database Tuning
- Monitor connection pool usage
- Optimize query performance
- Set appropriate timeout values

### Application Tuning
- Monitor request processing times
- Optimize external API calls
- Implement caching where appropriate