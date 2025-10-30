#!/usr/bin/env bash
set -euo pipefail

echo "════════════════════════════════════════════════════"
echo "🚀 DEPLOYING TO CLOUD RUN - SIMPLIFIED VERSION"
echo "════════════════════════════════════════════════════"

# Get project ID
PROJECT_ID=${PROJECT_ID:-$(gcloud config get-value project 2>/dev/null)}
echo "📦 Project: $PROJECT_ID"

# Get image tag
IMAGE_TAG=${IMAGE_TAG:-${BUILD_ID:-${SHORT_SHA:-latest}}}
echo "🏷️  Image Tag: $IMAGE_TAG"

# Deploy with MINIMAL configuration - just to make it START
echo ""
echo "🎯 Deploying with minimal config (debug mode)..."
echo ""

gcloud run deploy faltauno-backend \
  --image gcr.io/$PROJECT_ID/faltauno-backend:${IMAGE_TAG} \
  --region us-central1 \
  --platform managed \
  --allow-unauthenticated \
  --port 8080 \
  --timeout 600 \
  --memory 2Gi \
  --cpu 2 \
  --min-instances 0 \
  --max-instances 5 \
  --set-env-vars="SPRING_PROFILES_ACTIVE=cloudrun" \
  --set-env-vars="SERVER_PORT=8080" \
  --set-env-vars="JAVA_TOOL_OPTIONS=-Xmx1536m -Xms512m" \
  --execution-environment=gen2

echo ""
echo "✅ Deployment command executed!"
echo ""
echo "Check status:"
echo "https://console.cloud.google.com/run/detail/us-central1/faltauno-backend?project=$PROJECT_ID"
