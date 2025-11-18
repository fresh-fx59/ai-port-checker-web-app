@echo off
REM Start Frontend Script (Windows)
REM This script starts the React frontend development server

echo ========================================
echo Starting Gemini Web App Frontend
echo ========================================
echo.

REM Check if node_modules exists
if not exist "frontend\node_modules" (
    echo [INFO] Installing frontend dependencies ^(first time setup^)...
    cd frontend
    call npm install
    cd ..
    echo [OK] Dependencies installed
    echo.
)

echo [OK] Starting frontend dev server...
echo.
echo Frontend will be available at:
echo   http://localhost:5173
echo   ^(or http://localhost:3000 if 5173 is busy^)
echo.
echo Press Ctrl+C to stop the server
echo.

cd frontend
call npm run dev
