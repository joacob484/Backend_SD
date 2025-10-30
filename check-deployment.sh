#!/bin/bash
# Script para verificar el estado del deployment de Cloud Run

echo "🔍 Verificando estado del backend en Cloud Run..."
echo ""

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

SERVICE_NAME="faltauno-backend"
REGION="us-central1"
PROJECT_ID="master-might-274420"

echo "📦 Service: $SERVICE_NAME"
echo "🌎 Region: $REGION"
echo "🏗️  Project: $PROJECT_ID"
echo ""

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}❌ gcloud CLI no está instalado${NC}"
    echo ""
    echo "Para instalar gcloud CLI:"
    echo "https://cloud.google.com/sdk/docs/install"
    echo ""
    echo "O usar Docker:"
    echo "docker run --rm google/cloud-sdk:latest gcloud run services describe $SERVICE_NAME --region=$REGION --project=$PROJECT_ID"
    exit 1
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔄 Estado del servicio"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Get service URL
URL=$(gcloud run services describe $SERVICE_NAME \
  --region=$REGION \
  --project=$PROJECT_ID \
  --format='value(status.url)' 2>/dev/null)

if [ -z "$URL" ]; then
    echo -e "${RED}❌ No se pudo obtener la URL del servicio${NC}"
    exit 1
fi

echo -e "${GREEN}✅ URL: $URL${NC}"
echo ""

# Get latest revision
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📋 Últimas revisiones"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

gcloud run revisions list \
  --service=$SERVICE_NAME \
  --region=$REGION \
  --project=$PROJECT_ID \
  --limit=5 \
  --format='table(metadata.name,status.conditions[0].status,metadata.creationTimestamp,spec.containers[0].image)' 2>/dev/null

echo ""

# Get latest revision status
LATEST_REVISION=$(gcloud run services describe $SERVICE_NAME \
  --region=$REGION \
  --project=$PROJECT_ID \
  --format='value(status.latestCreatedRevisionName)' 2>/dev/null)

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔍 Estado de la última revisión: $LATEST_REVISION"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

READY=$(gcloud run revisions describe $LATEST_REVISION \
  --region=$REGION \
  --project=$PROJECT_ID \
  --format='value(status.conditions[0].status)' 2>/dev/null)

if [ "$READY" = "True" ]; then
    echo -e "${GREEN}✅ Estado: READY${NC}"
else
    echo -e "${RED}❌ Estado: NOT READY${NC}"
    echo ""
    echo "Mensaje de error:"
    gcloud run revisions describe $LATEST_REVISION \
      --region=$REGION \
      --project=$PROJECT_ID \
      --format='value(status.conditions[0].message)' 2>/dev/null
fi

echo ""

# Test health endpoint
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🏥 Probando health check..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$URL/actuator/health" 2>/dev/null)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ Health check: OK (HTTP $HTTP_CODE)${NC}"
    echo ""
    echo "Detalles:"
    curl -s "$URL/actuator/health" | jq '.' 2>/dev/null || curl -s "$URL/actuator/health"
else
    echo -e "${RED}❌ Health check: FAILED (HTTP $HTTP_CODE)${NC}"
fi

echo ""

# Get recent logs
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📜 Últimos logs (últimos 20 mensajes)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=$SERVICE_NAME AND resource.labels.revision_name=$LATEST_REVISION" \
  --limit=20 \
  --project=$PROJECT_ID \
  --format='table(timestamp,severity,textPayload)' \
  2>/dev/null

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✨ Para ver logs en tiempo real:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "gcloud logging tail \"resource.type=cloud_run_revision AND resource.labels.service_name=$SERVICE_NAME\" \\"
echo "  --project=$PROJECT_ID"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔗 URLs útiles:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Backend: $URL"
echo "Health: $URL/actuator/health"
echo "Console: https://console.cloud.google.com/run/detail/$REGION/$SERVICE_NAME?project=$PROJECT_ID"
echo "Logs: https://console.cloud.google.com/logs/query?project=$PROJECT_ID"
echo ""
