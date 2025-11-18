#!/bin/bash

# Server Setup Script for Gemini Web App
# This script helps set up the server for automatic deployments

set -e

echo "======================================"
echo "Gemini Web App - Server Setup"
echo "======================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if running as root
if [[ $EUID -eq 0 ]]; then
   echo -e "${RED}This script should NOT be run as root${NC}"
   echo "Please run as a regular user with sudo privileges"
   exit 1
fi

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Step 1: Check Docker installation
echo -e "${YELLOW}Step 1: Checking Docker installation...${NC}"
if command_exists docker; then
    echo -e "${GREEN}✓ Docker is already installed${NC}"
    docker --version
else
    echo -e "${YELLOW}Installing Docker...${NC}"
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker $USER
    echo -e "${GREEN}✓ Docker installed successfully${NC}"
    echo -e "${YELLOW}NOTE: You need to log out and log back in for group changes to take effect${NC}"
fi

echo ""

# Step 2: Check Docker Compose
echo -e "${YELLOW}Step 2: Checking Docker Compose...${NC}"
if command_exists docker-compose || docker compose version >/dev/null 2>&1; then
    echo -e "${GREEN}✓ Docker Compose is already installed${NC}"
    docker compose version 2>/dev/null || docker-compose --version
else
    echo -e "${YELLOW}Installing Docker Compose plugin...${NC}"
    sudo apt update
    sudo apt install -y docker-compose-plugin
    echo -e "${GREEN}✓ Docker Compose installed successfully${NC}"
fi

echo ""

# Step 3: GitHub Container Registry Authentication
echo -e "${YELLOW}Step 3: GitHub Container Registry Authentication${NC}"
echo ""
echo "You need a GitHub Personal Access Token (PAT) with 'read:packages' scope"
echo "Create one at: https://github.com/settings/tokens"
echo ""
read -p "Do you want to authenticate with GHCR now? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    read -p "Enter your GitHub username: " GITHUB_USERNAME
    read -sp "Enter your GitHub Personal Access Token: " GITHUB_PAT
    echo
    echo "$GITHUB_PAT" | docker login ghcr.io -u "$GITHUB_USERNAME" --password-stdin
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Successfully authenticated with GHCR${NC}"
    else
        echo -e "${RED}✗ Failed to authenticate with GHCR${NC}"
    fi
else
    echo -e "${YELLOW}Skipped GHCR authentication. You can do this later with:${NC}"
    echo "echo YOUR_PAT | docker login ghcr.io -u YOUR_USERNAME --password-stdin"
fi

echo ""

# Step 4: Environment Configuration
echo -e "${YELLOW}Step 4: Environment Configuration${NC}"
if [ -f .env.production ]; then
    echo -e "${YELLOW}.env.production already exists${NC}"
    read -p "Do you want to overwrite it? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Keeping existing .env.production"
    else
        cp .env.production.example .env.production
        echo -e "${GREEN}✓ Created .env.production from template${NC}"
        echo -e "${YELLOW}IMPORTANT: Edit .env.production with your actual values before deploying!${NC}"
    fi
else
    cp .env.production.example .env.production
    echo -e "${GREEN}✓ Created .env.production from template${NC}"
    echo -e "${YELLOW}IMPORTANT: Edit .env.production with your actual values before deploying!${NC}"
fi

echo ""

# Step 5: Generate Secure Secrets
echo -e "${YELLOW}Step 5: Generate Secure Secrets${NC}"
echo "Here are some randomly generated secure secrets you can use:"
echo ""
echo -e "${GREEN}Database Password:${NC}"
openssl rand -base64 32
echo ""
echo -e "${GREEN}Redis Password:${NC}"
openssl rand -base64 32
echo ""
echo -e "${GREEN}JWT Secret:${NC}"
openssl rand -base64 64
echo ""
echo -e "${YELLOW}Copy these to your .env.production file${NC}"

echo ""

# Step 6: Firewall Setup (optional)
echo -e "${YELLOW}Step 6: Firewall Setup (Optional)${NC}"
read -p "Do you want to configure UFW firewall? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if command_exists ufw; then
        sudo ufw allow 22/tcp comment 'SSH'
        sudo ufw allow 80/tcp comment 'HTTP'
        sudo ufw allow 443/tcp comment 'HTTPS'
        sudo ufw --force enable
        echo -e "${GREEN}✓ Firewall configured${NC}"
        sudo ufw status
    else
        echo -e "${YELLOW}UFW not installed. Install with: sudo apt install ufw${NC}"
    fi
else
    echo "Skipped firewall setup"
fi

echo ""
echo "======================================"
echo -e "${GREEN}Server Setup Complete!${NC}"
echo "======================================"
echo ""
echo "Next steps:"
echo "1. Edit .env.production with your actual configuration"
echo "2. Make sure GITHUB_REPOSITORY is set correctly in .env.production"
echo "3. Push code to main branch to trigger image build"
echo "4. Pull and start services:"
echo "   docker compose -f docker-compose.ghcr.yml --env-file .env.production pull"
echo "   docker compose -f docker-compose.ghcr.yml --env-file .env.production up -d"
echo ""
echo "For detailed deployment instructions, see DEPLOYMENT.md"
echo ""
