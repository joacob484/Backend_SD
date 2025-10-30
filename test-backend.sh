#!/bin/bash
# Script para probar el backend después del deployment

BACKEND_URL="https://faltauno-backend-169771742214.us-central1.run.app"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║           🧪 PRUEBAS DEL BACKEND - FALTA UNO                       ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo ""
echo -e "${BLUE}🌐 Backend URL: $BACKEND_URL${NC}"
echo ""

# Test 1: Health Check
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1️⃣  Health Check (/actuator/health)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
HEALTH_CODE=$(curl -s -o /tmp/health.json -w "%{http_code}" "$BACKEND_URL/actuator/health")
if [ "$HEALTH_CODE" = "200" ]; then
    echo -e "${GREEN}✅ Status: OK (HTTP $HEALTH_CODE)${NC}"
    echo ""
    cat /tmp/health.json | jq '.' 2>/dev/null || cat /tmp/health.json
else
    echo -e "${RED}❌ Status: FAILED (HTTP $HEALTH_CODE)${NC}"
    cat /tmp/health.json 2>/dev/null
fi
echo ""

# Test 2: Readiness Check
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "2️⃣  Readiness Check (/actuator/health/readiness)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
READINESS_CODE=$(curl -s -o /tmp/readiness.json -w "%{http_code}" "$BACKEND_URL/actuator/health/readiness")
if [ "$READINESS_CODE" = "200" ]; then
    echo -e "${GREEN}✅ Status: READY (HTTP $READINESS_CODE)${NC}"
    echo ""
    cat /tmp/readiness.json | jq '.' 2>/dev/null || cat /tmp/readiness.json
else
    echo -e "${RED}❌ Status: NOT READY (HTTP $READINESS_CODE)${NC}"
    cat /tmp/readiness.json 2>/dev/null
fi
echo ""

# Test 3: Liveness Check
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "3️⃣  Liveness Check (/actuator/health/liveness)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
LIVENESS_CODE=$(curl -s -o /tmp/liveness.json -w "%{http_code}" "$BACKEND_URL/actuator/health/liveness")
if [ "$LIVENESS_CODE" = "200" ]; then
    echo -e "${GREEN}✅ Status: ALIVE (HTTP $LIVENESS_CODE)${NC}"
    echo ""
    cat /tmp/liveness.json | jq '.' 2>/dev/null || cat /tmp/liveness.json
else
    echo -e "${RED}❌ Status: NOT ALIVE (HTTP $LIVENESS_CODE)${NC}"
    cat /tmp/liveness.json 2>/dev/null
fi
echo ""

# Test 4: API Root
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "4️⃣  API Root (/api)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
API_CODE=$(curl -s -o /tmp/api.json -w "%{http_code}" "$BACKEND_URL/api")
if [ "$API_CODE" = "200" ] || [ "$API_CODE" = "401" ] || [ "$API_CODE" = "403" ]; then
    echo -e "${GREEN}✅ Status: Accessible (HTTP $API_CODE)${NC}"
    if [ "$API_CODE" = "401" ] || [ "$API_CODE" = "403" ]; then
        echo -e "${YELLOW}ℹ️  Expected: API protegida requiere autenticación${NC}"
    fi
else
    echo -e "${RED}❌ Status: Error (HTTP $API_CODE)${NC}"
fi
echo ""

# Test 5: Partidos endpoint (público?)
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "5️⃣  Partidos endpoint (/api/partidos)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
PARTIDOS_CODE=$(curl -s -o /tmp/partidos.json -w "%{http_code}" "$BACKEND_URL/api/partidos")
if [ "$PARTIDOS_CODE" = "200" ]; then
    echo -e "${GREEN}✅ Status: OK (HTTP $PARTIDOS_CODE)${NC}"
    echo ""
    cat /tmp/partidos.json | jq '.data | length' 2>/dev/null && echo " partidos encontrados"
elif [ "$PARTIDOS_CODE" = "401" ] || [ "$PARTIDOS_CODE" = "403" ]; then
    echo -e "${YELLOW}⚠️  Status: Requiere autenticación (HTTP $PARTIDOS_CODE)${NC}"
