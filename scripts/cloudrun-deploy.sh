#!/usr/bin/env bash
set -euo pipefail

# Usage:
#  cloudrun-deploy.sh <SPRING_PASS> <REDIS_HOST> <POSTGRES_HOST> <CLOUDSQL_INSTANCE> <SPRING_URL> <SPRING_USER>

# The first argument is expected to be the base64-encoded password to avoid commas/newline parsing issues
ENCODED_SPRING_PASS=${1:-}
SPRING_PASS=""
if [ -n "$ENCODED_SPRING_PASS" ]; then
  # try decoding; don't let decode failure kill the script (fallback below)
  SPRING_PASS=$( { echo "$ENCODED_SPRING_PASS" | base64 --decode 2>/dev/null || true; } )
fi

# Ensure PROJECT_ID is available early (used when reading secrets)
PROJECT_ID=${PROJECT_ID:-$(gcloud config get-value project 2>/dev/null || true)}

# If we couldn't decode a valid base64 password, try to read it from Secret Manager
if [ -z "$SPRING_PASS" ]; then
  if command -v gcloud >/dev/null 2>&1; then
    echo "Decoded password empty or invalid base64 — attempting to read from Secret Manager 'db-password'"
    if [ -n "$PROJECT_ID" ]; then
      SPRING_PASS=$(gcloud secrets versions access latest --secret=db-password --project="$PROJECT_ID" 2>/dev/null || true)
    else
      SPRING_PASS=$(gcloud secrets versions access latest --secret=db-password 2>/dev/null || true)
    fi
  fi
fi
REDIS_HOST=${2:-}
POSTGRES_HOST=${3:-}
CLOUDSQL_INSTANCE=${4:-}
SPRING_URL=${5:-}
SPRING_USER=${6:-app}
IMAGE_ARG=${7:-}

# If substitutions were left as placeholders in the build (e.g. REPLACE_AT_SUBMIT), treat them as empty
if [ "${POSTGRES_HOST}" = "REPLACE_AT_SUBMIT" ]; then
  POSTGRES_HOST=""
fi
if [ "${REDIS_HOST}" = "REPLACE_AT_SUBMIT" ]; then
  REDIS_HOST=""
fi

# If the provided SPRING_URL looks like the default placeholder or contains unsubstituted variables,
# synthesize a reasonable JDBC URL. Prefer the Cloud SQL connector form when CLOUDSQL_INSTANCE is set.
if [ -z "${SPRING_URL}" ] || echo "${SPRING_URL}" | grep -E "REPLACE_AT_SUBMIT|\$\(_POSTGRES_HOST\)" >/dev/null 2>&1; then
  if [ -n "${CLOUDSQL_INSTANCE}" ]; then
    # Use Cloud SQL socket factory form. Use /// (no host) for Cloud SQL Proxy
    SPRING_URL="jdbc:postgresql:///faltauno_db?cloudSqlInstance=${CLOUDSQL_INSTANCE}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
  else
    # Fall back to a host-based URL using POSTGRES_HOST (or localhost if empty)
    PHOST=${POSTGRES_HOST:-localhost}
    SPRING_URL="jdbc:postgresql://${PHOST}:5432/faltauno_db"
  fi
  echo "Synthesized SPRING_DATASOURCE_URL=${SPRING_URL}"
fi

# If a Cloud SQL instance is provided, unconditionally prefer the Cloud SQL connector URL
# to avoid relying on potentially-mis-substituted submitted values.
if [ -n "${CLOUDSQL_INSTANCE}" ]; then
  SPRING_URL="jdbc:postgresql:///faltauno_db?cloudSqlInstance=${CLOUDSQL_INSTANCE}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
  echo "Overriding SPRING_DATASOURCE_URL to Cloud SQL connector form (/// no-host)"
fi

# Update SPRING_DATASOURCE_URL to use Cloud SQL socket factory
if [ -n "${CLOUDSQL_INSTANCE}" ]; then
  SPRING_URL="jdbc:postgresql:///faltauno_db?cloudSqlInstance=${CLOUDSQL_INSTANCE}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
  echo "Updated SPRING_DATASOURCE_URL to use Cloud SQL socket factory"
fi

