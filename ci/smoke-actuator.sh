#!/bin/bash
# Lightweight smoke check for /actuator/health
set -euo pipefail

HOST=${1:-http://localhost:8080}
TIMEOUT=${2:-60}

echo "Running smoke check against $HOST/actuator/health"

end=$((SECONDS+TIMEOUT))
while [ $SECONDS -lt $end ]; do
  status=$(curl -s -o /dev/null -w "%{http_code}" "$HOST/actuator/health" || true)
  if [ "$status" = "200" ]; then
    echo "OK: actuator healthy"
    exit 0
  fi
  echo "Waiting for actuator (status=$status)..."
  sleep 3
done

echo "ERROR: actuator did not become healthy within $TIMEOUT seconds"
exit 2
