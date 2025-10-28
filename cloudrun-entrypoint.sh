#!/bin/sh
# Ensure SERVER_PORT respects the container-provided PORT at runtime.
# Cloud Run sets PORT; Spring Boot reads SERVER_PORT first if present.
# This wrapper exports SERVER_PORT=$PORT when SERVER_PORT is unset.

if [ -z "${SERVER_PORT}" ] && [ -n "${PORT}" ]; then
  export SERVER_PORT="${PORT}"
fi

# Dump a few non-secret environment variables for troubleshooting (don't print passwords)
echo "==== Runtime env dump (non-secret) ===="
echo "SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL:-<unset>}"
echo "POSTGRES_HOST=${POSTGRES_HOST:-<unset>}"
echo "REDIS_HOST=${REDIS_HOST:-<unset>}"
echo "SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME:-<unset>}"
echo "SERVER_PORT=${SERVER_PORT:-<unset>}"
echo "PORT=${PORT:-<unset>}"
echo "======================================="

exec java -jar /app/app.jar