if [ -n "$CLOUDSQL_INSTANCE" ]; then
  CLOUDSQL_FLAG="--add-cloudsql-instances ${CLOUDSQL_INSTANCE}"
else
  CLOUDSQL_FLAG=""
fi

# Ensure PROJECT_ID is available (Cloud Build may not set PROJECT_ID env)
PROJECT_ID=${PROJECT_ID:-$(gcloud config get-value project 2>/dev/null || true)}
if [ -z "$PROJECT_ID" ]; then
  echo "WARNING: PROJECT_ID is not set and could not be determined via gcloud config; some gcloud commands may fail"
fi

# Determine image tag to deploy. Prefer an explicit IMAGE_ARG (positional), then IMAGE_TAG env, then BUILD_ID, then SHORT_SHA.
IMAGE_TAG=${IMAGE_ARG:-${IMAGE_TAG:-${BUILD_ID:-${SHORT_SHA:-}}}}
if [ -z "$IMAGE_TAG" ]; then
  echo "ERROR: Could not determine image tag (IMAGE_TAG, BUILD_ID and SHORT_SHA are empty)"
  exit 1
fi

echo "Deploying revision with no-traffic to allow health checks"

# Write environment variables to a temporary YAML file to avoid gcloud CLI parsing issues
ENV_FILE=$(mktemp /tmp/cloudrun-envs.XXXXXX.yaml)
trap 'rm -f "${ENV_FILE}"' EXIT

# Use YAML mapping and quote values to preserve special characters
# Strip any stray CR/LF characters that may have been introduced by substitutions
SPRING_PASS=$(printf '%s' "${SPRING_PASS}" | tr -d '\r\n')
REDIS_HOST=$(printf '%s' "${REDIS_HOST}" | tr -d '\r\n')
POSTGRES_HOST=$(printf '%s' "${POSTGRES_HOST}" | tr -d '\r\n')
SPRING_USER=$(printf '%s' "${SPRING_USER}" | tr -d '\r\n')
SPRING_URL=$(printf '%s' "${SPRING_URL}" | tr -d '\r\n')

# Escape any double quotes in values so we can write them as quoted YAML scalars
escape_for_yaml() {
  printf '%s' "$1" | sed 's/"/\\"/g'
}

printf 'SPRING_DATASOURCE_PASSWORD: "%s"\n' "$(escape_for_yaml "${SPRING_PASS}")" > "${ENV_FILE}"
printf 'REDIS_HOST: "%s"\n' "$(escape_for_yaml "${REDIS_HOST}")" >> "${ENV_FILE}"
printf 'SPRING_REDIS_HOST: "%s"\n' "$(escape_for_yaml "${REDIS_HOST}")" >> "${ENV_FILE}"
printf 'POSTGRES_HOST: "%s"\n' "$(escape_for_yaml "${POSTGRES_HOST}")" >> "${ENV_FILE}"
printf 'SPRING_DATASOURCE_USERNAME: "%s"\n' "$(escape_for_yaml "${SPRING_USER}")" >> "${ENV_FILE}"
printf 'SPRING_DATASOURCE_URL: "%s"\n' "$(escape_for_yaml "${SPRING_URL}")" >> "${ENV_FILE}"

# Ensure the container binds to the port Cloud Run expects
printf 'SERVER_PORT: "%s"\n' "8080" >> "${ENV_FILE}"

# Also provide explicit database-name synonyms to satisfy Cloud SQL/Spring auto-config
printf 'SPRING_DATASOURCE_DATABASE: "%s"\n' "faltauno_db" >> "${ENV_FILE}"
printf 'SPRING_DATASOURCE_NAME: "%s"\n' "faltauno_db" >> "${ENV_FILE}"
printf 'SPRING_CLOUDSQL_INSTANCE: "%s"\n' "$(escape_for_yaml "${CLOUDSQL_INSTANCE}")" >> "${ENV_FILE}"

# Pub/Sub configuration (optional - set to false to disable)
printf 'GCP_PUBSUB_ENABLED: "%s"\n' "${GCP_PUBSUB_ENABLED:-false}" >> "${ENV_FILE}"
printf 'GCP_PUBSUB_TOPIC: "%s"\n' "${GCP_PUBSUB_TOPIC:-faltauno-events}" >> "${ENV_FILE}"
printf 'GCP_PUBSUB_SUBSCRIPTION: "%s"\n' "${GCP_PUBSUB_SUBSCRIPTION:-faltauno-events-sub}" >> "${ENV_FILE}"
printf 'GCP_PROJECT_ID: "%s"\n' "$(escape_for_yaml "${PROJECT_ID}")" >> "${ENV_FILE}"

