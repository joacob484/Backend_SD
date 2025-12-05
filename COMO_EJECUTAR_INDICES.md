# 📋 GUÍA RÁPIDA: EJECUTAR ÍNDICES POSTGRESQL

## Opción 1: Via Cloud Console (MÁS FÁCIL) ⭐

1. **Abrir Cloud SQL en navegador**:
   ```
   https://console.cloud.google.com/sql/instances/faltauno-db/query?project=master-might-274420
   ```

2. **Copiar y pegar este SQL** (ejecutar todo junto):

```sql
-- Crear índices optimizados
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_partidos_activos 
  ON partidos(fecha, estado, latitud, longitud) 
  WHERE estado IN ('DISPONIBLE', 'ACTIVO');

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_usuarios_email_lower 
  ON usuarios(LOWER(email));

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inscripciones_lookup 
  ON inscripciones(partido_id, usuario_id, estado);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inscripciones_usuario 
  ON inscripciones(usuario_id, estado) 
  WHERE estado IN ('CONFIRMADA', 'PENDIENTE');

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_amistades_lookup 
  ON amistades(usuario_id, amigo_id, estado);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notificaciones_no_leidas 
  ON notificaciones(usuario_id, leido, fecha_creacion) 
  WHERE leido = FALSE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_mensajes_chat 
  ON mensajes(chat_id, fecha_envio DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_reviews_partido 
  ON reviews(partido_id, fecha_creacion DESC);

-- Actualizar estadísticas
ANALYZE partidos;
ANALYZE usuarios;
ANALYZE inscripciones;
ANALYZE amistades;
ANALYZE notificaciones;
ANALYZE mensajes;
ANALYZE reviews;

-- Verificar índices creados
SELECT indexname, tablename 
FROM pg_indexes 
WHERE schemaname = 'public' AND indexname LIKE 'idx_%'
ORDER BY tablename;
```

3. **Click "Run"** - Toma ~2-3 minutos

---

## Opción 2: Via gcloud CLI con Cloud SQL Proxy

```powershell
# 1. Descargar Cloud SQL Proxy (si no lo tienes)
# https://cloud.google.com/sql/docs/postgres/sql-proxy

# 2. Ejecutar proxy en una terminal
cloud_sql_proxy.exe -instances=master-might-274420:us-central1:faltauno-db=tcp:5432

# 3. En otra terminal, conectar con psql
psql -h localhost -U postgres -d faltauno_db

# 4. Copiar y pegar el SQL de arriba
```

---

## ✅ Beneficio de los Índices

- **10-50x más rápido** en queries de búsqueda
- **Partidos activos**: Búsqueda por fecha y ubicación
- **Usuarios**: Login por email (case-insensitive)
- **Inscripciones**: Lookup partido-usuario
- **Notificaciones**: Contador de no leídas
- **Mensajes**: Historial de chat
- **Reviews**: Reviews por partido

---

## 🔍 Verificar que Funcionan (después de crearlos)

```sql
-- Ver uso de índices
SELECT 
  schemaname,
  tablename,
  indexname,
  idx_scan as "Scans",
  idx_tup_read as "Tuples Read"
FROM pg_stat_user_indexes
WHERE indexname LIKE 'idx_%'
ORDER BY idx_scan DESC;
```

---

**Tiempo total: 5 minutos** ⏱️  
**Siguiente tarea**: Storage Lifecycle Policy
