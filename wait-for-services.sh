#!/bin/bash
# wait-for-services.sh - single, corrected version
# Waits for optional services (Postgres, Redis) and then execs the provided command.

set -e

echo "🔍 Waiting for required services..."

# Configuration with sensible defaults
POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
REDIS_HOST="${REDIS_HOST:-${SPRING_REDIS_HOST:-redis}}"
REDIS_PORT="${REDIS_PORT:-${SPRING_REDIS_PORT:-6379}}"

echo "ℹ️ Environment snapshot (non-sensitive):"
echo "  SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-}"
echo "  FRONTEND_URL=${FRONTEND_URL:-}"
echo "  REDIS_HOST=${REDIS_HOST} REDIS_PORT=${REDIS_PORT}"
echo "  POSTGRES_HOST=${POSTGRES_HOST} POSTGRES_PORT=${POSTGRES_PORT}"

MAX_RETRIES=30
RETRY_INTERVAL=2

# Helper: wait for a TCP port to be open
wait_for_service() {
    local host=$1
    local port=$2
    local service_name=$3
    local retries=0

    echo "⏳ Waiting for $service_name ($host:$port)..."
    while ! nc -z "$host" "$port" > /dev/null 2>&1; do
        retries=$((retries + 1))
        if [ $retries -ge $MAX_RETRIES ]; then
            echo "⚠️ WARNING: $service_name not available after $MAX_RETRIES attempts"
            echo "  Continuing startup, application will start without $service_name. Monitor for failures."
            return 1
        fi
        echo "   Attempt $retries/$MAX_RETRIES - waiting ${RETRY_INTERVAL}s..."
        sleep $RETRY_INTERVAL
    done
    echo "✅ $service_name is ready!"
    return 0
}

# PostgreSQL: skip TCP wait if SPRING_DATASOURCE_URL uses Cloud SQL socketFactory
if echo "${SPRING_DATASOURCE_URL:-}" | grep -q "cloudSqlInstance"; then
    echo "ℹ️ Detected Cloud SQL socket factory in SPRING_DATASOURCE_URL; skipping TCP PostgreSQL wait."
else
    if wait_for_service "$POSTGRES_HOST" "$POSTGRES_PORT" "PostgreSQL"; then
        # Optionally verify connections using pg_isready if available
        if command -v pg_isready > /dev/null 2>&1; then
            echo "🔍 Verifying PostgreSQL accepts connections..."
            PGPASSWORD="${SPRING_DATASOURCE_PASSWORD:-pass}" \
                pg_isready -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "${SPRING_DATASOURCE_USERNAME:-app}" -d "${POSTGRES_DB:-faltauno_db}" -t 5 || echo "⚠️ pg_isready failed; continuing startup"
        fi
    else
        echo "?? WARNING: PostgreSQL not available after ${MAX_RETRIES} attempts; continuing without DB"
    fi
fi

# Redis (optional)
wait_for_service "$REDIS_HOST" "$REDIS_PORT" "Redis" || echo "⚠️ Redis wait timed out; continuing startup"

echo ""
echo "🎉 Service wait complete; starting application..."
echo ""

# Debug info: print the command and Java availability before exec
echo "🔁 Exec command: $@"
echo "🔍 which java: $(which java 2>/dev/null || echo 'java not found')"
echo "🔍 PORT env: ${PORT:-not-set} SERVER_PORT env: ${SERVER_PORT:-not-set}"

# Exec the supplied command (this replaces the shell with the target process)
exec "$@"
