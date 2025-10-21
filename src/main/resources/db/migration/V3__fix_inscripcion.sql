-- V3__fix_inscripcion.sql
-- Mejoras a la tabla de inscripciones

-- 1) Nuevas columnas (solo si faltan)
ALTER TABLE inscripcion
  ADD COLUMN IF NOT EXISTS comentario VARCHAR(500),
  ADD COLUMN IF NOT EXISTS motivo_rechazo VARCHAR(500),
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS fecha_aceptacion TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS fecha_rechazo TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS fecha_cancelacion TIMESTAMPTZ;

-- 2) Constraint de estado (incluye CANCELADO)
ALTER TABLE inscripcion
  DROP CONSTRAINT IF EXISTS inscripcion_estado_check;

ALTER TABLE inscripcion
  ADD CONSTRAINT inscripcion_estado_check
  CHECK (estado IN ('PENDIENTE','ACEPTADO','RECHAZADO','CANCELADO'));

-- 3) Índices
CREATE INDEX IF NOT EXISTS idx_inscripcion_estado       ON inscripcion(estado);
CREATE INDEX IF NOT EXISTS idx_inscripcion_created_at   ON inscripcion(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_inscripcion_updated_at   ON inscripcion(updated_at DESC);

-- 4) Renombrar/normalizar constraint único
ALTER TABLE inscripcion
  DROP CONSTRAINT IF EXISTS inscripcion_partido_id_usuario_id_key;

ALTER TABLE inscripcion
  ADD CONSTRAINT uk_inscripcion_partido_usuario UNIQUE (partido_id, usuario_id);

-- 5) Comentarios
COMMENT ON COLUMN inscripcion.comentario        IS 'Comentario opcional del usuario al inscribirse';
COMMENT ON COLUMN inscripcion.motivo_rechazo    IS 'Motivo del rechazo si la inscripción fue rechazada';
COMMENT ON COLUMN inscripcion.updated_at        IS 'Fecha de última actualización de la inscripción';
COMMENT ON COLUMN inscripcion.fecha_aceptacion  IS 'Fecha en que la inscripción fue aceptada';
COMMENT ON COLUMN inscripcion.fecha_rechazo     IS 'Fecha en que la inscripción fue rechazada';
COMMENT ON COLUMN inscripcion.fecha_cancelacion IS 'Fecha en que la inscripción fue cancelada por el usuario';
COMMENT ON COLUMN inscripcion.estado            IS 'Estado: PENDIENTE, ACEPTADO, RECHAZADO, CANCELADO';

-- 6) Trigger updated_at
CREATE OR REPLACE FUNCTION update_inscripcion_updated_at()
RETURNS TRIGGER
AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_inscripcion_updated_at ON inscripcion;

CREATE TRIGGER trigger_inscripcion_updated_at
BEFORE UPDATE ON inscripcion
FOR EACH ROW
EXECUTE FUNCTION update_inscripcion_updated_at();

-- 7) Trigger para setear fechas según el estado
CREATE OR REPLACE FUNCTION set_inscripcion_fecha_estado()
RETURNS TRIGGER
AS $$
BEGIN
  IF NEW.estado = 'ACEPTADO' AND OLD.estado <> 'ACEPTADO' AND NEW.fecha_aceptacion IS NULL THEN
    NEW.fecha_aceptacion = NOW();
  END IF;

  IF NEW.estado = 'RECHAZADO' AND OLD.estado <> 'RECHAZADO' AND NEW.fecha_rechazo IS NULL THEN
    NEW.fecha_rechazo = NOW();
  END IF;

  IF NEW.estado = 'CANCELADO' AND OLD.estado <> 'CANCELADO' AND NEW.fecha_cancelacion IS NULL THEN
    NEW.fecha_cancelacion = NOW();
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_inscripcion_fecha_estado ON inscripcion;

CREATE TRIGGER trigger_inscripcion_fecha_estado
BEFORE UPDATE OF estado ON inscripcion
FOR EACH ROW
EXECUTE FUNCTION set_inscripcion_fecha_estado();

-- 8) Vistas
CREATE OR REPLACE VIEW v_inscripciones_estadisticas AS
SELECT 
  p.id AS partido_id,
  p.tipo_partido,
  p.fecha,
  p.hora,
  p.nombre_ubicacion,
  COUNT(*) FILTER (WHERE i.estado = 'PENDIENTE') AS pendientes,
  COUNT(*) FILTER (WHERE i.estado = 'ACEPTADO') AS aceptados,
  COUNT(*) FILTER (WHERE i.estado = 'RECHAZADO') AS rechazados,
  COUNT(*) FILTER (WHERE i.estado = 'CANCELADO') AS cancelados,
  COUNT(*) AS total
FROM partido p
LEFT JOIN inscripcion i ON p.id = i.partido_id
GROUP BY p.id, p.tipo_partido, p.fecha, p.hora, p.nombre_ubicacion;

COMMENT ON VIEW v_inscripciones_estadisticas IS 'Vista con estadísticas de inscripciones por partido';

CREATE OR REPLACE VIEW v_inscripciones_activas AS
SELECT 
  i.id,
  i.partido_id,
  i.usuario_id,
  i.estado,
  i.comentario,
  i.created_at,
  i.updated_at,
  u.nombre  AS usuario_nombre,
  u.apellido AS usuario_apellido,
  u.email   AS usuario_email,
  p.tipo_partido,
  p.fecha AS partido_fecha,
  p.hora  AS partido_hora,
  p.nombre_ubicacion
FROM inscripcion i
JOIN usuario u ON i.usuario_id = u.id
JOIN partido p ON i.partido_id = p.id
WHERE i.estado IN ('PENDIENTE','ACEPTADO')
ORDER BY i.created_at DESC;

COMMENT ON VIEW v_inscripciones_activas IS 'Vista de inscripciones activas (pendientes o aceptadas)';

-- 9) Índices compuestos
CREATE INDEX IF NOT EXISTS idx_inscripcion_partido_estado ON inscripcion(partido_id, estado);
CREATE INDEX IF NOT EXISTS idx_inscripcion_usuario_estado ON inscripcion(usuario_id, estado);

-- 10) Hint planner (opcional)
ANALYZE inscripcion;