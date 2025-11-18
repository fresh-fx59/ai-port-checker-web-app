# Deployment Guide - GitHub Actions + Docker + Watchtower

This guide explains how to deploy the Gemini Web App using GitHub Actions to build Docker images and Watchtower for automatic updates on your server.

## Architecture Overview

```
GitHub Push to main
    ↓
GitHub Actions builds Docker images
    ↓
Images pushed to GitHub Container Registry (GHCR)
    ↓
Watchtower on server polls GHCR every 5 minutes
    ↓
New images automatically pulled and deployed
```

## Prerequisites

- Docker server running Ubuntu (or any Docker-compatible OS)
- Docker and Docker Compose installed on server
- GitHub account with repository
- Domain name (optional, for production)

---

## Part 0: Local Testing (Start Here!)

**⚠️ IMPORTANT:** Always test your application locally before deploying to production!

### Quick Start with Automated Script

The easiest way to set up local testing:

```bash
# 1. Edit .env.local with your API keys
# Required: GEMINI_API_KEY from https://makersuite.google.com/app/apikey
# Optional: GOOGLE_CLIENT_ID/SECRET for OAuth
nano .env.local

# 2. Run the setup script
./test-local.sh          # On macOS/Linux
# or
test-local.bat           # On Windows

# 3. Follow the on-screen instructions to start backend and frontend
```

### Manual Local Setup

If you prefer to set up manually:

#### 1. Edit Local Environment File

```bash
# Edit .env.local with your actual values
nano .env.local
```

**Minimum required configuration for `.env.local`:**
```bash
# Database (uses Docker containers)
DATABASE_URL=jdbc:postgresql://localhost:5432/gemini_web_app_dev
DATABASE_USERNAME=gemini_user
DATABASE_PASSWORD=gemini_password

# Gemini API (REQUIRED - get from https://makersuite.google.com/app/apikey)
GEMINI_API_KEY=your-actual-gemini-api-key

# Security
JWT_SECRET=local-dev-secret-at-least-32-characters-long

# CORS (allow all local ports)
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

#### 2. Start Database Services

**Option A: With Redis (recommended for testing production-like setup)**
```bash
docker compose up -d postgres redis
```

**Option B: Without Redis (simpler, for basic testing)**
```bash
docker compose -f docker-compose.no-redis.yml up -d postgres
```

Verify services are running:
```bash
docker compose ps
docker exec gemini-postgres pg_isready -U gemini_user
```

#### 3. Start Backend

```bash
cd backend

# Load environment variables (Linux/macOS)
export $(grep -v '^#' ../.env.local | xargs)

# Start backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev      # With Redis
# or
mvn spring-boot:run -Dspring-boot.run.profiles=no-redis # Without Redis
```

Backend runs on: **http://localhost:8080**

Verify backend health:
```bash
curl http://localhost:8080/actuator/health
```

#### 4. Start Frontend (in a new terminal)

```bash
cd frontend

# Install dependencies (first time only)
npm install

# Start dev server
npm run dev
```

Frontend runs on: **http://localhost:5173** (or :3000 if 5173 is busy)

#### 5. Test the Application

1. **Open browser:** Navigate to http://localhost:5173
2. **Anonymous test:** Submit a prompt without logging in (uses browser fingerprint, 1 request quota)
3. **Register:** Create an account with email/password
4. **Authenticated test:** Login and submit more prompts (5 request quota)
5. **View history:** Check your request history page
6. **Test OAuth (optional):** If you configured Google OAuth, test "Login with Google"

#### 6. Verify Everything Works

```bash
# Check backend logs
# (visible in the terminal where you ran mvn spring-boot:run)

# Check database
docker exec -it gemini-postgres psql -U gemini_user -d gemini_web_app_dev
# Inside psql:
# \dt                          -- List tables
# SELECT * FROM users;         -- View users
# SELECT * FROM request_logs;  -- View request logs
# \q                           -- Exit

