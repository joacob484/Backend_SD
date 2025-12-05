-- 💰 SCRIPT DE OPTIMIZACION - EJECUTAR VIA PSQL
-- Ejecutar: psql -h [CLOUD_SQL_IP] -U postgres -d faltauno_db -f quick-optimize-indexes.sql

-- Crear índices CONCURRENTLY (no bloquea escrituras)
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
  ON notificaciones(usuario_id, leido, fecha_creacion DESC) 
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

-- Mostrar índices creados
\echo '✅ Índices optimizados creados'
SELECT indexname, tablename FROM pg_indexes 
WHERE schemaname = 'public' AND indexname LIKE 'idx_%'
ORDER BY tablename;
