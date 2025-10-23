#!/bin/bash
set -e

echo "� Starting Backend Deployment"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
HEALTH_CHECK_URL="http://localhost:8080/actuator/health/readiness"
MAX_WAIT=180  # 3 minutes max wait for health check

echo -e "${BLUE}📥 Step 1: Pulling latest code${NC}"
git pull origin main

echo -e "${BLUE}📦 Step 2: Rebuilding backend image${NC}"
docker-compose -f docker-compose.prod.yml build backend

echo -e "${BLUE}🔄 Step 3: Restarting backend (keeps Redis/RabbitMQ/Postgres running)${NC}"
docker-compose -f docker-compose.prod.yml up -d backend

echo -e "${YELLOW}⏳ Step 4: Waiting for backend to be healthy...${NC}"
SECONDS_WAITED=0
until curl -f ${HEALTH_CHECK_URL} > /dev/null 2>&1; do
    if [ ${SECONDS_WAITED} -ge ${MAX_WAIT} ]; then
        echo -e "${RED}❌ Backend deployment failed to become healthy after ${MAX_WAIT}s${NC}"
        echo -e "${RED}� Last 50 lines of logs:${NC}"
        docker-compose -f docker-compose.prod.yml logs --tail=50 backend
        exit 1
    fi
    echo -e "${YELLOW}⏳ Waiting for backend... (${SECONDS_WAITED}s / ${MAX_WAIT}s)${NC}"
    sleep 5
    SECONDS_WAITED=$((SECONDS_WAITED + 5))
done

echo -e "${GREEN}✅ Backend is healthy!${NC}"

echo -e "${GREEN}🧹 Step 5: Cleanup old containers${NC}"
docker system prune -f || true

echo -e "${GREEN}✅ Deployment Complete!${NC}"
echo -e "${GREEN}🚀 Backend is running on port 8080${NC}"

# Show status
echo -e "${BLUE}📊 Current services:${NC}"
docker-compose -f docker-compose.prod.yml ps
