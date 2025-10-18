-- V2__Fix_Schema.sql
-- Correcciones y mejoras al esquema

-- 1. Agregar campos faltantes a partido
ALTER TABLE partido 
ADD COLUMN IF NOT EXISTS estado VARCHAR(20) DEFAULT 'PENDIENTE' NOT NULL;

ALTER TABLE partido 
ADD COLUMN IF NOT EXISTS nivel VARCHAR(20) DEFAULT 'INTERMEDIO';

-- Agregar constraints para validar valores
ALTER TABLE partido 
ADD CONSTRAINT IF NOT EXISTS partido_estado_check 
CHECK (estado IN ('PENDIENTE', 'CONFIRMADO', 'CANCELADO', 'COMPLETADO'));

ALTER TABLE partido 
ADD CONSTRAINT IF NOT EXISTS partido_nivel_check 
CHECK (nivel IN ('PRINCIPIANTE', 'INTERMEDIO', 'AVANZADO', 'PROFESIONAL'));

-- 2. Corregir tabla mensaje para mensajes grupales
-- Si ya existe la tabla con NOT NULL, hay que modificarla
ALTER TABLE mensaje 
ALTER COLUMN destinatario_id DROP NOT NULL;

-- Agregar partido_id si no existe y hacerlo nullable para mensajes directos
ALTER TABLE mensaje 
ALTER COLUMN partido_id DROP NOT NULL;

-- 3. Agregar índices para optimización
CREATE INDEX IF NOT EXISTS idx_partido_estado ON partido(estado);
CREATE INDEX IF NOT EXISTS idx_partido_tipo ON partido(tipo_partido);
CREATE INDEX IF NOT EXISTS idx_partido_organizador ON partido(organizador_id);

CREATE INDEX IF NOT EXISTS idx_inscripcion_estado ON inscripcion(estado);
CREATE INDEX IF NOT EXISTS idx_inscripcion_created_at ON inscripcion(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mensaje_partido ON mensaje(partido_id);
CREATE INDEX IF NOT EXISTS idx_mensaje_remitente ON mensaje(remitente_id);
CREATE INDEX IF NOT EXISTS idx_mensaje_created_at ON mensaje(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_review_partido ON review(partido_id);
CREATE INDEX IF NOT EXISTS idx_review_calificador ON review(usuario_que_califica_id);

-- 4. Comentarios para documentación
COMMENT ON COLUMN partido.estado IS 'Estado del partido: PENDIENTE (creado), CONFIRMADO (lleno), CANCELADO, COMPLETADO';
COMMENT ON COLUMN partido.nivel IS 'Nivel técnico del partido: PRINCIPIANTE, INTERMEDIO, AVANZADO, PROFESIONAL';
COMMENT ON COLUMN mensaje.partido_id IS 'Si es NULL, es mensaje directo; si tiene valor, es mensaje del chat grupal del partido';
COMMENT ON COLUMN mensaje.destinatario_id IS 'Si es NULL, es mensaje grupal del partido; si tiene valor, es mensaje directo';
COMMENT ON COLUMN inscripcion.estado IS 'Estado de la inscripción: PENDIENTE (esperando aprobación), ACEPTADO (confirmado), RECHAZADO';