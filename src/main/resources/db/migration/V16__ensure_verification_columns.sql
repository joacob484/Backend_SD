-- ==================================================
-- V16: Asegurar que existan columnas de verificación
-- ==================================================
-- Esta migración es idempotente y se puede ejecutar múltiples veces

-- Agregar columnas de verificación si no existen
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS verification_code VARCHAR(6);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS verification_code_expires_at TIMESTAMP;

-- Usuarios OAuth ya tienen email verificado
UPDATE usuario SET email_verified = TRUE WHERE provider = 'GOOGLE' AND email_verified = FALSE;

-- Índice para búsqueda por código de verificación
CREATE INDEX IF NOT EXISTS idx_usuario_verification_code 
ON usuario(verification_code) 
WHERE verification_code IS NOT NULL;
