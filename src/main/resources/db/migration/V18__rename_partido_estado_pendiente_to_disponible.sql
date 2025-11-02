-- V18: Cambiar estado de partido de PENDIENTE a DISPONIBLE
-- Esto evita confusión con InscripcionEstado.PENDIENTE

-- Actualizar todos los partidos existentes que tienen estado PENDIENTE
-- (incluyendo soft-deleted para consistencia histórica)
UPDATE partido
SET estado = 'DISPONIBLE'
WHERE estado = 'PENDIENTE';
