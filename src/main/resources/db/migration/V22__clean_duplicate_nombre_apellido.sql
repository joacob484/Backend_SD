-- ======================================
-- V21: Limpiar nombres y apellidos duplicados
-- ======================================
-- Algunos usuarios tienen nombre y apellido duplicados en el campo nombre
-- Por ejemplo: nombre="Juan Pérez", apellido="Pérez"
-- Este script detecta y limpia esos casos

-- Temporal table para debug (opcional, comentar si no se necesita)
-- SELECT id, nombre, apellido, 
--        CASE 
--          WHEN POSITION(CONCAT(' ', apellido) IN nombre) > 0 THEN 'DUPLICADO'
--          ELSE 'OK'
--        END as estado
-- FROM usuario 
-- WHERE apellido IS NOT NULL 
--   AND apellido != ''
--   AND deleted_at IS NULL;

-- Limpiar casos donde nombre contiene apellido al final
-- Ejemplo: nombre="Juan Pérez" y apellido="Pérez" → nombre="Juan"
UPDATE usuario
SET nombre = TRIM(
    CASE 
        -- Si el nombre termina con espacio + apellido, quitarlo
        WHEN nombre LIKE CONCAT('% ', apellido) THEN 
            LEFT(nombre, LENGTH(nombre) - LENGTH(apellido) - 1)
        -- Si el nombre es exactamente "algo apellido", quedarse con "algo"
        WHEN POSITION(CONCAT(' ', apellido) IN nombre) > 0 THEN
            LEFT(nombre, POSITION(CONCAT(' ', apellido) IN nombre) - 1)
        ELSE nombre
    END
)
WHERE deleted_at IS NULL
  AND apellido IS NOT NULL 
  AND apellido != ''
  AND (
      nombre LIKE CONCAT('% ', apellido)
      OR POSITION(CONCAT(' ', apellido) IN nombre) > 0
  );

-- Comentario para auditoría
COMMENT ON COLUMN usuario.nombre IS 'Nombre del usuario (sin apellido)';
COMMENT ON COLUMN usuario.apellido IS 'Apellido del usuario';
