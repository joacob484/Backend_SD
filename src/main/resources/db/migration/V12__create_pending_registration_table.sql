-- ==================================================
-- V12: Crear tabla para registros pendientes de verificación
-- ==================================================

-- Tabla para almacenar pre-registros antes de verificar email
CREATE TABLE IF NOT EXISTS pending_registration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    verification_code VARCHAR(6) NOT NULL,
    verification_code_expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_verification_code CHECK (verification_code ~ '^[0-9]{6}$')
);

-- Índice para búsqueda rápida por email
CREATE INDEX IF NOT EXISTS idx_pending_registration_email ON pending_registration(email);

-- Índice para búsqueda por código
CREATE INDEX IF NOT EXISTS idx_pending_registration_code ON pending_registration(verification_code);

-- Índice para limpiar registros expirados
CREATE INDEX IF NOT EXISTS idx_pending_registration_expires ON pending_registration(verification_code_expires_at);

-- Auto-limpieza de registros expirados (ejecutar periódicamente)
-- DELETE FROM pending_registration WHERE verification_code_expires_at < NOW() - INTERVAL '24 hours';

COMMENT ON TABLE pending_registration IS 'Almacena pre-registros pendientes de verificación de email';
COMMENT ON COLUMN pending_registration.email IS 'Email del usuario (temporal hasta confirmar)';
COMMENT ON COLUMN pending_registration.password_hash IS 'Contraseña encriptada (temporal)';
COMMENT ON COLUMN pending_registration.verification_code IS 'Código de 6 dígitos enviado por email';
COMMENT ON COLUMN pending_registration.verification_code_expires_at IS 'Fecha de expiración del código (15 minutos)';

-- Limpiar campos de verificación de la tabla usuario (ya no se usan)
-- Ahora la verificación ocurre ANTES de crear el usuario
ALTER TABLE usuario DROP COLUMN IF EXISTS verification_code;
ALTER TABLE usuario DROP COLUMN IF EXISTS verification_code_expires_at;
-- email_verified se mantiene para usuarios OAuth
