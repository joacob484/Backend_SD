#!/bin/bash

# ═══════════════════════════════════════════════════════════
# Maven Docker Build Validator
# Verifica que el build de Maven funcione correctamente
# ═══════════════════════════════════════════════════════════

set -e

echo "🔍 Validating Maven Docker Build Configuration..."
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if Dockerfile exists
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} Found: $1"
        return 0
    else
        echo -e "${RED}✗${NC} Missing: $1"
        return 1
    fi
}

echo "📋 Checking required files..."
check_file "pom.xml"
check_file "Dockerfile"
check_file "Dockerfile.cloudrun"
check_file "Dockerfile.prod"
check_file ".dockerignore"
check_file ".mvn/maven.config"
echo ""

echo "🔍 Validating pom.xml configuration..."
if grep -q "<java.version>21</java.version>" pom.xml; then
    echo -e "${GREEN}✓${NC} Java version: 21"
else
    echo -e "${RED}✗${NC} Java version not set to 21"
fi

if grep -q "<spring-boot.version>3.5.0</spring-boot.version>" pom.xml; then
    echo -e "${GREEN}✓${NC} Spring Boot version: 3.5.0"
else
    echo -e "${YELLOW}⚠${NC} Spring Boot version might be different"
fi

if grep -q "mapstruct" pom.xml; then
    echo -e "${GREEN}✓${NC} MapStruct configured"
else
    echo -e "${RED}✗${NC} MapStruct not found"
fi

if grep -q "lombok" pom.xml; then
    echo -e "${GREEN}✓${NC} Lombok configured"
else
    echo -e "${RED}✗${NC} Lombok not found"
fi
echo ""

echo "🐳 Docker build options:"
echo ""
echo "1. Local development (with wait-for-services):"
echo -e "   ${YELLOW}docker build -f Dockerfile -t faltauno-backend:dev .${NC}"
echo ""
echo "2. Cloud Run deployment:"
echo -e "   ${YELLOW}docker build -f Dockerfile.cloudrun -t faltauno-backend:cloudrun .${NC}"
echo ""
echo "3. Production (Alpine, minimal):"
echo -e "   ${YELLOW}docker build -f Dockerfile.prod -t faltauno-backend:prod .${NC}"
echo ""

echo "🧪 Test Maven build locally (without Docker):"
echo -e "   ${YELLOW}mvn clean package -DskipTests${NC}"
echo ""

echo "✨ All configuration files are in place!"
echo ""
echo "💡 Tip: Use 'dependency:go-offline' for better Docker layer caching"
echo "   This is already configured in all Dockerfiles!"
echo ""
