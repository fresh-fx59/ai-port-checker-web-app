#!/bin/bash

# Start Backend Script
# This script loads .env.local and starts the Spring Boot backend

set -e

# Add common Maven installation paths to PATH
export PATH="/Applications/apache-maven-3.9.6/bin:$PATH"
export PATH="/usr/local/bin:$PATH"
export PATH="$HOME/.sdkman/candidates/maven/current/bin:$PATH"

echo "🚀 Starting Gemini Web App Backend"
echo "==================================="
echo ""

# Check if .env.local exists
if [ ! -f .env.local ]; then
    echo "❌ Error: .env.local not found!"
    echo "Please create .env.local in the project root."
    exit 1
fi

# Load environment variables from .env.local
echo "📄 Loading environment variables from .env.local..."
export $(grep -v '^#' .env.local | grep -v '^$' | xargs)

# Check if Gemini API key is set
if [ -z "$GEMINI_API_KEY" ] || [ "$GEMINI_API_KEY" == "REPLACE_WITH_YOUR_ACTUAL_GEMINI_API_KEY" ]; then
    echo "❌ Error: GEMINI_API_KEY is not set!"
    echo "Please edit .env.local and add your Gemini API key."
    exit 1
fi

echo "✅ Environment variables loaded"
echo ""

# Ask which profile to use
echo "Which profile do you want to run?"
echo "  1) dev       - Development mode with Redis"
echo "  2) no-redis  - Development mode without Redis (simpler)"
read -p "Enter choice [1-2]: " -n 1 -r
echo ""

PROFILE=""
if [[ $REPLY == "1" ]]; then
    PROFILE="dev"
    echo "Starting with dev profile (requires Redis)..."
elif [[ $REPLY == "2" ]]; then
    PROFILE="no-redis"
    echo "Starting with no-redis profile..."
else
    echo "Invalid choice. Using dev profile by default..."
    PROFILE="dev"
fi

echo ""
echo "🔧 Starting backend with profile: $PROFILE"
echo ""

# Change to backend directory
cd backend

# Start Spring Boot
mvn spring-boot:run -Dspring-boot.run.profiles=$PROFILE
