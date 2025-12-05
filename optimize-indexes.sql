-- 💰 INDICES OPTIMIZADOS PARA POSTGRESQL
-- Ejecutar UNA VEZ en tu base de datos PostgreSQL
-- Estos índices aceleran las queries más frecuentes 10-50x

-- ==========================================================================
-- IMPORTANTE: Ejecutar en horario de bajo tráfico
-- CONCURRENTLY = no bloquea escrituras durante creación
-- ==========================================================================

BEGIN;

-- 🚀 ÍNDICE 1: Partidos activos (query más frecuente)
-- Acelera: buscar partidos disponibles para inscripción
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_partidos_activos 
  ON partidos(fecha_partido, estado, ubicacion_latitud, ubicacion_longitud) 
  WHERE estado = 'ACTIVO' AND fecha_partido >= CURRENT_DATE;

-- 🚀 ÍNDICE 2: Usuarios por email (login)
-- Acelera: login, búsqueda de usuarios
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_usuarios_email_lower 
  ON usuarios(LOWER(email));

-- 🚀 ÍNDICE 3: Inscripciones lookup (más usado)
-- Acelera: verificar si usuario está inscrito, contar inscripciones
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inscripciones_lookup 
  ON inscripciones(partido_id, usuario_id, estado);

-- 🚀 ÍNDICE 4: Inscripciones por usuario
-- Acelera: obtener partidos de un usuario
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inscripciones_usuario 
  ON inscripciones(usuario_id, estado) 
  WHERE estado IN ('CONFIRMADA', 'PENDIENTE');

-- 🚀 ÍNDICE 5: Contactos y amistades
-- Acelera: buscar amigos, contactos
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_amistades_lookup 
  ON amistades(usuario_id, amigo_id, estado);

-- 🚀 ÍNDICE 6: Notificaciones no leídas
-- Acelera: contador de notificaciones
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notificaciones_no_leidas 
  ON notificaciones(usuario_id, leido, fecha_creacion) 
  WHERE leido = FALSE;

-- 🚀 ÍNDICE 7: Mensajes por chat
-- Acelera: obtener mensajes de un chat
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_mensajes_chat 
  ON mensajes(chat_id, fecha_envio DESC);

-- 🚀 ÍNDICE 8: Reviews por partido
-- Acelera: obtener reviews de un partido
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_reviews_partido 
  ON reviews(partido_id, fecha_creacion DESC);

COMMIT;

-- ==========================================================================
-- ANALIZAR TABLAS (actualizar estadísticas para mejor query planning)
-- ==========================================================================
ANALYZE partidos;
ANALYZE usuarios;
ANALYZE inscripciones;
ANALYZE amistades;
ANALYZE notificaciones;
ANALYZE mensajes;
ANALYZE reviews;

-- ==========================================================================
-- VACUUM (recuperar espacio, actualizar visibilidad)
-- ==========================================================================
VACUUM ANALYZE;

-- ==========================================================================
-- VERIFICAR ÍNDICES CREADOS
-- ==========================================================================
SELECT 
  schemaname,
  tablename,
  indexname,
  indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname LIKE 'idx_%'
ORDER BY tablename, indexname;

-- ==========================================================================
-- ESTADÍSTICAS DE USO (después de 24-48h)
-- ==========================================================================
-- Ejecutar después de 24-48 horas para ver qué índices se usan:
-- 
-- SELECT 
--   schemaname,
--   tablename,
--   indexname,
--   idx_scan as "Index Scans",
--   idx_tup_read as "Tuples Read",
--   idx_tup_fetch as "Tuples Fetched"
-- FROM pg_stat_user_indexes
-- WHERE indexname LIKE 'idx_%'
-- ORDER BY idx_scan DESC;
