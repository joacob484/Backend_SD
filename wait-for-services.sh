#!/bin/bash
# wait-for-services.sh - VERSIÓN CORREGIDA
# Espera a que los servicios estén disponibles antes de iniciar la app

set -e

echo "🔍 Waiting for required services..."

# Configuración
POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
# Prefer explicit SPRING_* env vars when present (Cloud Run sets SPRING_REDIS_HOST / SPRING_RABBITMQ_HOST)
REDIS_HOST="${REDIS_HOST:-${SPRING_REDIS_HOST:-redis}}"
REDIS_PORT="${REDIS_PORT:-${SPRING_REDIS_PORT:-6379}}"
RABBITMQ_HOST="${RABBITMQ_HOST:-${SPRING_RABBITMQ_HOST:-rabbitmq}}"
RABBITMQ_PORT="${RABBITMQ_PORT:-${SPRING_RABBITMQ_PORT:-5672}}"

MAX_RETRIES=30
RETRY_INTERVAL=2

# Función para esperar un servicio
wait_for_service() {
    local host=$1
    local port=$2
    local service_name=$3
    local retries=0

    echo "⏳ Waiting for $service_name ($host:$port)..."
    
    while ! nc -z "$host" "$port" > /dev/null 2>&1; do
        retries=$((retries + 1))
        
        if [ $retries -ge $MAX_RETRIES ]; then
            echo "❌ ERROR: $service_name not available after $MAX_RETRIES attempts"
            exit 1
        fi
        
        echo "   Attempt $retries/$MAX_RETRIES - waiting ${RETRY_INTERVAL}s..."
        sleep $RETRY_INTERVAL
    done
    
    echo "✅ $service_name is ready!"
}

# Esperar PostgreSQL
# In Cloud Run we often use Cloud SQL SocketFactory (JDBC URL contains "cloudSqlInstance").
# In that case the database is not reachable via TCP host/port and this script should
# skip the TCP-based checks to avoid exiting the container before the app can
# connect via the socket factory. Detect that and skip the wait when appropriate.
if echo "${SPRING_DATASOURCE_URL:-}" | grep -q "cloudSqlInstance"; then
    echo "ℹ️ Detected Cloud SQL socket factory in SPRING_DATASOURCE_URL; skipping TCP PostgreSQL wait."
else
    wait_for_service "$POSTGRES_HOST" "$POSTGRES_PORT" "PostgreSQL"

    # Verificar que PostgreSQL acepta conexiones (no solo que el puerto esté abierto)
    echo "🔍 Verifying PostgreSQL accepts connections..."
    PGPASSWORD="${SPRING_DATASOURCE_PASSWORD:-pass}" \
        pg_isready -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "${SPRING_DATASOURCE_USERNAME:-app}" -d "${POSTGRES_DB:-faltauno_db}" -t 30

    if [ $? -eq 0 ]; then
        echo "✅ PostgreSQL is accepting connections!"
    else
        echo "❌ ERROR: PostgreSQL not accepting connections"
        exit 1
    fi
fi

# Esperar Redis
wait_for_service "$REDIS_HOST" "$REDIS_PORT" "Redis"

# Esperar RabbitMQ
wait_for_service "$RABBITMQ_HOST" "$RABBITMQ_PORT" "RabbitMQ"

echo ""
echo "🎉 All services are ready!"
echo "🚀 Starting Spring Boot application..."
echo ""

# Ejecutar el comando que se pase como argumentos
exec "$@"