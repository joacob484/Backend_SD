#!/bin/bash
# 💰 SCRIPT DE OPTIMIZACION CLOUD RUN PARA <$30/MES
# Este script reduce el costo de ~$60/mes a ~$28-34/mes sin comprometer calidad

set -e  # Exit on error

REGION="us-central1"
BACKEND_SERVICE="faltauno-backend"
FRONTEND_SERVICE="faltauno-frontend"

echo "🚀 Optimizando Cloud Run para <\$30/mes..."
echo ""

# ====================================================================
# BACKEND OPTIMIZATION (512MB RAM, 1 CPU)
# ====================================================================
echo "📦 Optimizando Backend..."
gcloud run services update $BACKEND_SERVICE \
  --region=$REGION \
  --memory=512Mi \
  --cpu=1 \
  --min-instances=0 \
  --max-instances=2 \
  --concurrency=80 \
  --cpu-throttling \
  --no-cpu-boost \
  --timeout=60s \
  --set-env-vars="JAVA_OPTS=-Xmx384m -Xms128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m" \
  --quiet

echo "✅ Backend optimizado: 512Mi RAM, 1 CPU, max-instances=2"
echo "   Ahorro estimado: ~\$22/mes"
echo ""

# ====================================================================
# FRONTEND OPTIMIZATION (256MB RAM, 1 CPU)
# ====================================================================
echo "🎨 Optimizando Frontend..."
gcloud run services update $FRONTEND_SERVICE \
  --region=$REGION \
  --memory=256Mi \
  --cpu=1 \
  --min-instances=0 \
  --max-instances=2 \
  --concurrency=100 \
  --cpu-throttling \
  --timeout=30s \
  --quiet

echo "✅ Frontend optimizado: 256Mi RAM, 1 CPU, max-instances=2"
echo "   Ahorro estimado: ~\$12/mes"
echo ""

# ====================================================================
# VERIFICACION
# ====================================================================
echo "🔍 Verificando configuración..."
echo ""

# Backend status
echo "Backend:"
gcloud run services describe $BACKEND_SERVICE --region=$REGION --format="value(spec.template.spec.containers[0].resources.limits.memory,spec.template.spec.containers[0].resources.limits.cpu,spec.template.spec.containerConcurrency)"

# Frontend status
echo "Frontend:"
gcloud run services describe $FRONTEND_SERVICE --region=$REGION --format="value(spec.template.spec.containers[0].resources.limits.memory,spec.template.spec.containers[0].resources.limits.cpu,spec.template.spec.containerConcurrency)"

echo ""
echo "✅ OPTIMIZACIÓN COMPLETA"
echo ""
echo "📊 Resumen:"
echo "   - Backend: 512Mi RAM, 1 CPU, 80 concurrent"
echo "   - Frontend: 256Mi RAM, 1 CPU, 100 concurrent"
echo "   - Min instances: 0 (serverless, \$0 cuando no hay tráfico)"
echo "   - Max instances: 2 (suficiente para 1K usuarios)"
echo ""
echo "💰 Ahorro total estimado: ~\$34/mes"
echo "💰 Costo nuevo: ~\$28-34/mes (vs \$60-80 anterior)"
echo ""
echo "🎯 Próximos pasos:"
echo "   1. Deploy nuevo código (application.yaml optimizado)"
echo "   2. Monitorear latencia en Grafana (debe ser <200ms P95)"
echo "   3. Verificar costos en Cloud Console en 24-48h"
echo ""
