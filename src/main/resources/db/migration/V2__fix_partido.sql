-- V2__fix_partido.sql
-- Correcciones y mejoras al esquema (idempotente)

-- 1) Columnas nuevas en partido
-- estado: garantizamos existencia, default, backfill y NOT NULL
ALTER TABLE partido ADD COLUMN IF NOT EXISTS estado VARCHAR(20);
ALTER TABLE partido ALTER COLUMN estado SET DEFAULT 'PENDIENTE';
UPDATE partido SET estado = 'PENDIENTE' WHERE estado IS NULL;
ALTER TABLE partido ALTER COLUMN estado SET NOT NULL;

-- nivel: solo default si no existe y dejamos nullable
ALTER TABLE partido ADD COLUMN IF NOT EXISTS nivel VARCHAR(20);
ALTER TABLE partido ALTER COLUMN nivel SET DEFAULT 'INTERMEDIO';

-- 1.b) Checks (sin IF NOT EXISTS, usamos bloque DO para evitar duplicados)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'partido_estado_check'
  ) THEN
    ALTER TABLE partido
      ADD CONSTRAINT partido_estado_check
      CHECK (estado IN ('PENDIENTE','CONFIRMADO','CANCELADO','COMPLETADO'));
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'partido_nivel_check'
  ) THEN
    ALTER TABLE partido
      ADD CONSTRAINT partido_nivel_check
      CHECK (nivel IN ('PRINCIPIANTE','INTERMEDIO','AVANZADO','PROFESIONAL'));
  END IF;
END $$;

-- 2) Mensajes grupales: permitir NULL en destinatario_id y partido_id
-- (Si ya son NULLABLE, esto no falla)
ALTER TABLE mensaje ALTER COLUMN destinatario_id DROP NOT NULL;
ALTER TABLE mensaje ALTER COLUMN partido_id DROP NOT NULL;

-- 3) Índices (idempotentes)
CREATE INDEX IF NOT EXISTS idx_partido_estado     ON partido(estado);
CREATE INDEX IF NOT EXISTS idx_partido_tipo       ON partido(tipo_partido);
CREATE INDEX IF NOT EXISTS idx_partido_organizador ON partido(organizador_id);

CREATE INDEX IF NOT EXISTS idx_inscripcion_estado      ON inscripcion(estado);
CREATE INDEX IF NOT EXISTS idx_inscripcion_created_at  ON inscripcion(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mensaje_partido    ON mensaje(partido_id);
CREATE INDEX IF NOT EXISTS idx_mensaje_remitente  ON mensaje(remitente_id);
CREATE INDEX IF NOT EXISTS idx_mensaje_created_at ON mensaje(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_review_partido     ON review(partido_id);
CREATE INDEX IF NOT EXISTS idx_review_calificador ON review(usuario_que_califica_id);

-- 4) Comentarios
COMMENT ON COLUMN partido.estado IS 'Estado del partido: PENDIENTE (creado), CONFIRMADO (lleno), CANCELADO, COMPLETADO';
COMMENT ON COLUMN partido.nivel  IS 'Nivel técnico del partido: PRINCIPIANTE, INTERMEDIO, AVANZADO, PROFESIONAL';
COMMENT ON COLUMN mensaje.partido_id IS 'NULL = mensaje directo; con valor = chat grupal del partido';
COMMENT ON COLUMN mensaje.destinatario_id IS 'NULL = mensaje grupal del partido; con valor = mensaje directo';
COMMENT ON COLUMN inscripcion.estado IS 'PENDIENTE, ACEPTADO, RECHAZADO';