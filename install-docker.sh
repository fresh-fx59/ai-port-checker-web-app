#!/bin/bash

# Docker Installation Script for Ubuntu 24.04
# This script installs Docker Engine and Docker Compose on a fresh Ubuntu 24.04 server
# Fully automated - no human interaction required

set -e  # Exit on any error

# Set non-interactive mode for apt
export DEBIAN_FRONTEND=noninteractive

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_message() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check if running as root
if [[ $EUID -ne 0 ]]; then
   print_error "This script must be run as root (use sudo)"
   exit 1
fi

print_message "Starting Docker installation on Ubuntu 24.04..."

# Check if Docker is already installed
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version 2>/dev/null || echo "unknown")
    print_warning "Docker is already installed: $DOCKER_VERSION"
    print_message "Skipping installation, proceeding to verification..."

    # Jump to testing section
    print_message "Testing Docker installation..."
    if docker info > /dev/null 2>&1; then
        print_message "Docker test successful! Daemon is running."
    else
        print_warning "Docker daemon check failed. Attempting to start Docker service..."
        systemctl start docker 2>/dev/null || true
        sleep 2
        if docker info > /dev/null 2>&1; then
            print_message "Docker daemon started successfully!"
        else
            print_error "Docker daemon is not running. Please check: systemctl status docker"
            exit 1
        fi
    fi

    # Display Docker info
    print_message "Docker is ready to use!"
    echo ""
    echo "=========================================="
    echo "Docker Version Information:"
    echo "=========================================="
    docker --version
    docker compose version 2>/dev/null || echo "Docker Compose: Not installed"
    echo ""
    echo "=========================================="
    echo "Docker System Information:"
    echo "=========================================="
    docker info 2>/dev/null | grep -E "Server Version|Storage Driver|Logging Driver|Cgroup Driver|Kernel Version" || true
    echo ""
    print_message "Docker verification complete!"
    exit 0
fi

print_message "Docker not found. Proceeding with installation..."

# Update package index
print_message "Updating package index..."
apt-get update -qq

# Install prerequisites
print_message "Installing prerequisites..."
apt-get install -y -qq \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# Add Docker's official GPG key
print_message "Adding Docker's official GPG key..."
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /tmp/docker.gpg
gpg --dearmor --yes -o /etc/apt/keyrings/docker.gpg /tmp/docker.gpg 2>/dev/null || true
chmod a+r /etc/apt/keyrings/docker.gpg
rm -f /tmp/docker.gpg

# Set up Docker repository
print_message "Setting up Docker repository..."
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null

# Update package index with Docker packages
print_message "Updating package index with Docker packages..."
apt-get update -qq

# Install Docker Engine, CLI, containerd, and Docker Compose
print_message "Installing Docker Engine and Docker Compose..."
apt-get install -y -qq \
    docker-ce \
    docker-ce-cli \
    containerd.io \
    docker-buildx-plugin \
    docker-compose-plugin

# Start and enable Docker service
print_message "Starting and enabling Docker service..."
systemctl start docker
systemctl enable docker

# Verify Docker installation
print_message "Verifying Docker installation..."
docker --version
docker compose version

# Configure Docker daemon for production use
print_message "Configuring Docker daemon..."
mkdir -p /etc/docker

cat > /etc/docker/daemon.json <<EOF
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "live-restore": true,
  "userland-proxy": false,
  "ipv6": false
}
EOF

# Restart Docker to apply configuration
print_message "Restarting Docker service to apply configuration..."
systemctl restart docker

# Optional: Add current user to docker group (if not root)
if [ -n "$SUDO_USER" ]; then
    print_message "Adding user $SUDO_USER to docker group..."
    usermod -aG docker "$SUDO_USER" 2>/dev/null || true
    print_warning "User $SUDO_USER added to docker group. Log out and back in for changes to take effect."
fi

# Test Docker installation
print_message "Testing Docker installation..."
if docker info > /dev/null 2>&1; then
    print_message "Docker test successful! Daemon is running."
else
    print_warning "Docker daemon check failed, but Docker is installed. You may need to wait a moment for the daemon to start."
fi

# Display Docker info
print_message "Docker installation completed successfully!"
echo ""
echo "=========================================="
echo "Docker Version Information:"
echo "=========================================="
docker --version
docker compose version
echo ""
echo "=========================================="
echo "Docker System Information:"
echo "=========================================="
docker info | grep -E "Server Version|Storage Driver|Logging Driver|Cgroup Driver|Kernel Version"
echo ""
print_message "Installation complete! Docker is ready to use."
if [ -n "$SUDO_USER" ]; then
    print_warning "Remember: User $SUDO_USER needs to log out and back in to use Docker without sudo."
fi