# Docker Setup Guide

This document provides instructions for running the Gemini Web Application using Docker.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- At least 4GB of available RAM
- At least 2GB of available disk space

## Quick Start

### Development Environment

1. **Clone the repository and navigate to the project directory**

2. **Create environment file**
   ```bash
   cp .env.example .env
   ```
   
   Edit `.env` file with your actual API keys:
   ```env
   GEMINI_API_KEY=your-actual-gemini-api-key
   GOOGLE_CLIENT_ID=your-google-oauth-client-id
   GOOGLE_CLIENT_SECRET=your-google-oauth-client-secret
   JWT_SECRET=your-secure-jwt-secret-key
   ```

3. **Start the development environment**
   ```bash
   docker-compose up -d
   ```

4. **Access the application**
   - Backend API: http://localhost:8080
   - API Documentation: http://localhost:8080/actuator
   - Health Check: http://localhost:8080/actuator/health

5. **Start frontend (optional)**
   ```bash
   docker-compose --profile frontend up -d
   ```
   - Frontend: http://localhost:3000

### Production Environment

1. **Create production environment file**
   ```bash
   cp .env.example .env.prod
   ```
   
   Configure production values in `.env.prod`:
   ```env
   DB_USER=secure_db_user
   DB_PASSWORD=secure_db_password
   REDIS_PASSWORD=secure_redis_password
   GEMINI_API_KEY=your-production-gemini-api-key
   GOOGLE_CLIENT_ID=your-production-google-client-id
   GOOGLE_CLIENT_SECRET=your-production-google-client-secret
   JWT_SECRET=your-production-jwt-secret-key
   CORS_ALLOWED_ORIGINS=https://yourdomain.com
   ```

2. **Start production environment**
   ```bash
   docker-compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d
   ```

## Services

### PostgreSQL Database
- **Image**: postgres:15-alpine
- **Port**: 5432 (development only)
- **Database**: gemini_web_app_dev (dev) / gemini_web_app (prod)
- **Volume**: Persistent data storage

### Redis Cache
- **Image**: redis:7-alpine
- **Port**: 6379 (development only)
- **Volume**: Persistent data storage
- **Features**: Append-only file persistence

### Spring Boot Application
- **Port**: 8080
- **Health Check**: /actuator/health
- **Profiles**: dev (development) / prod (production)
- **Dependencies**: PostgreSQL, Redis

### Frontend (Optional)
- **Development**: React dev server on port 3000
- **Production**: Nginx serving static files on port 80

## Docker Commands

### Basic Operations
```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f [service-name]

# Restart a service
docker-compose restart [service-name]

# Rebuild and start
docker-compose up -d --build
```

### Database Operations
```bash
# Access PostgreSQL
docker-compose exec postgres psql -U gemini_user -d gemini_web_app_dev

# Backup database
docker-compose exec postgres pg_dump -U gemini_user gemini_web_app_dev > backup.sql

# Restore database
docker-compose exec -T postgres psql -U gemini_user -d gemini_web_app_dev < backup.sql
```

### Redis Operations
```bash
# Access Redis CLI
docker-compose exec redis redis-cli

# Monitor Redis
docker-compose exec redis redis-cli monitor
```

### Application Operations
```bash
# View application logs
docker-compose logs -f app

# Access application container
docker-compose exec app sh

# Check application health
curl http://localhost:8080/actuator/health
```

## Volumes

### Development
- `postgres_data`: PostgreSQL data directory
- `redis_data`: Redis data directory
- `./logs`: Application logs (mounted from host)

### Production
- `postgres_prod_data`: PostgreSQL production data
- `redis_prod_data`: Redis production data

## Environment Variables

### Required
- `GEMINI_API_KEY`: Google Gemini API key
- `GOOGLE_CLIENT_ID`: Google OAuth client ID
- `GOOGLE_CLIENT_SECRET`: Google OAuth client secret

### Optional (with defaults)
- `JWT_SECRET`: JWT signing secret
- `CORS_ALLOWED_ORIGINS`: Allowed CORS origins
- `DB_USER`: Database username (production)
- `DB_PASSWORD`: Database password (production)
- `REDIS_PASSWORD`: Redis password (production)

## Networking

All services communicate through the `gemini-network` bridge network:
- Services can reach each other using service names as hostnames
- External access is only available through exposed ports
- Production setup doesn't expose database ports externally

## Health Checks

All services include health checks:
- **PostgreSQL**: `pg_isready` command
- **Redis**: `redis-cli ping` command
- **Application**: HTTP check on `/actuator/health`

## Troubleshooting

### Common Issues

1. **Port conflicts**
   ```bash
   # Check what's using the port
   lsof -i :8080
   
   # Change ports in docker-compose.yml if needed
   ```

2. **Database connection issues**
   ```bash
   # Check if PostgreSQL is ready
   docker-compose exec postgres pg_isready -U gemini_user
   
   # Check application logs
   docker-compose logs app
   ```

3. **Memory issues**
   ```bash
   # Check Docker resource usage
   docker stats
   
   # Increase Docker memory limit if needed
   ```

4. **Build issues**
   ```bash
   # Clean build
   docker-compose down
   docker system prune -f
   docker-compose up -d --build
   ```

### Logs and Monitoring

```bash
# View all logs
docker-compose logs

# Follow specific service logs
docker-compose logs -f app

# Check resource usage
docker-compose top

# View container status
docker-compose ps
```

## Security Considerations

### Development
- Default passwords are used (change for production)
- All ports are exposed for debugging
- Debug logging is enabled

### Production
- Use strong, unique passwords
- Database and Redis ports are not exposed
- Security headers are configured
- Use HTTPS in production (configure reverse proxy)

## Backup and Recovery

### Database Backup
```bash
# Create backup
docker-compose exec postgres pg_dump -U gemini_user -d gemini_web_app > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore from backup
docker-compose exec -T postgres psql -U gemini_user -d gemini_web_app < backup_file.sql
```

### Volume Backup
```bash
# Backup volumes
docker run --rm -v gemini-web-app_postgres_data:/data -v $(pwd):/backup alpine tar czf /backup/postgres_backup.tar.gz -C /data .
```

## Performance Tuning

### PostgreSQL
- Adjust `shared_buffers` and `effective_cache_size` in production
- Configure connection pooling
- Monitor query performance

### Redis
- Configure appropriate memory limits
- Use Redis persistence settings based on requirements
- Monitor memory usage

### Application
- Adjust JVM heap size using `JAVA_OPTS` environment variable
- Configure connection pool sizes
- Monitor application metrics via Actuator