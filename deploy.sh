#!/bin/bash

# Deployment Management Script for Gemini Web App
# Quick commands for managing the production deployment

set -e

COMPOSE_FILE="docker-compose.ghcr.yml"
ENV_FILE=".env.production"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Check if .env.production exists
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}Error: $ENV_FILE not found!${NC}"
    echo "Run ./server-setup.sh first or copy .env.production.example to .env.production"
    exit 1
fi

# Function to show usage
show_usage() {
    echo -e "${BLUE}Gemini Web App - Deployment Manager${NC}"
    echo ""
    echo "Usage: ./deploy.sh [command]"
    echo ""
    echo "Commands:"
    echo "  start       - Start all services"
    echo "  stop        - Stop all services"
    echo "  restart     - Restart all services"
    echo "  status      - Show status of all services"
    echo "  logs        - Show logs (all services)"
    echo "  logs-f      - Follow logs (all services)"
    echo "  pull        - Pull latest images from GHCR"
    echo "  update      - Pull latest images and restart"
    echo "  health      - Check health of backend service"
    echo "  backup-db   - Backup PostgreSQL database"
    echo "  clean       - Remove unused Docker images/volumes"
    echo "  watchtower  - Show Watchtower logs"
    echo ""
    echo "Service-specific logs:"
    echo "  logs-backend   - Show backend logs"
    echo "  logs-frontend  - Show frontend logs"
    echo "  logs-db        - Show database logs"
    echo "  logs-redis     - Show Redis logs"
    echo ""
}

# Function to run docker compose
dc() {
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" "$@"
}

# Main command handling
case "${1:-}" in
    start)
        echo -e "${GREEN}Starting all services...${NC}"
        dc up -d
        echo -e "${GREEN}✓ Services started${NC}"
        dc ps
        ;;

    stop)
        echo -e "${YELLOW}Stopping all services...${NC}"
        dc down
        echo -e "${GREEN}✓ Services stopped${NC}"
        ;;

    restart)
        echo -e "${YELLOW}Restarting all services...${NC}"
        dc restart
        echo -e "${GREEN}✓ Services restarted${NC}"
        dc ps
        ;;

    status)
        echo -e "${BLUE}Service Status:${NC}"
        dc ps
        ;;

    logs)
        dc logs
        ;;

    logs-f)
        echo -e "${BLUE}Following logs (Ctrl+C to exit)...${NC}"
        dc logs -f
        ;;

    logs-backend)
        dc logs backend
        ;;

    logs-frontend)
        dc logs frontend
        ;;

    logs-db)
        dc logs postgres
        ;;

    logs-redis)
        dc logs redis
        ;;

    pull)
        echo -e "${BLUE}Pulling latest images from GHCR...${NC}"
        dc pull
        echo -e "${GREEN}✓ Images pulled${NC}"
        ;;

    update)
        echo -e "${BLUE}Updating to latest version...${NC}"
        dc pull
        dc up -d
        echo -e "${GREEN}✓ Update complete${NC}"
        dc ps
        ;;

    health)
        echo -e "${BLUE}Checking backend health...${NC}"
        curl -f http://localhost:8080/actuator/health | python3 -m json.tool || echo -e "${RED}Backend is not healthy${NC}"
        ;;

    backup-db)
        BACKUP_FILE="backup-$(date +%Y%m%d-%H%M%S).sql"
        echo -e "${BLUE}Backing up database to $BACKUP_FILE...${NC}"

        # Source the env file to get database credentials
        set -a
        source "$ENV_FILE"
        set +a

        docker exec gemini-postgres pg_dump -U "$DATABASE_USERNAME" "$DATABASE_NAME" > "$BACKUP_FILE"
        echo -e "${GREEN}✓ Database backed up to $BACKUP_FILE${NC}"
        ;;

    clean)
        echo -e "${YELLOW}Cleaning up Docker resources...${NC}"
        docker image prune -a -f
        docker volume prune -f
        echo -e "${GREEN}✓ Cleanup complete${NC}"
        ;;

    watchtower)
        echo -e "${BLUE}Watchtower logs:${NC}"
        docker logs gemini-watchtower --tail 100 -f
        ;;

    *)
        show_usage
        exit 0
        ;;
esac
