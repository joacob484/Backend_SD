-- Fix partido tipo_partido column length to match entity definition
-- Cannot alter column type directly because views depend on it, so we need to:
-- 1. Drop the dependent views
-- 2. Alter the column type
-- 3. Recreate the views

-- Drop views that depend on tipo_partido
DROP VIEW IF EXISTS v_inscripciones_estadisticas;
DROP VIEW IF EXISTS v_inscripciones_activas;

-- Alter column type
ALTER TABLE partido ALTER COLUMN tipo_partido TYPE VARCHAR(20);

-- Recreate v_inscripciones_estadisticas view
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

-- Recreate v_inscripciones_activas view
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
  AND p.fecha >= CURRENT_DATE;

COMMENT ON VIEW v_inscripciones_activas IS 'Vista de inscripciones activas con información del usuario y partido';
