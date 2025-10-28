#!/usr/bin/env bash
set -euo pipefail

# promote.sh - promote a Cloud Run service revision to full traffic after smoke checks
# Usage: ./promote.sh <service> <region> <project>

SERVICE=${1:-faltauno-backend-test}
REGION=${2:-us-central1}
PROJECT=${3:-master-might-274420}

echo "Promoting latest revision of $SERVICE in $PROJECT/$REGION to 100% traffic"
gcloud run services update-traffic "$SERVICE" --to-latest --region "$REGION" --project "$PROJECT"

echo "Promotion complete."
