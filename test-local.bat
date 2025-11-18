@echo off
REM Local Testing Script for Gemini Web App (Windows)
REM This script helps you quickly set up and test the application locally

echo ========================================
echo Gemini Web App - Local Testing Setup
echo ========================================
echo.

REM Check if .env.local exists
if not exist .env.local (
    echo [ERROR] .env.local not found!
    echo The .env.local file should be in the project root.
    echo Please make sure you're running this script from the project root directory.
    pause
    exit /b 1
)

echo [OK] Loading environment variables from .env.local...
REM Note: Windows batch doesn't easily load .env files
REM Users should set environment variables manually or use PowerShell

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

echo [OK] Docker is running
echo.

REM Ask user if they want Redis
set /p USE_REDIS="Do you want to run with Redis? [y/n]: "

echo.
echo Starting database services...
if /i "%USE_REDIS%"=="y" (
    echo Starting PostgreSQL and Redis...
    docker compose up -d postgres redis
    set PROFILE=dev
) else (
    echo Starting PostgreSQL only...
    docker compose -f docker-compose.no-redis.yml up -d postgres
    set PROFILE=no-redis
)

echo.
echo Waiting for database to be ready...
timeout /t 5 /nobreak >nul

REM Check database health
docker exec gemini-postgres pg_isready -U gemini_user >nul 2>&1
if errorlevel 1 (
    echo [WARNING] Database might not be fully ready yet. Waiting a bit longer...
    timeout /t 5 /nobreak >nul
) else (
    echo [OK] Database is ready
)

echo.
echo Checking if backend dependencies need to be installed...
if not exist "backend\target" (
    echo Running: mvn clean install ^(this may take a few minutes^)...
    cd backend
    call mvn clean install -DskipTests
    cd ..
    echo [OK] Backend dependencies installed
) else (
    echo [OK] Backend dependencies already installed
)

echo.
echo Checking if frontend dependencies need to be installed...
if not exist "frontend\node_modules" (
    echo Running: npm install...
    cd frontend
    call npm install
    cd ..
    echo [OK] Frontend dependencies installed
) else (
    echo [OK] Frontend dependencies already installed
)

echo.
echo ========================================
echo Setup Complete!
echo ========================================
echo.
echo Next steps:
echo.
echo 1. Start the backend ^(in a new terminal^):
echo    cd backend
echo    mvn spring-boot:run -Dspring-boot.run.profiles=%PROFILE%
echo.
echo 2. Start the frontend ^(in another terminal^):
echo    cd frontend
echo    npm run dev
echo.
echo 3. Open your browser:
echo    http://localhost:5173 ^(or http://localhost:3000^)
echo.
echo 4. Check backend health:
echo    curl http://localhost:8080/actuator/health
echo.
echo Tips:
echo    - View database: docker exec -it gemini-postgres psql -U gemini_user -d gemini_web_app_dev
echo    - Stop services: docker compose down
echo.
pause
