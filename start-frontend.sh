#!/bin/bash

# Start Frontend Script
# This script starts the React frontend development server

set -e

echo "🎨 Starting Gemini Web App Frontend"
echo "===================================="
echo ""

# Check if node_modules exists
if [ ! -d "frontend/node_modules" ]; then
    echo "📦 Installing frontend dependencies (first time setup)..."
    cd frontend
    npm install
    cd ..
    echo "✅ Dependencies installed"
    echo ""
fi

echo "🚀 Starting frontend dev server..."
echo ""
echo "Frontend will be available at:"
echo "  http://localhost:5173"
echo "  (or http://localhost:3000 if 5173 is busy)"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

cd frontend
npm run dev
