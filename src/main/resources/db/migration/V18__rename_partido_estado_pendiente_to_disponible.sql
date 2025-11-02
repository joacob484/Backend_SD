-- V18: Cambiar estado de partido de PENDIENTE a DISPONIBLE
-- Esto evita confusión con InscripcionEstado.PENDIENTE

-- PASO 1: Eliminar el constraint PRIMERO (el viejo no permite DISPONIBLE)
ALTER TABLE partido DROP CONSTRAINT IF EXISTS partido_estado_check;

-- PASO 2: Actualizar todos los partidos de PENDIENTE → DISPONIBLE
UPDATE partido
SET estado = 'DISPONIBLE'
WHERE estado = 'PENDIENTE';

-- PASO 3: Crear nuevo constraint que incluye DISPONIBLE (sin PENDIENTE)
ALTER TABLE partido
  ADD CONSTRAINT partido_estado_check
  CHECK (estado IN ('DISPONIBLE','CONFIRMADO','CANCELADO','COMPLETADO'));
