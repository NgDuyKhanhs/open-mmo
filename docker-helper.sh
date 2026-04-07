#!/bin/bash

# OpenMMO AI - Docker Helper Script
# Usage: ./docker-helper.sh [command]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Docker is installed
check_docker() {
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}❌ Docker is not installed!${NC}"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        echo -e "${RED}❌ Docker Compose is not installed!${NC}"
        exit 1
    fi

    echo -e "${GREEN}✅ Docker and Docker Compose are installed${NC}"
}

# Build images
build() {
    echo -e "${BLUE}🔨 Building Docker images...${NC}"
    docker-compose build
    echo -e "${GREEN}✅ Build complete!${NC}"
}

# Start services
start() {
    echo -e "${BLUE}🚀 Starting services...${NC}"
    docker-compose up -d

    echo -e "${GREEN}✅ Services started!${NC}"
    echo -e "${BLUE}📝 Service URLs:${NC}"
    echo "   Frontend: http://localhost"
    echo "   Backend: http://localhost:8080"
    echo "   MongoDB: localhost:27017"
}

# Stop services
stop() {
    echo -e "${BLUE}⛔ Stopping services...${NC}"
    docker-compose down
    echo -e "${GREEN}✅ Services stopped!${NC}"
}

# View logs
logs() {
    service=${1:-}
    if [ -z "$service" ]; then
        docker-compose logs -f
    else
        docker-compose logs -f "$service"
    fi
}

# Check status
status() {
    echo -e "${BLUE}📊 Service Status:${NC}"
    docker-compose ps
}

# Rebuild and restart
rebuild() {
    service=${1:-}
    if [ -z "$service" ]; then
        echo -e "${BLUE}🔄 Rebuilding all services...${NC}"
        docker-compose up -d --build
    else
        echo -e "${BLUE}🔄 Rebuilding $service...${NC}"
        docker-compose up -d --build "$service"
    fi
    echo -e "${GREEN}✅ Rebuild complete!${NC}"
}

# Clean up
clean() {
    echo -e "${YELLOW}⚠️  This will remove containers and volumes!${NC}"
    read -p "Continue? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose down -v
        echo -e "${GREEN}✅ Cleanup complete!${NC}"
    else
        echo -e "${YELLOW}⏸️  Cleanup cancelled${NC}"
    fi
}

# Database commands
db_clear() {
    echo -e "${YELLOW}⚠️  This will delete all Gmail connections!${NC}"
    read -p "Continue? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${BLUE}🗑️  Clearing Gmail connections...${NC}"
        curl -X POST http://localhost:8080/api/v1/admin/gmail/clear-all
        echo -e "\n${GREEN}✅ Gmail connections cleared!${NC}"
    fi
}

# Execute command in container
exec_backend() {
    docker-compose exec backend bash
}

exec_frontend() {
    docker-compose exec frontend sh
}

# Main menu
show_help() {
    cat << EOF
${BLUE}OpenMMO AI - Docker Helper${NC}

Usage: ./docker-helper.sh [command]

Commands:
  ${GREEN}check${NC}              Check Docker installation
  ${GREEN}build${NC}              Build Docker images
  ${GREEN}start${NC}              Start all services
  ${GREEN}stop${NC}               Stop all services
  ${GREEN}restart${NC}            Restart all services
  ${GREEN}rebuild [service]${NC}   Rebuild and restart service(s)
  ${GREEN}logs [service]${NC}      View service logs
  ${GREEN}status${NC}              Show service status
  ${GREEN}clean${NC}               Stop and remove all containers/volumes
  ${GREEN}db-clear${NC}            Clear all Gmail connections from database
  ${GREEN}exec:backend${NC}        Execute bash in backend container
  ${GREEN}exec:frontend${NC}       Execute sh in frontend container
  ${GREEN}help${NC}               Show this help message

Examples:
  ./docker-helper.sh build
  ./docker-helper.sh start
  ./docker-helper.sh logs backend
  ./docker-helper.sh rebuild frontend
  ./docker-helper.sh db-clear

EOF
}

# Main script
main() {
    case "${1:-help}" in
        check)
            check_docker
            ;;
        build)
            check_docker
            build
            ;;
        start)
            check_docker
            start
            ;;
        stop)
            stop
            ;;
        restart)
            stop
            start
            ;;
        rebuild)
            rebuild "${2:-}"
            ;;
        logs)
            logs "${2:-}"
            ;;
        status)
            status
            ;;
        clean)
            clean
            ;;
        db-clear)
            db_clear
            ;;
        exec:backend)
            exec_backend
            ;;
        exec:frontend)
            exec_frontend
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            echo -e "${RED}❌ Unknown command: $1${NC}"
            show_help
            exit 1
            ;;
    esac
}

main "$@"

