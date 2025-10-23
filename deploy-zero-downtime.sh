#!/bin/bash
set -e

echo "🔵🟢 Starting Zero-Downtime Blue-Green Deployment"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
HEALTH_CHECK_URL="http://localhost:8081/actuator/health/readiness"
MAX_WAIT=180  # 3 minutes max wait for health check

echo -e "${BLUE}📥 Step 1: Pulling latest code${NC}"
git pull origin main

echo -e "${BLUE}📦 Step 2: Building new GREEN image${NC}"
# Build and tag the image
docker-compose -f docker-compose.bluegreen.yml build backend-green
# Tag it so we can reuse it later
docker tag $(docker images --format "{{.Repository}}:{{.Tag}}" | grep backend_sd-backend-green | head -1) backend_sd-backend:latest

echo -e "${BLUE}🟢 Step 3: Starting GREEN deployment on port 8081${NC}"
echo "Current BLUE is still serving traffic on port 8080"
docker-compose -f docker-compose.bluegreen.yml up -d backend-green

echo -e "${YELLOW}⏳ Step 4: Waiting for GREEN to be healthy...${NC}"
SECONDS_WAITED=0
until curl -f ${HEALTH_CHECK_URL} > /dev/null 2>&1; do
    if [ ${SECONDS_WAITED} -ge ${MAX_WAIT} ]; then
        echo -e "${RED}❌ GREEN deployment failed to become healthy after ${MAX_WAIT}s${NC}"
        echo -e "${RED}📋 Last 50 lines of GREEN logs:${NC}"
        docker logs backend-green --tail=50
        echo -e "${RED}🔙 Rolling back: stopping GREEN${NC}"
        docker-compose -f docker-compose.bluegreen.yml stop backend-green
        docker-compose -f docker-compose.bluegreen.yml rm -f backend-green
        echo -e "${YELLOW}✅ BLUE is still running - no downtime occurred${NC}"
        exit 1
    fi
    echo -e "${YELLOW}⏳ Waiting for GREEN... (${SECONDS_WAITED}s / ${MAX_WAIT}s)${NC}"
    sleep 5
    SECONDS_WAITED=$((SECONDS_WAITED + 5))
done

echo -e "${GREEN}✅ GREEN is healthy!${NC}"

echo -e "${BLUE}🔄 Step 5: Switching traffic from BLUE (8080) to GREEN${NC}"

# Stop the old BLUE container (from docker-compose.prod.yml)
echo -e "${YELLOW}Stopping old BLUE container...${NC}"
docker-compose -f docker-compose.prod.yml stop backend 2>/dev/null || true

# Now remap GREEN from 8081 to 8080
echo -e "${YELLOW}Remapping GREEN to port 8080...${NC}"
docker stop backend-green

# Remove the GREEN container but keep the image
docker rm backend-green

# Start GREEN on port 8080 using docker-compose.prod.yml
# This will use the same image we just built
docker-compose -f docker-compose.prod.yml up -d backend

# Wait a moment for it to start
sleep 5

# Verify the new container is healthy
echo -e "${YELLOW}⏳ Verifying new backend on port 8080...${NC}"
SECONDS_WAITED=0
MAX_WAIT_FINAL=120  # 2 minutes for final verification
until curl -f http://localhost:8080/actuator/health/readiness > /dev/null 2>&1; do
    if [ ${SECONDS_WAITED} -ge ${MAX_WAIT_FINAL} ]; then
        echo -e "${RED}❌ Failed to verify backend on port 8080 after ${MAX_WAIT_FINAL}s${NC}"
        echo -e "${RED}📋 Last 50 lines of logs:${NC}"
        docker logs faltauno_backend --tail=50
        exit 1
    fi
    echo -e "${YELLOW}⏳ Waiting... (${SECONDS_WAITED}s / ${MAX_WAIT_FINAL}s)${NC}"
    sleep 5
    SECONDS_WAITED=$((SECONDS_WAITED + 5))
done

echo -e "${GREEN}✅ Traffic successfully switched to new version!${NC}"

echo -e "${GREEN}🧹 Step 6: Cleanup${NC}"
# Clean up any orphaned containers from bluegreen
docker-compose -f docker-compose.bluegreen.yml down 2>/dev/null || true
docker system prune -f || true

echo -e "${GREEN}✅ Zero-Downtime Deployment Complete!${NC}"
echo -e "${GREEN}🚀 Backend is running on port 8080 with the latest code${NC}"

# Show final status
echo -e "${BLUE}📊 Current services:${NC}"
docker-compose -f docker-compose.prod.yml ps
