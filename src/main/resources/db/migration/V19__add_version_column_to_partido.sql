-- V19: Agregar columna version para optimistic locking en tabla partido
-- Necesaria para @Version annotation en Partido.java

ALTER TABLE partido 
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- Inicializar la columna version en 0 para todos los registros existentes
UPDATE partido SET version = 0 WHERE version IS NULL;
