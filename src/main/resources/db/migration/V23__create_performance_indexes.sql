-- ============================================
-- V23: Critical Performance Indexes (Fast Creation)
-- ============================================
-- Solo índices más críticos primero para deploy rápido
-- Índices adicionales se crearán en V24 de forma CONCURRENTE

-- ✅ Índices CRÍTICOS para tabla PARTIDO
CREATE INDEX IF NOT EXISTS idx_partido_estado 
    ON partido(estado);

CREATE INDEX IF NOT EXISTS idx_partido_organizador 
    ON partido(organizador_id);

-- ✅ Índices CRÍTICOS para tabla INSCRIPCION
CREATE INDEX IF NOT EXISTS idx_inscripcion_usuario 
    ON inscripcion(usuario_id);

CREATE INDEX IF NOT EXISTS idx_inscripcion_partido 
    ON inscripcion(partido_id);

-- ✅ Índices CRÍTICOS para tabla NOTIFICACION  
CREATE INDEX IF NOT EXISTS idx_notificacion_usuario 
    ON notificacion(usuario_id);

-- Nota: Índices adicionales se agregarán en V24 con CONCURRENTLY