# Frontend URL for CORS
printf 'FRONTEND_URL: "%s"\n' "${FRONTEND_URL:-https://faltauno-frontend-169771742214.us-central1.run.app}" >> "${ENV_FILE}"

# JWT and OAuth secrets (if provided)
if [ -n "${JWT_SECRET:-}" ]; then
  printf 'JWT_SECRET: "%s"\n' "$(escape_for_yaml "${JWT_SECRET}")" >> "${ENV_FILE}"
fi
if [ -n "${GOOGLE_CLIENT_ID:-}" ]; then
  printf 'GOOGLE_CLIENT_ID: "%s"\n' "$(escape_for_yaml "${GOOGLE_CLIENT_ID}")" >> "${ENV_FILE}"
fi
if [ -n "${GOOGLE_CLIENT_SECRET:-}" ]; then
  printf 'GOOGLE_CLIENT_SECRET: "%s"\n' "$(escape_for_yaml "${GOOGLE_CLIENT_SECRET}")" >> "${ENV_FILE}"
fi

# Disable Spring Cloud GCP Cloud SQL environment post-processor so the app uses the provided
# SPRING_DATASOURCE_URL directly and does not require additional Cloud SQL-specific properties.
printf 'SPRING_CLOUD_GCP_SQL_ENABLED: "%s"\n' "true" >> "${ENV_FILE}"

echo "==== Diagnostic: env file contents (password redacted) ===="
# Print env file but redact the line containing the decoded password for logs
awk '
  /^SPRING_DATASOURCE_PASSWORD: \|/ { print; if(getline) { print "  <REDACTED>"; } next }
  { print }
' "${ENV_FILE}"

# If the service doesn't exist yet, gcloud run deploy does not support --no-traffic on create.
if gcloud run services describe faltauno-backend --region us-central1 --platform managed >/dev/null 2>&1; then
  echo "Service exists — deploying revision with --no-traffic"
  REVISION=$(gcloud run deploy faltauno-backend \
    --image gcr.io/$PROJECT_ID/faltauno-backend:${IMAGE_TAG} \
    --region us-central1 \
    --platform managed \
    --no-traffic \
    --timeout=600 \
    --memory=1Gi \
    --cpu=2 \
    $CLOUDSQL_FLAG \
    --env-vars-file "${ENV_FILE}" \
    --format='value(status.latestCreatedRevisionName)')
else
  echo "Service does not exist — creating service (initial deploy will receive traffic)"
  REVISION=$(gcloud run deploy faltauno-backend \
    --image gcr.io/$PROJECT_ID/faltauno-backend:${IMAGE_TAG} \
    --region us-central1 \
    --platform managed \
    --timeout=600 \
    --memory=1Gi \
    --cpu=2 \
    $CLOUDSQL_FLAG \
    --env-vars-file "${ENV_FILE}" \
    --format='value(status.latestCreatedRevisionName)')
fi

echo "Created revision: ${REVISION}"

# Wait for the revision to become Ready (timeout after ~5 minutes)
ATTEMPTS=60
SLEEP=5
i=0
while [ $i -lt $ATTEMPTS ]; do
  READY=$(gcloud run revisions describe "${REVISION}" --region us-central1 --platform managed --format='value(status.conditions[?(@.type=="Ready")].status)') || true
  echo "Revision status: ${READY}"
  if [ "${READY}" = "True" ]; then
    echo "Revision ${REVISION} is ready"
    break
  fi
  i=$(( i + 1 ))
  sleep $SLEEP
done

if [ "${READY}" != "True" ]; then
  echo "Revision ${REVISION} did not become ready after $(( ATTEMPTS * SLEEP )) seconds"
  exit 1
fi

echo "Routing 100% traffic to ${REVISION}"
gcloud run services update-traffic faltauno-backend --to-revisions=${REVISION}=100 --region us-central1 --platform managed