else
    echo -e "${RED}❌ Status: Error (HTTP $PARTIDOS_CODE)${NC}"
fi
echo ""

# Test 6: CORS Check
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "6️⃣  CORS Check"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
CORS=$(curl -s -I -X OPTIONS "$BACKEND_URL/api/partidos" \
  -H "Origin: https://faltauno-frontend-169771742214.us-central1.run.app" \
  -H "Access-Control-Request-Method: GET" | grep -i "access-control-allow-origin")

if [ ! -z "$CORS" ]; then
    echo -e "${GREEN}✅ CORS configurado${NC}"
    echo "$CORS"
else
    echo -e "${YELLOW}⚠️  CORS no detectado en respuesta${NC}"
fi
echo ""

# Test 7: Redis Check (via health)
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "7️⃣  Redis Connection (via health check)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
REDIS_STATUS=$(cat /tmp/health.json | jq -r '.components.redis.status' 2>/dev/null)
if [ "$REDIS_STATUS" = "UP" ]; then
    echo -e "${GREEN}✅ Redis: UP${NC}"
elif [ "$REDIS_STATUS" = "null" ] || [ -z "$REDIS_STATUS" ]; then
    echo -e "${YELLOW}⚠️  Redis: No incluido en health check${NC}"
else
    echo -e "${RED}❌ Redis: DOWN${NC}"
fi
echo ""

# Test 8: Database Check (via health)
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "8️⃣  Database Connection (via health check)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
DB_STATUS=$(cat /tmp/health.json | jq -r '.components.db.status' 2>/dev/null)
if [ "$DB_STATUS" = "UP" ]; then
    echo -e "${GREEN}✅ Database: UP${NC}"
    DB_DETAILS=$(cat /tmp/health.json | jq -r '.components.db.details.database' 2>/dev/null)
    echo "   Database: $DB_DETAILS"
elif [ "$DB_STATUS" = "null" ] || [ -z "$DB_STATUS" ]; then
    echo -e "${YELLOW}⚠️  Database: No incluido en health check${NC}"
else
    echo -e "${RED}❌ Database: DOWN${NC}"
fi
echo ""

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 RESUMEN"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

TOTAL_TESTS=8
PASSED=0

[ "$HEALTH_CODE" = "200" ] && ((PASSED++))
[ "$READINESS_CODE" = "200" ] && ((PASSED++))
[ "$LIVENESS_CODE" = "200" ] && ((PASSED++))
[ "$API_CODE" = "200" ] || [ "$API_CODE" = "401" ] || [ "$API_CODE" = "403" ] && ((PASSED++))
[ "$PARTIDOS_CODE" = "200" ] || [ "$PARTIDOS_CODE" = "401" ] || [ "$PARTIDOS_CODE" = "403" ] && ((PASSED++))
[ ! -z "$CORS" ] && ((PASSED++))
[ "$REDIS_STATUS" = "UP" ] || [ "$REDIS_STATUS" = "null" ] && ((PASSED++))
[ "$DB_STATUS" = "UP" ] && ((PASSED++))

if [ $PASSED -ge 7 ]; then
    echo -e "${GREEN}✅ Backend funcionando correctamente: $PASSED/$TOTAL_TESTS tests passed${NC}"
elif [ $PASSED -ge 5 ]; then
    echo -e "${YELLOW}⚠️  Backend parcialmente funcional: $PASSED/$TOTAL_TESTS tests passed${NC}"
else
    echo -e "${RED}❌ Backend tiene problemas: $PASSED/$TOTAL_TESTS tests passed${NC}"
fi

echo ""
echo "🔗 URLs útiles:"
echo "   • Backend: $BACKEND_URL"
echo "   • Health: $BACKEND_URL/actuator/health"
echo "   • Frontend: https://faltauno-frontend-169771742214.us-central1.run.app"
echo ""

# Cleanup
rm -f /tmp/health.json /tmp/readiness.json /tmp/liveness.json /tmp/api.json /tmp/partidos.json
