-- V20: Agregar columna version para optimistic locking en tabla inscripcion
-- Necesaria para @Version annotation en Inscripcion.java

ALTER TABLE inscripcion 
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- Inicializar la columna version en 0 para todos los registros existentes
UPDATE inscripcion SET version = 0 WHERE version IS NULL;
