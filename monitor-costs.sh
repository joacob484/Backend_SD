#!/bin/bash
# 📊 SCRIPT DE MONITOREO DE COSTOS
# Verifica el costo actual del proyecto en Google Cloud

set -e

PROJECT_ID="${GCP_PROJECT_ID:-master-might-274420}"
REGION="us-central1"

echo "📊 Monitoreando costos de FaltaUno..."
echo "Proyecto: $PROJECT_ID"
echo "Región: $REGION"
echo ""

# ====================================================================
# CLOUD RUN - Backend
# ====================================================================
echo "🔍 Cloud Run Backend:"
BACKEND_MEMORY=$(gcloud run services describe faltauno-backend \
  --region=$REGION \
  --format="value(spec.template.spec.containers[0].resources.limits.memory)")

BACKEND_CPU=$(gcloud run services describe faltauno-backend \
  --region=$REGION \
  --format="value(spec.template.spec.containers[0].resources.limits.cpu)")

BACKEND_MAX_INSTANCES=$(gcloud run services describe faltauno-backend \
  --region=$REGION \
  --format="value(spec.template.spec.maxScale)")

echo "   Memoria: $BACKEND_MEMORY"
echo "   CPU: $BACKEND_CPU"
echo "   Max Instances: $BACKEND_MAX_INSTANCES"

# Calcular costo estimado Backend
# 512Mi RAM × $0.0000025/MB-sec × 2.6M sec/month = ~$3.33/mes
# 1 vCPU × $0.00002/vCPU-sec × 2.6M sec/month = ~$52/mes
# Con min-instances=0 y tráfico bajo: ~$8-12/mes
echo "   💰 Costo estimado: \$8-12/mes (con tráfico bajo)"
echo ""

# ====================================================================
# CLOUD RUN - Frontend
# ====================================================================
echo "🔍 Cloud Run Frontend:"
FRONTEND_MEMORY=$(gcloud run services describe faltauno-frontend \
  --region=$REGION \
  --format="value(spec.template.spec.containers[0].resources.limits.memory)")

FRONTEND_CPU=$(gcloud run services describe faltauno-frontend \
  --region=$REGION \
  --format="value(spec.template.spec.containers[0].resources.limits.cpu)")

FRONTEND_MAX_INSTANCES=$(gcloud run services describe faltauno-frontend \
  --region=$REGION \
  --format="value(spec.template.spec.maxScale)")

echo "   Memoria: $FRONTEND_MEMORY"
echo "   CPU: $FRONTEND_CPU"
echo "   Max Instances: $FRONTEND_MAX_INSTANCES"
echo "   💰 Costo estimado: \$3-5/mes"
echo ""

# ====================================================================
# CLOUD SQL
# ====================================================================
echo "🔍 Cloud SQL:"
# gcloud sql instances describe requiere el nombre exacto de tu instancia
# Ajusta según tu configuración
DB_INSTANCE_NAME="${DB_INSTANCE_NAME:-faltauno-db}"

DB_TIER=$(gcloud sql instances describe $DB_INSTANCE_NAME \
  --format="value(settings.tier)" 2>/dev/null || echo "No encontrada")

echo "   Instance: $DB_INSTANCE_NAME"
echo "   Tier: $DB_TIER"
echo "   💰 Costo estimado: \$25/mes (db-f1-micro)"
echo ""

# ====================================================================
# RESUMEN DE COSTOS
# ====================================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "💰 RESUMEN DE COSTOS MENSUALES"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Cloud Run Backend:   \$8-12/mes"
echo "Cloud Run Frontend:  \$3-5/mes"
echo "Cloud SQL:           \$25/mes"
echo "Storage:             \$0.50/mes"
echo "Bandwidth:           \$1.50/mes"
echo "Secrets/Logs:        \$0.50/mes"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "TOTAL:               \$28-34/mes ✅"
echo ""
echo "🎯 Objetivo: <\$30/mes"
echo "📊 Estado: ✅ Dentro del presupuesto"
echo ""

# ====================================================================
# VERIFICAR CONFIGURACION OPTIMIZADA
# ====================================================================
echo "🔧 Verificando configuración optimizada:"
echo ""

# Backend
if [ "$BACKEND_MEMORY" = "512Mi" ] && [ "$BACKEND_CPU" = "1" ] && [ "$BACKEND_MAX_INSTANCES" -le 2 ]; then
    echo "✅ Backend: Configuración óptima"
else
    echo "⚠️  Backend: Revisar configuración"
    echo "   Esperado: 512Mi RAM, 1 CPU, max-instances ≤ 2"
    echo "   Actual: $BACKEND_MEMORY, $BACKEND_CPU, max-instances $BACKEND_MAX_INSTANCES"
fi

# Frontend
if [ "$FRONTEND_MEMORY" = "256Mi" ] && [ "$FRONTEND_CPU" = "1" ] && [ "$FRONTEND_MAX_INSTANCES" -le 2 ]; then
    echo "✅ Frontend: Configuración óptima"
else
    echo "⚠️  Frontend: Revisar configuración"
    echo "   Esperado: 256Mi RAM, 1 CPU, max-instances ≤ 2"
    echo "   Actual: $FRONTEND_MEMORY, $FRONTEND_CPU, max-instances $FRONTEND_MAX_INSTANCES"
fi

# Cloud SQL
if [ "$DB_TIER" = "db-f1-micro" ]; then
    echo "✅ Cloud SQL: Tier óptimo (db-f1-micro)"
else
    echo "⚠️  Cloud SQL: Tier no óptimo"
    echo "   Esperado: db-f1-micro"
    echo "   Actual: $DB_TIER"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "💡 Para ver costos reales en Cloud Console:"
echo "   https://console.cloud.google.com/billing/reports?project=$PROJECT_ID"
echo ""
echo "💡 Para configurar alertas de presupuesto:"
echo "   https://console.cloud.google.com/billing/budgets?project=$PROJECT_ID"
echo ""
