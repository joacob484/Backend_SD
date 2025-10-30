#!/bin/bash
# Script para detectar cambios en código relacionado con Redis o Pub/Sub

set -e

echo "🔍 Detectando cambios en servicios..."
echo ""

# Obtener archivos modificados en el último commit
CHANGED_FILES=$(git diff --name-only HEAD~1 HEAD 2>/dev/null || echo "")

# Flags para detectar cambios
REDIS_CHANGED=false
PUBSUB_CHANGED=false

# Buscar archivos relacionados con Redis
if echo "$CHANGED_FILES" | grep -iE "(redis|cache|RedisConfig|RateLimiting)" > /dev/null 2>&1; then
  REDIS_CHANGED=true
  echo "✅ Cambios detectados en código de Redis"
fi

# Buscar archivos relacionados con Pub/Sub
if echo "$CHANGED_FILES" | grep -iE "(pubsub|PubSub|EventPublisher|EventSubscriber)" > /dev/null 2>&1; then
  PUBSUB_CHANGED=true
  echo "✅ Cambios detectados en código de Pub/Sub"
fi

# Exportar como outputs de GitHub Actions
echo "redis_changed=$REDIS_CHANGED" >> $GITHUB_OUTPUT
echo "pubsub_changed=$PUBSUB_CHANGED" >> $GITHUB_OUTPUT

echo ""
echo "📊 Resumen de cambios:"
echo "  Redis: $REDIS_CHANGED"
echo "  Pub/Sub: $PUBSUB_CHANGED"
