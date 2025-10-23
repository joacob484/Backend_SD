#!/bin/bash
# Script para reparar Flyway checksum en Cloud Shell

echo "🔧 Reparando checksum de Flyway V8..."

# Descargar Cloud SQL Proxy si no existe
if [ ! -f ./cloud-sql-proxy ]; then
    echo "📥 Descargando Cloud SQL Proxy..."
    wget https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 -O cloud-sql-proxy
    chmod +x cloud-sql-proxy
fi

# Iniciar proxy en background
echo "🚀 Iniciando Cloud SQL Proxy..."
./cloud-sql-proxy -instances=master-might-274420:us-central1:faltauno-db=tcp:5432 &
PROXY_PID=$!

# Esperar a que el proxy esté listo
echo "⏳ Esperando conexión..."
sleep 5

# Ejecutar el repair
echo "🔄 Ejecutando UPDATE en flyway_schema_history..."
PGPASSWORD="your_password_here" psql "host=127.0.0.1 port=5432 sslmode=disable user=app dbname=faltauno_db" << 'EOF'
UPDATE flyway_schema_history SET checksum = -178316961 WHERE version = '8';
SELECT version, description, checksum, installed_on FROM flyway_schema_history WHERE version = '8';
EOF

# Matar el proxy
echo "🛑 Deteniendo Cloud SQL Proxy..."
kill $PROXY_PID

echo "✅ ¡Listo! Verifica el resultado arriba."