# Check Docker services
docker compose ps
docker compose logs postgres
docker compose logs redis
```

### Local Testing Checklist

Before deploying to production, verify locally:

- [ ] Backend starts without errors (check logs)
- [ ] Database migrations run successfully (Flyway)
- [ ] Frontend dev server starts and shows UI
- [ ] Anonymous prompt submission works (1 request limit enforced)
- [ ] User registration works (email/password)
- [ ] User login works
- [ ] Authenticated prompt submission works (5 request limit enforced)
- [ ] Request history displays correctly
- [ ] Rate limiting works (try submitting >10 requests/minute)
- [ ] Backend health endpoint responds: `/actuator/health`
- [ ] No CORS errors in browser console
- [ ] Database has correct data (check with psql)

### Troubleshooting Local Setup

**Port already in use:**
```bash
# Check what's using the port
lsof -i :8080    # Backend port
lsof -i :5432    # PostgreSQL port
lsof -i :6379    # Redis port

# Change ports in .env.local or stop conflicting services
```

**Database connection refused:**
```bash
# Verify PostgreSQL container is running
docker compose ps postgres

# Check container logs
docker compose logs postgres

# Restart PostgreSQL
docker compose restart postgres
```

**Gemini API 401 Unauthorized:**
- Verify `GEMINI_API_KEY` in `.env.local` is correct
- Check key is enabled at https://console.cloud.google.com/
- Ensure "Generative Language API" is enabled

**Frontend can't reach backend:**
- Check `CORS_ALLOWED_ORIGINS` includes your frontend URL
- Verify backend is running on port 8080
- Check browser console for CORS errors

**Redis connection error (if using Redis profile):**
- Verify Redis container is running: `docker compose ps redis`
- Or switch to `no-redis` profile if not needed

**Schema validation errors:**
- Delete volumes and restart: `docker compose down -v && docker compose up -d`
- Check Flyway migrations ran: Look for "Successfully applied X migrations" in backend logs

### Cleaning Up

```bash
# Stop all services
docker compose down

# Remove volumes (WARNING: deletes all data)
docker compose down -v

# Remove .env.local (keeps your secrets safe)
rm .env.local

# Stop and remove everything
docker compose down -v
docker system prune -a
```

---

## Part 1: Initial Setup on Your Server

### 1. Install Docker and Docker Compose

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add your user to docker group (logout/login required after)
sudo usermod -aG docker $USER

# Install Docker Compose
sudo apt install docker-compose-plugin -y

# Verify installation
docker --version
docker compose version
```

### 2. Authenticate with GitHub Container Registry

```bash
# Create a GitHub Personal Access Token (PAT)
# Go to: GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
# Create token with these scopes:
#   - read:packages
#   - write:packages (if you need to push)

# Login to GHCR
echo "YOUR_GITHUB_PAT" | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin

# Verify login succeeded
cat ~/.docker/config.json
```

### 3. Clone Repository and Set Up Environment

```bash
# Clone your repository
git clone https://github.com/fresh-fx59/ai-port-checker-web-app.git
cd ai-port-checker-web-app

# Create production environment file
cp .env.example .env.production

# Edit with your production values
nano .env.production
```

### 4. Configure Environment Variables

Edit `.env.production` with your actual production values:

```bash
# Required: Replace these with your actual values
DATABASE_NAME=gemini_web_app
DATABASE_USERNAME=your_secure_username
DATABASE_PASSWORD=your_secure_password_here
REDIS_PASSWORD=your_redis_password_here

# Google OAuth credentials
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# Gemini API
GEMINI_API_KEY=your-gemini-api-key

# Security - IMPORTANT: Generate a strong random secret
JWT_SECRET=your-very-long-random-secret-at-least-256-bits

# CORS - Set to your frontend URL
CORS_ALLOWED_ORIGINS=http://your-domain.com,https://your-domain.com

# GitHub repository (format: username/repo)
GITHUB_REPOSITORY=YOUR_USERNAME/YOUR_REPO

# Frontend API URL
VITE_API_URL=http://your-domain.com:8080
```

**Security Note:** Generate strong passwords:
```bash
# Generate strong passwords
openssl rand -base64 32
```

### 5. Initial Deployment

```bash
# Pull images from GHCR (after first GitHub Actions run)
docker compose -f docker-compose.ghcr.yml --env-file .env.production pull

# Start all services
docker compose -f docker-compose.ghcr.yml --env-file .env.production up -d

# Check logs
docker compose -f docker-compose.ghcr.yml logs -f

# Check status
docker compose -f docker-compose.ghcr.yml ps
```

## Part 2: GitHub Actions Setup

### 1. Set Up GitHub Secrets

Before running the workflow, you need to add Docker Hub credentials to avoid rate limits:

