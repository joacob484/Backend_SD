-- V9__add_performance_indexes.sql
-- Índices compuestos para mejorar performance de queries críticos

-- ========================================
-- PARTIDO: Índices para búsquedas y filtros
-- ========================================

-- Query: Búsqueda de partidos disponibles por estado y fecha
CREATE INDEX IF NOT EXISTS idx_partido_estado_fecha 
  ON partido(estado, fecha DESC);

-- Query: Búsqueda de partidos por tipo y nivel
CREATE INDEX IF NOT EXISTS idx_partido_tipo_nivel 
  ON partido(tipo_partido, nivel);

-- Query: Búsqueda de partidos por género y fecha
CREATE INDEX IF NOT EXISTS idx_partido_genero_fecha 
  ON partido(genero, fecha DESC);

-- ========================================
-- INSCRIPCION: Índices para queries de jugadores
-- ========================================

-- Query: Obtener inscripciones de un usuario por estado
CREATE INDEX IF NOT EXISTS idx_inscripcion_usuario_estado 
  ON inscripcion(usuario_id, estado);

-- Query: Obtener jugadores aceptados de un partido
-- (Ya existe idx_inscripcion_partido, pero mejoramos con estado)
CREATE INDEX IF NOT EXISTS idx_inscripcion_partido_estado_created 
  ON inscripcion(partido_id, estado, created_at DESC);

-- ========================================
-- REVIEW: Índices para calificaciones
-- ========================================

-- Query: Obtener reviews de un usuario calificado
CREATE INDEX IF NOT EXISTS idx_review_calificado_partido 
  ON review(usuario_calificado_id, partido_id);

-- Query: Reviews pendientes de un partido
CREATE INDEX IF NOT EXISTS idx_review_partido_created 
  ON review(partido_id, created_at DESC);

-- ========================================
-- NOTIFICACION: Índices para listado y marcado de leídas
-- ========================================

-- Query: Notificaciones de un usuario ordenadas por fecha
-- (Incluye leida para filtrar no leídas primero)
CREATE INDEX IF NOT EXISTS idx_notificacion_usuario_leida_created 
  ON notificacion(usuario_id, leida, created_at DESC);

-- Query: Contar notificaciones no leídas
CREATE INDEX IF NOT EXISTS idx_notificacion_usuario_leida 
  ON notificacion(usuario_id, leida) WHERE leida = false;

-- ========================================
-- MENSAJE: Índices para chat de partidos
-- ========================================

-- Query: Mensajes de un partido ordenados por fecha
CREATE INDEX IF NOT EXISTS idx_mensaje_partido_created 
  ON mensaje(partido_id, created_at DESC);

-- Query: Mensajes no leídos de un usuario
CREATE INDEX IF NOT EXISTS idx_mensaje_destinatario_leido 
  ON mensaje(destinatario_id, leido) WHERE leido = false;

-- ========================================
-- AMISTAD: Índices para búsqueda de relaciones
-- ========================================

-- Query: Amistades de un usuario por estado
CREATE INDEX IF NOT EXISTS idx_amistad_usuario_estado 
  ON amistad(usuario_id, estado);

-- Query: Buscar amistad entre dos usuarios (ambas direcciones)
CREATE INDEX IF NOT EXISTS idx_amistad_amigo_usuario 
  ON amistad(amigo_id, usuario_id);

-- ========================================
-- COMENTARIOS
-- ========================================
COMMENT ON INDEX idx_partido_estado_fecha IS 'Optimiza búsqueda de partidos disponibles filtrados por estado y ordenados por fecha';
COMMENT ON INDEX idx_inscripcion_usuario_estado IS 'Optimiza consulta de partidos de un usuario por estado de inscripción';
COMMENT ON INDEX idx_review_calificado_partido IS 'Optimiza consulta de reviews recibidas por un usuario';
COMMENT ON INDEX idx_notificacion_usuario_leida_created IS 'Optimiza listado de notificaciones con filtro de leídas';
COMMENT ON INDEX idx_mensaje_partido_created IS 'Optimiza chat de partidos con ordenamiento por fecha';
COMMENT ON INDEX idx_amistad_usuario_estado IS 'Optimiza consulta de amigos y solicitudes pendientes';

-- ========================================
-- ANALYZE para actualizar estadísticas
-- ========================================
ANALYZE partido;
ANALYZE inscripcion;
ANALYZE review;
ANALYZE notificacion;
ANALYZE mensaje;
ANALYZE amistad;
