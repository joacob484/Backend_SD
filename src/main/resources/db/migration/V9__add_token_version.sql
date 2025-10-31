-- V9: Agregar token_version para invalidación de JWT (estándar industria)
-- Permite invalidar todos los tokens de un usuario sin afectar a otros usuarios

ALTER TABLE usuario 
ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 1;

-- Crear índice para queries rápidas por token_version
CREATE INDEX IF NOT EXISTS idx_usuario_token_version ON usuario(token_version);

COMMENT ON COLUMN usuario.token_version IS 'Versión del token JWT. Se incrementa para invalidar todos los tokens existentes (cambio de contraseña, compromiso de seguridad). Estándar de la industria para token management.';
