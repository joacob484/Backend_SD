-- ============================================
-- V23: Performance Indexes for Faster Queries
-- ============================================
-- Objetivo: Reducir tiempo de queries en endpoints frecuentes
-- Impacto esperado: 50-80% m├ís r├ípido en b├║squedas

-- Γ£à ├ìndices para tabla PARTIDO (queries m├ís frecuentes)
CREATE INDEX IF NOT EXISTS idx_partido_fecha 
    ON partido(fecha) 
    WHERE fecha >= CURRENT_DATE;  -- Partial index: solo partidos futuros

CREATE INDEX IF NOT EXISTS idx_partido_estado 
    ON partido(estado);

CREATE INDEX IF NOT EXISTS idx_partido_organizador 
    ON partido(organizador_id);

-- ├ìndice compuesto para b├║squeda combinada (estado + fecha)
-- Usado en: buscar partidos disponibles por fecha
CREATE INDEX IF NOT EXISTS idx_partido_estado_fecha 
    ON partido(estado, fecha DESC);

-- ├ìndice para b├║squedas por ubicaci├│n (LIKE queries)
CREATE INDEX IF NOT EXISTS idx_partido_ubicacion 
    ON partido USING gin(to_tsvector('spanish', 
        COALESCE(nombre_ubicacion, '') || ' ' || COALESCE(direccion_ubicacion, '')));

-- Γ£à ├ìndices para tabla INSCRIPCION (joins frecuentes)
CREATE INDEX IF NOT EXISTS idx_inscripcion_usuario 
    ON inscripcion(usuario_id);

CREATE INDEX IF NOT EXISTS idx_inscripcion_partido 
    ON inscripcion(partido_id);

-- ├ìndice compuesto para verificar si usuario est├í inscrito
CREATE INDEX IF NOT EXISTS idx_inscripcion_usuario_partido 
    ON inscripcion(usuario_id, partido_id);

-- ├ìndice para contar inscripciones por partido
CREATE INDEX IF NOT EXISTS idx_inscripcion_partido_estado 
    ON inscripcion(partido_id, estado);

-- Γ£à ├ìndices para tabla NOTIFICACION (polling frecuente)
CREATE INDEX IF NOT EXISTS idx_notificacion_usuario 
    ON notificacion(usuario_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notificacion_usuario_leida 
    ON notificacion(usuario_id, leida) 
    WHERE leida = false;  -- Partial index: solo no le├¡das

-- Γ£à ├ìndices para tabla USUARIO (b├║squedas y autenticaci├│n)
CREATE INDEX IF NOT EXISTS idx_usuario_email 
    ON usuario(email) 
    WHERE deleted_at IS NULL;  -- Partial index: solo usuarios activos

CREATE INDEX IF NOT EXISTS idx_usuario_last_activity 
    ON usuario(last_activity_at) 
    WHERE deleted_at IS NULL;

-- Γ£à ├ìndices para tabla REVIEW (promedios y estad├¡sticas)
CREATE INDEX IF NOT EXISTS idx_review_usuario_calificado 
    ON review(usuario_calificado_id);

CREATE INDEX IF NOT EXISTS idx_review_partido 
    ON review(partido_id);

-- Γ£à ├ìndices para tabla AMISTAD (b├║squedas bidireccionales)
CREATE INDEX IF NOT EXISTS idx_amistad_usuario1 
    ON amistad(usuario1_id, estado);

CREATE INDEX IF NOT EXISTS idx_amistad_usuario2 
    ON amistad(usuario2_id, estado);

-- Γ£à ├ìndices para tabla MENSAJE (chats por partido)
CREATE INDEX IF NOT EXISTS idx_mensaje_partido 
    ON mensaje(partido_id, fecha_envio DESC);

CREATE INDEX IF NOT EXISTS idx_mensaje_usuario 
    ON mensaje(usuario_id);

-- ============================================
-- ANALYZE para actualizar estad├¡sticas
-- ============================================
ANALYZE partido;
ANALYZE inscripcion;
ANALYZE notificacion;
ANALYZE usuario;
ANALYZE review;
ANALYZE amistad;
ANALYZE mensaje;
