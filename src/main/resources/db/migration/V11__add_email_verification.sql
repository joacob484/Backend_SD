-- ==================================================
-- V11: Agregar campos de verificación de email
-- ==================================================

-- Agregar columnas de verificación
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS verification_code VARCHAR(6);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS verification_code_expires_at TIMESTAMP;

-- Usuarios OAuth ya tienen email verificado
UPDATE usuario SET email_verified = TRUE WHERE provider = 'GOOGLE';

-- Índice para búsqueda por código de verificación
CREATE INDEX IF NOT EXISTS idx_usuario_verification_code 
ON usuario(verification_code) 
WHERE verification_code IS NOT NULL;

COMMENT ON COLUMN usuario.email_verified IS 'Indica si el email del usuario ha sido verificado';
COMMENT ON COLUMN usuario.verification_code IS 'Código de verificación de 6 dígitos enviado por email';
COMMENT ON COLUMN usuario.verification_code_expires_at IS 'Fecha de expiración del código de verificación (15 minutos)';