**Create Docker Hub Access Token:**
1. Go to https://hub.docker.com/settings/security
2. Click "New Access Token"
3. Name it "github-actions" (or any name)
4. Copy the token (you won't see it again!)

**Add Secrets to GitHub Repository:**
1. Go to your GitHub repository
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add these two secrets:

   **Secret 1:**
   - Name: `DOCKERHUB_USERNAME`
   - Value: Your Docker Hub username

   **Secret 2:**
   - Name: `DOCKERHUB_TOKEN`
   - Value: The access token you just created

**Why this is needed:**
- Docker Hub limits unauthenticated pulls to 100 per 6 hours
- Authenticated users get 200 pulls per 6 hours (free tier)
- GitHub Actions pulls base images (postgres, redis, node, nginx) during builds
- Without authentication, you'll hit rate limits quickly

### 2. Enable GitHub Container Registry

The workflow is already configured in `.github/workflows/docker-build-push.yml`.

When you push to `main` branch, GitHub Actions will automatically:
1. **Log in to Docker Hub** (using your secrets to avoid rate limits)
2. **Build backend and frontend Docker images** (pulls base images from Docker Hub)
3. **Push to GitHub Container Registry (GHCR)** (stores your built images)
4. **Tag with `latest` and the git commit SHA**

### 3. Make Repository Packages Public (Optional)

By default, GHCR packages are private. To make them public:

1. Go to your GitHub repository
2. Click on "Packages" in the right sidebar
3. Click on each package (backend/frontend)
4. Click "Package settings"
5. Scroll to "Danger Zone" → "Change visibility" → "Public"

**Note:** If keeping packages private, ensure your server is authenticated with GHCR (see Part 1, step 2).

### 4. Trigger First Build

```bash
# Make a commit and push to main
git add .
git commit -m "Add GitHub Actions deployment"
git push origin main

# Monitor the workflow
# Go to: GitHub → Actions tab
```

## Part 3: Watchtower Configuration

Watchtower is automatically configured in `docker-compose.ghcr.yml` with these settings:

- **Poll Interval:** Every 5 minutes (300 seconds)
- **Cleanup:** Old images are automatically removed
- **Label-based:** Only updates containers with `com.centurylinklabs.watchtower.enable=true`
- **Rolling Restart:** Updates one container at a time to minimize downtime

### Watchtower Commands

```bash
# Check Watchtower logs
docker logs gemini-watchtower -f

# Force immediate check (without waiting for interval)
docker compose -f docker-compose.ghcr.yml restart watchtower

# Disable Watchtower temporarily
docker compose -f docker-compose.ghcr.yml stop watchtower

# Re-enable Watchtower
docker compose -f docker-compose.ghcr.yml start watchtower
```

## Part 4: Deployment Workflow

### Normal Deployment Process

1. **Develop locally** and test changes
2. **Commit and push** to `main` branch
3. **GitHub Actions** automatically builds and pushes new images (~5-10 minutes)
4. **Watchtower** detects new images within 5 minutes
5. **Automatic deployment** - Watchtower pulls and restarts containers
6. **Total time:** ~10-15 minutes from push to live

### Manual Deployment (if needed)

```bash
# On your server
cd /path/to/your/repo

# Pull latest images
docker compose -f docker-compose.ghcr.yml --env-file .env.production pull

# Restart services
docker compose -f docker-compose.ghcr.yml --env-file .env.production up -d

# Or restart specific service
docker compose -f docker-compose.ghcr.yml restart backend
```

## Part 5: Monitoring and Maintenance

### Check Application Health

```bash
# Check all container status
docker compose -f docker-compose.ghcr.yml ps

# Check backend health
curl http://localhost:8080/actuator/health

# View logs for all services
docker compose -f docker-compose.ghcr.yml logs -f

# View logs for specific service
docker compose -f docker-compose.ghcr.yml logs -f backend
docker compose -f docker-compose.ghcr.yml logs -f frontend
docker compose -f docker-compose.ghcr.yml logs -f watchtower
```

### Database Backup

```bash
# Backup PostgreSQL database
docker exec gemini-postgres pg_dump -U $DATABASE_USERNAME gemini_web_app > backup-$(date +%Y%m%d).sql

# Restore from backup
cat backup-20240101.sql | docker exec -i gemini-postgres psql -U $DATABASE_USERNAME gemini_web_app
```

### Update Environment Variables

```bash
# Edit environment file
nano .env.production

# Restart affected services
docker compose -f docker-compose.ghcr.yml --env-file .env.production up -d
```

### Clean Up Old Images

```bash
# Remove unused images (Watchtower does this automatically)
docker image prune -a

# Remove unused volumes
docker volume prune

# Full cleanup
docker system prune -a --volumes
```

## Part 6: Troubleshooting

### Images Not Updating

```bash
# Check Watchtower logs
docker logs gemini-watchtower

# Verify GHCR authentication
cat ~/.docker/config.json

# Re-authenticate if needed
echo "YOUR_GITHUB_PAT" | docker login ghcr.io -u YOUR_USERNAME --password-stdin

# Restart Watchtower
docker compose -f docker-compose.ghcr.yml restart watchtower
```

### Container Won't Start

```bash
# Check logs
docker compose -f docker-compose.ghcr.yml logs backend

# Verify environment variables
docker compose -f docker-compose.ghcr.yml config

# Check health
docker inspect gemini-backend --format='{{.State.Health.Status}}'
```

### Database Connection Issues

```bash
# Check PostgreSQL is running
docker compose -f docker-compose.ghcr.yml ps postgres

# Test database connection
docker exec gemini-postgres pg_isready -U $DATABASE_USERNAME

# Check backend can reach database
docker compose -f docker-compose.ghcr.yml logs backend | grep -i postgres
```

### GitHub Actions Failures

1. Check Actions tab in GitHub for error messages
2. Common issues:
   - Missing secrets or environment variables
   - Docker build errors (check Dockerfile syntax)
   - Test failures (fix code before pushing)

## Part 7: Security Best Practices

### 1. Use Strong Secrets

```bash
# Generate secure JWT secret
openssl rand -base64 64

# Generate secure database password
openssl rand -base64 32
```

### 2. Firewall Configuration

```bash
# Allow only necessary ports
sudo ufw allow 22/tcp      # SSH
sudo ufw allow 80/tcp      # HTTP
sudo ufw allow 443/tcp     # HTTPS
sudo ufw enable
```

### 3. Enable HTTPS with Let's Encrypt (Recommended)

```bash
# Install Certbot
sudo apt install certbot

# Get SSL certificate
sudo certbot certonly --standalone -d your-domain.com

# Update docker-compose to use certificates
# Add nginx reverse proxy for HTTPS termination
```

### 4. Rotate Secrets Regularly

- Change JWT_SECRET every 90 days
- Rotate database passwords quarterly
- Update GitHub PAT before expiration

## Part 8: Production Checklist

Before going live:

- [ ] Strong passwords for all services (database, Redis, JWT)
- [ ] HTTPS enabled with valid SSL certificate
- [ ] CORS configured for production domain only
- [ ] Firewall configured to block unnecessary ports
- [ ] Database backups automated
- [ ] Monitoring and alerting set up
- [ ] Environment variables reviewed and secured
- [ ] GitHub Container Registry authenticated on server
- [ ] Watchtower running and monitoring logs
- [ ] Health checks passing for all services
- [ ] Test deployment by pushing a small change

## Part 9: Scaling Considerations

### Horizontal Scaling (Multiple Servers)

To run on multiple servers:

1. Use external PostgreSQL (managed service or dedicated server)
2. Use external Redis (managed service)
3. Deploy app containers on multiple servers
4. Add load balancer in front (nginx, HAProxy, cloud LB)
5. Use shared storage for volumes if needed

### Vertical Scaling (Resource Limits)

Add resource limits to `docker-compose.ghcr.yml`:

```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

## Support

For issues:
- Check application logs: `docker compose -f docker-compose.ghcr.yml logs`
- Check Watchtower logs: `docker logs gemini-watchtower`
- Review GitHub Actions runs in repository
- Verify environment variables are correct


## Quick commands

```bash
ls -la
ss -tulpen

git clone https://github.com/fresh-fx59/ai-port-checker-web-app.git

git pull origin main

docker compose -f docker-compose.prod.yml --env-file .env.production build
docker compose -f docker-compose.prod.yml --env-file .env.production up -d --force-recreate

docker logs a749f3973247
docker start 6d96258288e3
docker stop a1b2ea28b4eb
docker rm a1b2ea28b4eb
docker ps
```