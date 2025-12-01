-- V35: Ensure phone verification columns are completely removed
-- This is idempotent and safe to run multiple times
-- Handles the case where V34 may not have been applied

-- Drop phone-related columns if they still exist
ALTER TABLE usuario 
    DROP COLUMN IF EXISTS celular_verificado,
    DROP COLUMN IF EXISTS codigo_verificacion,
    DROP COLUMN IF EXISTS codigo_verificacion_expira,
    DROP COLUMN IF EXISTS codigo_verificacion_intentos,
    DROP COLUMN IF EXISTS celular;

-- Drop indexes if they exist
DROP INDEX IF EXISTS idx_usuario_codigo_verificacion;
DROP INDEX IF EXISTS idx_usuario_codigo_expira;

-- Update comment
COMMENT ON TABLE usuario IS 'Usuarios del sistema - Phone verification completely removed in V35 (2025-12-01)';
