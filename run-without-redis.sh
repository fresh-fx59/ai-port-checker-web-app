#!/bin/bash

# Script to run the Gemini Web App without Redis

echo "🚀 Starting Gemini Web App without Redis..."
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Check if PostgreSQL is already running
if docker ps | grep -q "gemini-postgres"; then
    echo "✅ PostgreSQL container is already running"
else
    echo "🐘 Starting PostgreSQL container..."
    docker-compose -f docker-compose.no-redis.yml up -d postgres
    
    # Wait for PostgreSQL to be ready
    echo "⏳ Waiting for PostgreSQL to be ready..."
    until docker exec gemini-postgres pg_isready -U gemini_user -d gemini_web_app_dev > /dev/null 2>&1; do
        sleep 1
    done
    echo "✅ PostgreSQL is ready"
fi

echo ""
echo "🔧 Configuration:"
echo "   - Profile: no-redis"
echo "   - Database: PostgreSQL (Docker)"
echo "   - Session Management: In-memory"
echo "   - Cache: Disabled"
echo ""

# Check if we're in the backend directory
if [ -f "pom.xml" ]; then
    echo "🏃 Running Spring Boot application..."
    mvn spring-boot:run -Dspring-boot.run.profiles=no-redis
elif [ -f "backend/pom.xml" ]; then
    echo "🏃 Running Spring Boot application..."
    cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=no-redis
else
    echo "❌ Could not find pom.xml. Please run this script from the project root or backend directory."
    exit 1
fi