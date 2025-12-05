-- ÍNDICES OPTIMIZADOS PARA FALTA UNO
-- Ejecutar vía Cloud Console: https://console.cloud.google.com/sql/instances/faltauno-db/query?project=master-might-274420

-- Crear índices optimizados (CONCURRENTLY no bloquea la tabla)
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

-- Actualizar estadísticas para query optimizer
ANALYZE partidos;
ANALYZE usuarios;
ANALYZE inscripciones;
ANALYZE amistades;
ANALYZE notificaciones;
ANALYZE mensajes;
ANALYZE reviews;

-- Verificar índices creados
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname LIKE 'idx_%'
ORDER BY tablename, indexname;
