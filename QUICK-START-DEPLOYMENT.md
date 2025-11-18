# Quick Start - Automated Deployment

Get your Gemini Web App deployed with automatic updates in minutes!

## Overview

This setup uses:
- **GitHub Actions** to build Docker images on every push to `main`
- **GitHub Container Registry (GHCR)** to store images
- **Watchtower** to automatically update your server when new images are available

## Step-by-Step Guide

### 1. On Your Server (One-Time Setup)

```bash
# Clone your repository
git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git
cd YOUR_REPO

# Run the automated setup script
./server-setup.sh

# This script will:
# - Install Docker and Docker Compose
# - Authenticate with GitHub Container Registry
# - Create .env.production template
# - Generate secure passwords
# - Configure firewall (optional)
```

### 2. Configure Your Environment

```bash
# Edit the production environment file
nano .env.production

# Required changes:
# - DATABASE_USERNAME: Choose a secure username
# - DATABASE_PASSWORD: Use generated password from setup script
# - REDIS_PASSWORD: Use generated password from setup script
# - JWT_SECRET: Use generated secret from setup script
# - GOOGLE_CLIENT_ID: Your Google OAuth client ID
# - GOOGLE_CLIENT_SECRET: Your Google OAuth client secret
# - GEMINI_API_KEY: Your Gemini API key
# - CORS_ALLOWED_ORIGINS: Your domain (e.g., https://yourdomain.com)
# - GITHUB_REPOSITORY: YOUR_USERNAME/YOUR_REPO
# - VITE_API_URL: Your API URL (e.g., https://yourdomain.com:8080)
```

### 3. First Deployment

```bash
# Make sure GitHub Actions has run at least once
# (push something to main branch or run workflow manually)

# Pull the images
docker compose -f docker-compose.ghcr.yml --env-file .env.production pull

# Start all services
./deploy.sh start

# Check status
./deploy.sh status

# View logs
./deploy.sh logs-f
```

### 4. Verify Deployment

```bash
# Check backend health
curl http://localhost:8080/actuator/health

# Check frontend
curl http://localhost:80

# View Watchtower logs
./deploy.sh watchtower
```

## How Automatic Updates Work

1. You push code to `main` branch
2. GitHub Actions builds new Docker images (~5-10 minutes)
3. Images are pushed to GHCR with `latest` tag
4. Watchtower checks GHCR every 5 minutes
5. New images are automatically pulled and deployed
6. **Total deployment time: ~10-15 minutes** from push to live

## Daily Usage

### Deploy New Changes
```bash
# On your development machine
git add .
git commit -m "Your changes"
git push origin main

# That's it! Watchtower handles the rest automatically.
# Check progress on your server:
./deploy.sh watchtower
```

### Management Commands

```bash
./deploy.sh start       # Start all services
./deploy.sh stop        # Stop all services
./deploy.sh restart     # Restart all services
./deploy.sh status      # Show service status
./deploy.sh logs        # View logs
./deploy.sh logs-f      # Follow logs (Ctrl+C to exit)
./deploy.sh update      # Manually pull and update
./deploy.sh health      # Check backend health
./deploy.sh backup-db   # Backup database
./deploy.sh watchtower  # View Watchtower logs
```

### Service-Specific Logs
```bash
./deploy.sh logs-backend   # Backend logs
./deploy.sh logs-frontend  # Frontend logs
./deploy.sh logs-db        # Database logs
./deploy.sh logs-redis     # Redis logs
```

## Troubleshooting

### Images not updating automatically?

```bash
# Check Watchtower logs
./deploy.sh watchtower

# Verify GHCR authentication
cat ~/.docker/config.json

# Re-authenticate if needed
echo "YOUR_PAT" | docker login ghcr.io -u YOUR_USERNAME --password-stdin

# Restart Watchtower
docker compose -f docker-compose.ghcr.yml restart watchtower
```

### Service won't start?

```bash
# Check logs for specific service
./deploy.sh logs-backend

# Verify environment variables
docker compose -f docker-compose.ghcr.yml --env-file .env.production config

# Check container status
docker ps -a
```

### Database issues?

```bash
# Check PostgreSQL
docker compose -f docker-compose.ghcr.yml ps postgres

# Test database connectivity
docker exec gemini-postgres pg_isready

# View database logs
./deploy.sh logs-db
```

## Security Checklist

Before going live:
- [ ] Strong passwords set in `.env.production`
- [ ] JWT_SECRET is long and random (64+ characters)
- [ ] CORS_ALLOWED_ORIGINS set to your actual domain
- [ ] Firewall configured (ports 22, 80, 443 only)
- [ ] HTTPS enabled (use Let's Encrypt)
- [ ] Google OAuth configured for production domain
- [ ] Database backups automated

## Next Steps

- **Enable HTTPS**: See DEPLOYMENT.md for Let's Encrypt setup
- **Set up monitoring**: Configure health checks and alerts
- **Regular backups**: Schedule automated database backups
- **Domain configuration**: Point your domain to server IP
- **SSL certificates**: Install SSL for production

For detailed information, see [DEPLOYMENT.md](DEPLOYMENT.md)
