-- V18: Cambiar estado de partido de PENDIENTE a DISPONIBLE
-- Esto evita confusión con InscripcionEstado.PENDIENTE

-- PASO 1: Actualizar PRIMERO todos los partidos de PENDIENTE → DISPONIBLE
-- (esto garantiza que no haya violaciones cuando creemos el nuevo constraint)
UPDATE partido
SET estado = 'DISPONIBLE'
WHERE estado = 'PENDIENTE';

-- PASO 2: Eliminar el constraint anterior
ALTER TABLE partido DROP CONSTRAINT IF EXISTS partido_estado_check;

-- PASO 3: Crear nuevo constraint que incluye DISPONIBLE (sin PENDIENTE)
ALTER TABLE partido
  ADD CONSTRAINT partido_estado_check
  CHECK (estado IN ('DISPONIBLE','CONFIRMADO','CANCELADO','COMPLETADO'));
