-- V18: Cambiar estado de partido de PENDIENTE a DISPONIBLE
-- Esto evita confusión con InscripcionEstado.PENDIENTE

-- PASO 1: Eliminar el constraint anterior
ALTER TABLE partido DROP CONSTRAINT IF EXISTS partido_estado_check;

-- PASO 2: Crear nuevo constraint que incluye DISPONIBLE (sin PENDIENTE)
ALTER TABLE partido
  ADD CONSTRAINT partido_estado_check
  CHECK (estado IN ('DISPONIBLE','CONFIRMADO','CANCELADO','COMPLETADO'));

-- PASO 3: Actualizar todos los partidos existentes que tienen estado PENDIENTE
-- (incluyendo soft-deleted para consistencia histórica)
UPDATE partido
SET estado = 'DISPONIBLE'
WHERE estado = 'PENDIENTE';
