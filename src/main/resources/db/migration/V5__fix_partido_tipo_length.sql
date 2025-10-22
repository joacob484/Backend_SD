-- Fix partido tipo_partido column length to match entity definition
ALTER TABLE partido ALTER COLUMN tipo_partido TYPE VARCHAR(20);
