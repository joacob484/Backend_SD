#!/bin/bash

# ═══════════════════════════════════════════════════════════
# Maven Local Build Test
# Prueba el build de Maven localmente sin Docker
# ═══════════════════════════════════════════════════════════

set -e

echo "🔨 Testing Maven Build Configuration..."
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check Java version
echo -e "${BLUE}📌 Checking Java version...${NC}"
if command -v java &> /dev/null; then
    java -version 2>&1 | head -1
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        echo -e "${GREEN}✓${NC} Java $JAVA_VERSION detected (>=21 required)"
    else
        echo -e "${RED}✗${NC} Java $JAVA_VERSION detected (21 required)"
        echo -e "${YELLOW}Please install Java 21 or higher${NC}"
        exit 1
    fi
else
    echo -e "${RED}✗${NC} Java not found"
    echo -e "${YELLOW}Please install Java 21 or higher${NC}"
    exit 1
fi
echo ""

# Check Maven
echo -e "${BLUE}📌 Checking Maven...${NC}"
if command -v mvn &> /dev/null; then
    mvn --version | head -1
    echo -e "${GREEN}✓${NC} Maven detected"
else
    echo -e "${YELLOW}⚠${NC} Maven not found in PATH"
    echo -e "${YELLOW}Will use Maven from Docker during Docker builds${NC}"
fi
echo ""

# Clean previous builds
echo -e "${BLUE}🧹 Cleaning previous builds...${NC}"
if [ -d "target" ]; then
    rm -rf target
    echo -e "${GREEN}✓${NC} Cleaned target directory"
fi
echo ""

# Test Maven configuration
echo -e "${BLUE}🔍 Testing Maven configuration...${NC}"
if [ -f ".mvn/maven.config" ]; then
    echo -e "${GREEN}✓${NC} Found .mvn/maven.config"
    cat .mvn/maven.config
else
    echo -e "${YELLOW}⚠${NC} .mvn/maven.config not found"
fi
echo ""

# Build project
echo -e "${BLUE}🚀 Building project...${NC}"
echo ""

if command -v mvn &> /dev/null; then
    echo -e "${YELLOW}Running: mvn clean package -DskipTests${NC}"
    echo ""
    
    # Run Maven build
    if mvn clean package -DskipTests; then
        echo ""
        echo -e "${GREEN}✅ BUILD SUCCESSFUL!${NC}"
        echo ""
        
        # Show JAR info
        if [ -f target/*.jar ]; then
            echo -e "${BLUE}📦 Generated JAR:${NC}"
            ls -lh target/*.jar
            echo ""
            
            JAR_FILE=$(ls target/*.jar | head -1)
            echo -e "${BLUE}📋 JAR Contents (first 20 entries):${NC}"
            jar tf "$JAR_FILE" | head -20
            echo ""
            
            echo -e "${GREEN}✨ Maven build completed successfully!${NC}"
            echo ""
            echo -e "${BLUE}Next steps:${NC}"
            echo "1. Test with Docker:"
            echo -e "   ${YELLOW}docker build -f Dockerfile.cloudrun -t faltauno-backend:test .${NC}"
            echo ""
            echo "2. Run locally:"
            echo -e "   ${YELLOW}java -jar target/*.jar${NC}"
            echo ""
        else
            echo -e "${RED}✗${NC} JAR file not found in target/"
            exit 1
        fi
    else
        echo ""
        echo -e "${RED}❌ BUILD FAILED!${NC}"
        echo ""
        echo -e "${YELLOW}Check the errors above and fix them.${NC}"
        echo ""
        echo -e "${BLUE}Common issues:${NC}"
        echo "1. Java version < 21"
        echo "2. Missing dependencies in pom.xml"
        echo "3. Compilation errors in source code"
        echo "4. MapStruct/Lombok annotation processing errors"
        echo ""
        exit 1
    fi
else
    echo -e "${YELLOW}⚠${NC} Maven not found locally"
    echo -e "${YELLOW}You can still build with Docker:${NC}"
    echo ""
    echo -e "   ${BLUE}docker build -f Dockerfile.cloudrun -t faltauno-backend:test .${NC}"
    echo ""
fi
