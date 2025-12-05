#!/bin/bash
# 📦 SCRIPT DE OPTIMIZACION DE STORAGE
# Configura lifecycle policies para borrar archivos temporales automáticamente

set -e

BUCKET_NAME="${GCS_BUCKET_NAME:-faltauno-assets}"

echo "🗑️ Configurando Storage Lifecycle Policies..."
echo ""

# Crear archivo de configuración
cat > /tmp/lifecycle.json << 'EOF'
{
  "lifecycle": {
    "rule": [
      {
        "action": {
          "type": "Delete"
        },
        "condition": {
          "age": 90,
          "matchesPrefix": ["temp/", "uploads/temp/"]
        },
        "description": "Borrar archivos temporales después de 90 días"
      },
      {
        "action": {
          "type": "SetStorageClass",
          "storageClass": "NEARLINE"
        },
        "condition": {
          "age": 30,
          "matchesPrefix": ["images/old/", "backups/"]
        },
        "description": "Mover archivos viejos a NEARLINE (más barato)"
      },
      {
        "action": {
          "type": "Delete"
        },
        "condition": {
          "age": 7,
          "matchesPrefix": ["logs/", "debug/"]
        },
        "description": "Borrar logs después de 7 días"
      }
    ]
  }
}
EOF

echo "📋 Configuración creada:"
cat /tmp/lifecycle.json
echo ""

# Aplicar lifecycle policy
echo "⚙️ Aplicando lifecycle policy al bucket $BUCKET_NAME..."
gsutil lifecycle set /tmp/lifecycle.json gs://$BUCKET_NAME

echo ""
echo "✅ Lifecycle policy aplicada exitosamente"
echo ""
echo "📊 Políticas configuradas:"
echo "   - Archivos temporales: borrar después de 90 días"
echo "   - Archivos viejos: mover a NEARLINE después de 30 días"
echo "   - Logs/debug: borrar después de 7 días"
echo ""
echo "💰 Ahorro estimado: ~\$0.50-1/mes"
echo ""

# Limpiar
rm /tmp/lifecycle.json

# Verificar
echo "🔍 Verificando configuración actual:"
gsutil lifecycle get gs://$BUCKET_NAME
