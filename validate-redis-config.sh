#!/bin/bash
#
# Script de validación de configuración Redis
# Ejecutar ANTES de cada deployment para asegurar que Redis esté correctamente configurado
#
# Uso: bash validate-redis-config.sh
#

set -e

echo "╔════════════════════════════════════════════════════════════╗"
echo "║      VALIDACIÓN DE CONFIGURACIÓN REDIS - FALTA UNO        ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuración esperada
EXPECTED_REDIS_IP="10.217.135.172"
EXPECTED_REDIS_PORT="6379"
PROJECT_ID="master-might-274420"
SERVICE_NAME="faltauno-backend"
REGION="us-central1"

errors=0
warnings=0

echo "📋 VERIFICANDO CONFIGURACIÓN..."
echo ""

# 1. Verificar cloudbuild-cloudrun.yaml
echo "1️⃣  Verificando cloudbuild-cloudrun.yaml..."
if grep -q "_REDIS_HOST: \"$EXPECTED_REDIS_IP\"" cloudbuild-cloudrun.yaml; then
    echo -e "   ${GREEN}✓${NC} _REDIS_HOST configurado correctamente ($EXPECTED_REDIS_IP)"
else
    echo -e "   ${RED}✗${NC} _REDIS_HOST NO encontrado o incorrecto en cloudbuild-cloudrun.yaml"
    ((errors++))
fi

if grep -q "_REDIS_PORT: \"$EXPECTED_REDIS_PORT\"" cloudbuild-cloudrun.yaml; then
    echo -e "   ${GREEN}✓${NC} _REDIS_PORT configurado correctamente ($EXPECTED_REDIS_PORT)"
else
    echo -e "   ${RED}✗${NC} _REDIS_PORT NO encontrado o incorrecto en cloudbuild-cloudrun.yaml"
    ((errors++))
fi

# 2. Verificar que las variables se usan en el deployment
echo ""
echo "2️⃣  Verificando uso de variables en deployment..."
if grep -q "SPRING_REDIS_HOST=\${_REDIS_HOST}" cloudbuild-cloudrun.yaml; then
    echo -e "   ${GREEN}✓${NC} SPRING_REDIS_HOST usa \${_REDIS_HOST}"
else
    echo -e "   ${RED}✗${NC} SPRING_REDIS_HOST no usa la variable de sustitución"
    ((errors++))
fi

if grep -q "SPRING_REDIS_PORT=\${_REDIS_PORT}" cloudbuild-cloudrun.yaml; then
    echo -e "   ${GREEN}✓${NC} SPRING_REDIS_PORT usa \${_REDIS_PORT}"
else
    echo -e "   ${RED}✗${NC} SPRING_REDIS_PORT no usa la variable de sustitución"
    ((errors++))
fi

if grep -q "REDIS_HOST=\${_REDIS_HOST}" cloudbuild-cloudrun.yaml; then
    echo -e "   ${GREEN}✓${NC} REDIS_HOST usa \${_REDIS_HOST}"
else
    echo -e "   ${RED}✗${NC} REDIS_HOST no usa la variable de sustitución"
    ((errors++))
fi

# 3. Verificar application.yml
echo ""
echo "3️⃣  Verificando application.yml..."
if grep -q "host: \${SPRING_REDIS_HOST}" src/main/resources/application.yml; then
    echo -e "   ${GREEN}✓${NC} Redis host usa variable de entorno SPRING_REDIS_HOST"
else
    echo -e "   ${YELLOW}⚠${NC}  Redis host podría estar hardcodeado en application.yml"
    ((warnings++))
fi

if grep -q "port: \${SPRING_REDIS_PORT:6379}" src/main/resources/application.yml; then
    echo -e "   ${GREEN}✓${NC} Redis port usa variable de entorno SPRING_REDIS_PORT"
else
    echo -e "   ${YELLOW}⚠${NC}  Redis port podría estar hardcodeado en application.yml"
    ((warnings++))
fi

# 4. Verificar Cloud Run service (si está deployado)
echo ""
echo "4️⃣  Verificando servicio Cloud Run deployado..."
if command -v gcloud &> /dev/null; then
    deployed_redis_host=$(gcloud run services describe $SERVICE_NAME \
        --region $REGION \
        --format='value(spec.template.spec.containers[0].env.filter(name:SPRING_REDIS_HOST).value)' 2>/dev/null || echo "")
    
    if [ -n "$deployed_redis_host" ]; then
        if [ "$deployed_redis_host" == "$EXPECTED_REDIS_IP" ]; then
            echo -e "   ${GREEN}✓${NC} SPRING_REDIS_HOST en Cloud Run: $deployed_redis_host"
        else
            echo -e "   ${RED}✗${NC} SPRING_REDIS_HOST en Cloud Run es INCORRECTO: $deployed_redis_host (esperado: $EXPECTED_REDIS_IP)"
            echo -e "   ${YELLOW}→${NC} Ejecutar: gcloud run services update $SERVICE_NAME --region $REGION --set-env-vars SPRING_REDIS_HOST=$EXPECTED_REDIS_IP"
            ((errors++))
        fi
    else
        echo -e "   ${YELLOW}⚠${NC}  No se pudo verificar el servicio Cloud Run (¿no deployado aún?)"
        ((warnings++))
    fi
else
    echo -e "   ${YELLOW}⚠${NC}  gcloud CLI no disponible, saltando verificación de Cloud Run"
    ((warnings++))
fi

# 5. Verificar conectividad a Memorystore (si gcloud disponible)
echo ""
echo "5️⃣  Verificando Memorystore Redis..."
if command -v gcloud &> /dev/null; then
    redis_instance=$(gcloud redis instances list --region $REGION --format='value(host)' 2>/dev/null | head -n1 || echo "")
    
    if [ -n "$redis_instance" ]; then
        if [ "$redis_instance" == "$EXPECTED_REDIS_IP" ]; then
            echo -e "   ${GREEN}✓${NC} Memorystore Redis IP: $redis_instance"
        else
            echo -e "   ${RED}✗${NC} Memorystore Redis IP es diferente: $redis_instance (esperado: $EXPECTED_REDIS_IP)"
            echo -e "   ${YELLOW}→${NC} Actualizar EXPECTED_REDIS_IP en este script y cloudbuild-cloudrun.yaml"
            ((errors++))
        fi
    else
        echo -e "   ${YELLOW}⚠${NC}  No se encontró instancia de Memorystore Redis"
        ((warnings++))
    fi
fi

# RESUMEN
echo ""
echo "════════════════════════════════════════════════════════════"
if [ $errors -eq 0 ] && [ $warnings -eq 0 ]; then
    echo -e "${GREEN}✅ VALIDACIÓN EXITOSA - CONFIGURACIÓN CORRECTA${NC}"
elif [ $errors -eq 0 ]; then
    echo -e "${YELLOW}⚠️  VALIDACIÓN CON ADVERTENCIAS ($warnings warnings)${NC}"
    echo "   La configuración es válida pero revisa las advertencias arriba"
else
    echo -e "${RED}❌ VALIDACIÓN FALLIDA ($errors errores, $warnings warnings)${NC}"
    echo "   NO DEPLOYAR hasta resolver los errores"
fi
echo "════════════════════════════════════════════════════════════"
echo ""

# Exit code
if [ $errors -gt 0 ]; then
    exit 1
else
    exit 0
fi
