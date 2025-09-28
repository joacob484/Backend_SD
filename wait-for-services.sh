#!/bin/sh
set -e

# Variables por defecto
POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_USER="${SPRING_DATASOURCE_USERNAME:-app}"
POSTGRES_DB="${SPRING_DATASOURCE_DB:-faltauno_db}"

REDIS_HOST="${SPRING_REDIS_HOST:-redis}"
REDIS_PORT="${SPRING_REDIS_PORT:-6379}"

RABBITMQ_HOST="${SPRING_RABBITMQ_HOST:-rabbitmq}"
RABBITMQ_PORT="${SPRING_RABBITMQ_PORT:-5672}"

# Espera Postgres
echo "Esperando a Postgres en $POSTGRES_HOST..."
until pg_isready -h "$POSTGRES_HOST" -U "$POSTGRES_USER" -d "$POSTGRES_DB"; do
  echo "Esperando a Postgres..."
  sleep 2
done

# Espera Redis
echo "Esperando a Redis en $REDIS_HOST:$REDIS_PORT..."
until nc -z "$REDIS_HOST" "$REDIS_PORT"; do
  echo "Esperando a Redis..."
  sleep 2
done

# Espera RabbitMQ
echo "Esperando a RabbitMQ en $RABBITMQ_HOST:$RABBITMQ_PORT..."
until nc -z "$RABBITMQ_HOST" "$RABBITMQ_PORT"; do
  echo "Esperando a RabbitMQ..."
  sleep 2
done

echo "Todos los servicios están disponibles, iniciando backend..."
exec "$@"