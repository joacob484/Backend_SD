-- ======================================
-- V22: Limpiar nombres y apellidos duplicados
-- ======================================
-- Algunos usuarios tienen nombre y apellido duplicados en el campo nombre
-- Por ejemplo: nombre="Juan Pérez", apellido="Pérez"
-- Este script detecta y limpia esos casos

-- Limpiar casos donde nombre contiene apellido al final
-- Ejemplo: nombre="Juan Pérez" y apellido="Pérez" → nombre="Juan"
UPDATE usuario
SET nombre = TRIM(
    SUBSTRING(
        nombre, 
        1, 
        POSITION(' ' || apellido IN nombre) - 1
    )
)
WHERE deleted_at IS NULL
  AND apellido IS NOT NULL 
  AND apellido != ''
  AND nombre IS NOT NULL
  AND POSITION(' ' || apellido IN nombre) > 0;

-- Comentario para auditoría
COMMENT ON COLUMN usuario.nombre IS 'Nombre del usuario (sin apellido)';
COMMENT ON COLUMN usuario.apellido IS 'Apellido del usuario';
