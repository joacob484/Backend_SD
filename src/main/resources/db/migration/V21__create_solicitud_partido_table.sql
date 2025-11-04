-- V21: Separar solicitudes de inscripciones
-- Lógica simplificada:
-- - solicitud_partido: usuarios que PIDIERON unirse (pendientes de aprobación)
-- - inscripcion: usuarios ACEPTADOS (sin estados, estar aquí = estar dentro)

-- PASO 1: Crear tabla para solicitudes pendientes
CREATE TABLE solicitud_partido (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partido_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    comentario VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_solicitud_partido FOREIGN KEY (partido_id) 
        REFERENCES partido(id) ON DELETE CASCADE,
    CONSTRAINT fk_solicitud_usuario FOREIGN KEY (usuario_id) 
        REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT uk_solicitud_partido_usuario UNIQUE (partido_id, usuario_id)
);

-- Índices para búsquedas rápidas
CREATE INDEX idx_solicitud_partido ON solicitud_partido(partido_id, created_at DESC);
CREATE INDEX idx_solicitud_usuario ON solicitud_partido(usuario_id, created_at DESC);

COMMENT ON TABLE solicitud_partido IS 'Solicitudes pendientes de aprobación para unirse a un partido';
COMMENT ON COLUMN solicitud_partido.comentario IS 'Mensaje opcional del usuario al solicitar unirse';

-- PASO 2: Migrar solicitudes PENDIENTES actuales a la nueva tabla
INSERT INTO solicitud_partido (id, partido_id, usuario_id, comentario, created_at)
SELECT id, partido_id, usuario_id, comentario, created_at
FROM inscripcion
WHERE estado = 'PENDIENTE';

-- PASO 3: Eliminar solicitudes pendientes de la tabla inscripcion
DELETE FROM inscripcion WHERE estado = 'PENDIENTE';

-- PASO 4: Eliminar inscripciones rechazadas/canceladas (ya no existen conceptualmente)
DELETE FROM inscripcion WHERE estado IN ('RECHAZADO', 'CANCELADO');

-- PASO 5: Eliminar objetos dependientes de la columna estado antes de dropearla
-- Estas vistas/triggers fueron creadas en migraciones anteriores y dependen del estado
DROP VIEW IF EXISTS v_inscripciones_activas CASCADE;
DROP VIEW IF EXISTS v_inscripciones_estadisticas CASCADE;
DROP TRIGGER IF EXISTS trigger_inscripcion_fecha_estado ON inscripcion CASCADE;
DROP FUNCTION IF EXISTS update_inscripcion_fecha_estado() CASCADE;

-- PASO 6: Eliminar columna estado de inscripcion (ya no se necesita)
-- Todas las inscripciones que quedan son ACEPTADAS por definición
ALTER TABLE inscripcion DROP COLUMN IF EXISTS estado;
ALTER TABLE inscripcion DROP COLUMN IF EXISTS motivo_rechazo;
ALTER TABLE inscripcion DROP COLUMN IF EXISTS fecha_rechazo;
ALTER TABLE inscripcion DROP COLUMN IF EXISTS fecha_cancelacion;

-- Solo conservar fecha_aceptacion (cuando fueron agregados al partido)
ALTER TABLE inscripcion RENAME COLUMN fecha_aceptacion TO fecha_inscripcion;
ALTER TABLE inscripcion ALTER COLUMN fecha_inscripcion SET DEFAULT NOW();

COMMENT ON TABLE inscripcion IS 'Jugadores ACEPTADOS en un partido (estar aquí = estar dentro)';
COMMENT ON COLUMN inscripcion.fecha_inscripcion IS 'Fecha en que el organizador aceptó al jugador';

-- PASO 7: Actualizar índices (eliminar los que usaban estado)
DROP INDEX IF EXISTS idx_inscripcion_estado;
DROP INDEX IF EXISTS idx_inscripcion_usuario_estado;
DROP INDEX IF EXISTS idx_inscripcion_partido_estado_created;

-- Crear índices simples (sin estado)
CREATE INDEX IF NOT EXISTS idx_inscripcion_partido_created 
    ON inscripcion(partido_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_inscripcion_usuario_created 
    ON inscripcion(usuario_id, created_at DESC);

-- PASO 8: Log de migración
DO $$
DECLARE
    solicitudes_movidas INT;
    inscripciones_limpiadas INT;
    inicio TIMESTAMP;
    fin TIMESTAMP;
    duracion INTERVAL;
BEGIN
    inicio := clock_timestamp();
    
    SELECT COUNT(*) INTO solicitudes_movidas FROM solicitud_partido;
    SELECT COUNT(*) INTO inscripciones_limpiadas FROM inscripcion;
    
    fin := clock_timestamp();
    duracion := fin - inicio;
    
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Migración V21 completada exitosamente';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Solicitudes movidas a solicitud_partido: %', solicitudes_movidas;
    RAISE NOTICE 'Inscripciones activas (aceptadas): %', inscripciones_limpiadas;
    RAISE NOTICE 'Columna estado eliminada de inscripcion: SI';
    RAISE NOTICE 'Duración de la migración: %', duracion;
    RAISE NOTICE '========================================';
END $$;
