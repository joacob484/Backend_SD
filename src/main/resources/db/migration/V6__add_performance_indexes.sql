-- V6__add_performance_indexes.sql
-- Índices para mejorar performance en queries frecuentes
-- Nota: V4 ya creó idx_notificacion_usuario, idx_notificacion_leida, idx_notificacion_created

-- ============================================
-- TABLA: partido
-- ============================================

-- Índice compuesto para búsqueda de partidos por fecha y estado
CREATE INDEX IF NOT EXISTS idx_partido_fecha_estado 
    ON partido(fecha DESC, estado);

-- Índice para búsqueda de partidos por ubicación (si hay búsquedas geográficas)
CREATE INDEX IF NOT EXISTS idx_partido_ubicacion 
    ON partido(nombre_ubicacion);

-- Índice para búsqueda de partidos por organizador
CREATE INDEX IF NOT EXISTS idx_partido_organizador 
    ON partido(organizador_id);

-- Índice para búsqueda de partidos por tipo
CREATE INDEX IF NOT EXISTS idx_partido_tipo 
    ON partido(tipo_partido);

-- ============================================
-- TABLA: inscripcion
-- ============================================

-- Índice compuesto para búsqueda de inscripciones por usuario y estado
CREATE INDEX IF NOT EXISTS idx_inscripcion_usuario_estado 
    ON inscripcion(usuario_id, estado);

-- Índice compuesto para búsqueda de inscripciones por partido y estado
CREATE INDEX IF NOT EXISTS idx_inscripcion_partido_estado 
    ON inscripcion(partido_id, estado);

-- ============================================
-- TABLA: amistad
-- ============================================

-- Índice compuesto para búsqueda bidireccional de amistades
CREATE INDEX IF NOT EXISTS idx_amistad_usuarios 
    ON amistad(usuario_id, amigo_id);

-- Índice inverso para búsqueda bidireccional optimizada
CREATE INDEX IF NOT EXISTS idx_amistad_usuarios_inverso 
    ON amistad(amigo_id, usuario_id);

-- Índice para búsqueda de amistades por estado
CREATE INDEX IF NOT EXISTS idx_amistad_estado 
    ON amistad(estado);

-- Índice compuesto para solicitudes pendientes de un usuario
CREATE INDEX IF NOT EXISTS idx_amistad_amigo_estado 
    ON amistad(amigo_id, estado);

-- ============================================
-- TABLA: notificacion
-- ============================================

-- Índice compuesto para notificaciones por usuario y estado de lectura
-- (idx_notificacion_usuario y idx_notificacion_leida ya existen en V4, este es compuesto)
CREATE INDEX IF NOT EXISTS idx_notificacion_usuario_leida 
    ON notificacion(usuario_id, leida);

-- ============================================
-- TABLA: mensaje
-- ============================================

-- Índice compuesto para mensajes de un partido ordenados por fecha
CREATE INDEX IF NOT EXISTS idx_mensaje_partido_created 
    ON mensaje(partido_id, created_at DESC);

-- Índice para mensajes enviados por un usuario (remitente)
CREATE INDEX IF NOT EXISTS idx_mensaje_remitente 
    ON mensaje(remitente_id);

-- Índice para mensajes recibidos por un usuario (destinatario)
CREATE INDEX IF NOT EXISTS idx_mensaje_destinatario 
    ON mensaje(destinatario_id);

-- ============================================
-- TABLA: review
-- ============================================

-- Índice para reviews de un usuario (calificado)
CREATE INDEX IF NOT EXISTS idx_review_usuario_calificado 
    ON review(usuario_calificado_id);

-- Índice para reviews escritas por un usuario
CREATE INDEX IF NOT EXISTS idx_review_calificador 
    ON review(calificador_id);

-- Índice para reviews de un partido
CREATE INDEX IF NOT EXISTS idx_review_partido 
    ON review(partido_id);

-- ============================================
-- TABLA: usuario
-- ============================================

-- Índice para búsqueda de usuarios por email (ya existe único, pero explícito)
-- Este índice ya existe por el constraint UNIQUE, no es necesario duplicarlo

-- Índice para búsqueda de usuarios por cédula
CREATE INDEX IF NOT EXISTS idx_usuario_cedula 
    ON usuario(cedula);

-- Índice para búsqueda de usuarios por provider (LOCAL vs GOOGLE)
CREATE INDEX IF NOT EXISTS idx_usuario_provider 
    ON usuario(provider);

-- ============================================
-- COMENTARIOS
-- ============================================

COMMENT ON INDEX idx_partido_fecha_estado IS 'Mejora búsqueda de partidos disponibles por fecha y estado';
COMMENT ON INDEX idx_inscripcion_usuario_estado IS 'Mejora búsqueda de inscripciones de un usuario por estado';
COMMENT ON INDEX idx_amistad_usuarios IS 'Mejora búsqueda bidireccional de amistades';
COMMENT ON INDEX idx_notificacion_usuario_leida IS 'Mejora búsqueda de notificaciones no leídas';
COMMENT ON INDEX idx_mensaje_partido_created IS 'Mejora carga del chat de un partido';
COMMENT ON INDEX idx_review_usuario_calificado IS 'Mejora carga de reviews de un usuario';
