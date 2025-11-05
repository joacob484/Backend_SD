-- ============================================
-- V24: Fix Performance Indexes
-- ============================================
-- Corrige la migración V23 eliminando índices problemáticos
-- y recreando solo los críticos

-- Eliminar índices que pueden existir de versiones anteriores de V23
DROP INDEX IF EXISTS idx_partido_fecha;
DROP INDEX IF EXISTS idx_partido_estado_fecha;
DROP INDEX IF EXISTS idx_partido_nombre_ubicacion;
DROP INDEX IF EXISTS idx_partido_direccion_ubicacion;
DROP INDEX IF EXISTS idx_partido_ubicacion;
DROP INDEX IF EXISTS idx_inscripcion_usuario_partido;
DROP INDEX IF EXISTS idx_inscripcion_partido_estado;
DROP INDEX IF EXISTS idx_notificacion_usuario_leida;
DROP INDEX IF EXISTS idx_usuario_email;
DROP INDEX IF EXISTS idx_usuario_last_activity;
DROP INDEX IF EXISTS idx_review_usuario_calificado;
DROP INDEX IF EXISTS idx_review_partido;
DROP INDEX IF EXISTS idx_amistad_usuario1;
DROP INDEX IF EXISTS idx_amistad_usuario2;
DROP INDEX IF EXISTS idx_mensaje_partido;
DROP INDEX IF EXISTS idx_mensaje_usuario;

-- Recrear solo los índices críticos (evitar duplicados con IF NOT EXISTS)
CREATE INDEX IF NOT EXISTS idx_partido_estado 
    ON partido(estado);

CREATE INDEX IF NOT EXISTS idx_partido_organizador 
    ON partido(organizador_id);

CREATE INDEX IF NOT EXISTS idx_inscripcion_usuario 
    ON inscripcion(usuario_id);

CREATE INDEX IF NOT EXISTS idx_inscripcion_partido 
    ON inscripcion(partido_id);

CREATE INDEX IF NOT EXISTS idx_notificacion_usuario 
    ON notificacion(usuario_id);
