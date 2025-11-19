-- Migration V10: Eliminar funcionalidad de verificación de celular
-- Fecha: 2025-11-19
-- Descripción: Elimina todas las columnas relacionadas con celular y su verificación

-- 1. Eliminar columnas de verificación de celular
ALTER TABLE usuario 
    DROP COLUMN IF EXISTS celular_verificado,
    DROP COLUMN IF EXISTS codigo_verificacion,
    DROP COLUMN IF EXISTS codigo_verificacion_expira,
    DROP COLUMN IF EXISTS codigo_verificacion_intentos;

-- 2. Eliminar columna de número de celular
ALTER TABLE usuario 
    DROP COLUMN IF EXISTS celular;

-- 3. Agregar comentario de auditoría
COMMENT ON TABLE usuario IS 'Usuarios del sistema - Eliminada funcionalidad de celular en V10 (2025-11-19)';
