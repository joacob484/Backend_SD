-- Agregar campo ultima_edicion a tabla partido para tracking de cooldown de confirmación
-- Este campo se actualiza cada vez que se edita un partido
-- Se usa para validar que debe pasar 1 hora desde la última edición antes de confirmar

ALTER TABLE partido 
ADD COLUMN ultima_edicion TIMESTAMP;

-- Agregar índice para mejorar performance en queries de cooldown
CREATE INDEX idx_partido_ultima_edicion ON partido(ultima_edicion);

-- Comentario en la tabla
COMMENT ON COLUMN partido.ultima_edicion IS 'Timestamp de la última edición del partido. Se usa para validar cooldown de 1 hora antes de confirmar';
