@echo off
REM Start Backend Script (Windows)
REM This script loads .env.local and starts the Spring Boot backend

echo ========================================
echo Starting Gemini Web App Backend
echo ========================================
echo.

REM Check if .env.local exists
if not exist .env.local (
    echo [ERROR] .env.local not found!
    echo Please create .env.local in the project root.
    pause
    exit /b 1
)

echo [OK] Loading environment variables from .env.local...
echo.
echo NOTE: Windows batch files cannot easily load .env files.
echo You need to manually set environment variables or use PowerShell.
echo.
echo Quick option: Run this in PowerShell instead:
echo   Get-Content .env.local ^| ForEach-Object { if($_ -match '^([^=]+)=(.*)$'){ [Environment]::SetEnvironmentVariable($matches[1], $matches[2]) } }
echo   cd backend
echo   mvn spring-boot:run -Dspring-boot.run.profiles=dev
echo.

REM Ask which profile to use
set /p PROFILE="Which profile? (1=dev, 2=no-redis): "

if "%PROFILE%"=="1" (
    set SPRING_PROFILE=dev
    echo Starting with dev profile ^(requires Redis^)...
) else if "%PROFILE%"=="2" (
    set SPRING_PROFILE=no-redis
    echo Starting with no-redis profile...
) else (
    set SPRING_PROFILE=dev
    echo Invalid choice. Using dev profile by default...
)

echo.
echo Starting backend with profile: %SPRING_PROFILE%
echo.

cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=%SPRING_PROFILE%
