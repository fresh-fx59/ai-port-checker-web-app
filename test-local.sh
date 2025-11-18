#!/bin/bash

# Local Testing Script for Gemini Web App
# This script helps you quickly set up and test the application locally

set -e  # Exit on error

echo "🚀 Gemini Web App - Local Testing Setup"
echo "========================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if .env.local exists
if [ ! -f .env.local ]; then
    echo -e "${RED}❌ .env.local not found!${NC}"
    echo "The .env.local file should be in the project root."
    echo "Please make sure you're running this script from the project root directory."
    exit 1
fi

# Load environment variables
echo "📄 Loading environment variables from .env.local..."
export $(grep -v '^#' .env.local | xargs)

# Check for required variables
if [ -z "$GEMINI_API_KEY" ] || [ "$GEMINI_API_KEY" == "your-gemini-api-key-from-google-cloud" ]; then
    echo -e "${RED}❌ GEMINI_API_KEY is not set or still has default value!${NC}"
    echo "Please edit .env.local and add your Gemini API key."
    exit 1
fi

echo -e "${GREEN}✅ Environment variables loaded${NC}"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running!${NC}"
    echo "Please start Docker and try again."
    exit 1
fi

echo -e "${GREEN}✅ Docker is running${NC}"
echo ""

# Ask user if they want Redis
echo "Do you want to run with Redis? (recommended)"
echo "  y) Yes - Use Redis for caching and sessions"
echo "  n) No  - Use in-memory alternatives (simpler setup)"
read -p "Your choice [y/n]: " -n 1 -r
echo ""
USE_REDIS=$REPLY

# Start database services
echo ""
echo "🐳 Starting database services..."
if [[ $USE_REDIS =~ ^[Yy]$ ]]; then
    echo "Starting PostgreSQL and Redis..."
    docker compose up -d postgres redis
    PROFILE="dev"
else
    echo "Starting PostgreSQL only..."
    docker compose -f docker-compose.no-redis.yml up -d postgres
    PROFILE="no-redis"
fi

# Wait for database to be ready
echo ""
echo "⏳ Waiting for database to be ready..."
sleep 5

# Check database health
if docker exec gemini-postgres pg_isready -U gemini_user > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Database is ready${NC}"
else
    echo -e "${YELLOW}⚠️  Database might not be fully ready yet. Waiting a bit longer...${NC}"
    sleep 5
fi

echo ""
echo "📦 Checking if backend dependencies need to be installed..."
if [ ! -d "backend/target" ]; then
    echo "Running: mvn clean install (this may take a few minutes on first run)..."
    cd backend
    mvn clean install -DskipTests
    cd ..
    echo -e "${GREEN}✅ Backend dependencies installed${NC}"
else
    echo -e "${GREEN}✅ Backend dependencies already installed${NC}"
fi

echo ""
echo "📦 Checking if frontend dependencies need to be installed..."
if [ ! -d "frontend/node_modules" ]; then
    echo "Running: npm install..."
    cd frontend
    npm install
    cd ..
    echo -e "${GREEN}✅ Frontend dependencies installed${NC}"
else
    echo -e "${GREEN}✅ Frontend dependencies already installed${NC}"
fi

echo ""
echo "========================================"
echo -e "${GREEN}✨ Setup Complete!${NC}"
echo "========================================"
echo ""
echo "Next steps:"
echo ""
echo "1️⃣  Start the backend (in a new terminal):"
echo -e "   ${BLUE}cd backend${NC}"
echo -e "   ${BLUE}mvn spring-boot:run -Dspring-boot.run.profiles=$PROFILE${NC}"
echo ""
echo "2️⃣  Start the frontend (in another terminal):"
echo -e "   ${BLUE}cd frontend${NC}"
echo -e "   ${BLUE}npm run dev${NC}"
echo ""
echo "3️⃣  Open your browser:"
echo -e "   ${BLUE}http://localhost:5173${NC} (or http://localhost:3000)"
echo ""
echo "4️⃣  Check backend health:"
echo -e "   ${BLUE}curl http://localhost:8080/actuator/health${NC}"
echo ""
echo "💡 Tips:"
echo "   - View database: docker exec -it gemini-postgres psql -U gemini_user -d gemini_web_app_dev"
echo "   - View logs: docker compose logs -f postgres"
echo "   - Stop services: docker compose down"
echo ""
echo "🐛 Troubleshooting:"
echo "   - If port 8080 is busy: Change SERVER_PORT in .env.local"
echo "   - If port 5432 is busy: Stop other PostgreSQL instances"
echo "   - Backend logs: Check terminal where mvn is running"
echo ""
