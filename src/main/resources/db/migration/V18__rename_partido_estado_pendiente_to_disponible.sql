-- V18: Cambiar estado de partido de PENDIENTE a DISPONIBLE
-- Esto evita confusión con InscripcionEstado.PENDIENTE

-- Actualizar todos los partidos existentes que tienen estado PENDIENTE
UPDATE partido
SET estado = 'DISPONIBLE'
WHERE estado = 'PENDIENTE'
  AND deleted_at IS NULL; -- Solo partidos activos

-- Nota: Los partidos con soft-delete (deleted_at IS NOT NULL) también se actualizan
-- para mantener consistencia histórica
UPDATE partido
SET estado = 'DISPONIBLE'
WHERE estado = 'PENDIENTE'
  AND deleted_at IS NOT NULL;

-- Verificar que no quedan partidos con estado PENDIENTE, ABIERTO o ACTIVO
-- (estos estados ya no se usan en el código)
