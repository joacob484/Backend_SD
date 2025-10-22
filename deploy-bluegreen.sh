#!/bin/bash
set -e

echo "🔵🟢 Starting Blue-Green Deployment"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
BLUE_PORT=8080
GREEN_PORT=8081
HEALTH_CHECK_URL="http://localhost:${GREEN_PORT}/actuator/health"
MAX_WAIT=180  # 3 minutes max wait for health check

echo -e "${BLUE}📥 Step 1: Pulling latest code${NC}"
git pull origin main

echo -e "${BLUE}📦 Step 2: Building and starting GREEN deployment (port ${GREEN_PORT})${NC}"
docker-compose -f docker-compose.bluegreen.yml up -d --build backend-green

echo -e "${YELLOW}⏳ Step 3: Waiting for GREEN to be healthy...${NC}"
SECONDS_WAITED=0
until curl -f ${HEALTH_CHECK_URL} > /dev/null 2>&1; do
    if [ ${SECONDS_WAITED} -ge ${MAX_WAIT} ]; then
        echo -e "${RED}❌ GREEN deployment failed to become healthy after ${MAX_WAIT}s${NC}"
        echo -e "${RED}🔙 Rolling back: stopping GREEN${NC}"
        docker-compose -f docker-compose.bluegreen.yml stop backend-green
        exit 1
    fi
    echo -e "${YELLOW}⏳ Waiting for backend-green... (${SECONDS_WAITED}s / ${MAX_WAIT}s)${NC}"
    sleep 5
    SECONDS_WAITED=$((SECONDS_WAITED + 5))
done

echo -e "${GREEN}✅ GREEN is healthy!${NC}"

echo -e "${BLUE}🔄 Step 4: Switching traffic from BLUE to GREEN${NC}"
# Update nginx or firewall rules here if using a load balancer
# For now, we'll just change the main compose file

echo -e "${BLUE}🛑 Step 5: Stopping BLUE deployment (port ${BLUE_PORT})${NC}"
docker-compose -f docker-compose.cloudsql.yml stop backend || true

echo -e "${BLUE}♻️  Step 6: Promoting GREEN to BLUE${NC}"
# Stop the green container
docker stop backend-green || true
# Remove old blue if exists
docker rm backend-1 || true
# Rename green to blue
docker rename backend-green backend-1 || true

# Update port mapping: remove green's 8081 mapping and map blue to 8080
docker stop backend-1 || true
docker rm backend-1 || true

# Restart with the main compose file
docker-compose -f docker-compose.cloudsql.yml up -d backend

echo -e "${GREEN}🧹 Step 7: Cleanup${NC}"
docker-compose -f docker-compose.bluegreen.yml down --remove-orphans || true

echo -e "${GREEN}✅ Blue-Green Deployment Complete!${NC}"
echo -e "${GREEN}🚀 Backend is now running on port ${BLUE_PORT}${NC}"

# Show status
docker ps | grep -E "backend|redis|rabbitmq"
