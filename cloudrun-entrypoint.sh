#!/bin/sh
# Ensure SERVER_PORT respects the container-provided PORT at runtime.
# Cloud Run sets PORT; Spring Boot reads SERVER_PORT first if present.
# This wrapper exports SERVER_PORT=$PORT when SERVER_PORT is unset.

if [ -z "${SERVER_PORT}" ] && [ -n "${PORT}" ]; then
  export SERVER_PORT="${PORT}"
fi

exec java -jar /app/app.jar
