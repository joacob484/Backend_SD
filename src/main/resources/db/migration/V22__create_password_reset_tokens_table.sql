-- Crear tabla de tokens de recuperación de contraseña
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(255) NOT NULL UNIQUE,
    usuario_id UUID NOT NULL,
    expira_en TIMESTAMP NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_password_reset_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON DELETE CASCADE
);

-- Índices para mejorar performance
CREATE INDEX idx_password_reset_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_usuario ON password_reset_tokens(usuario_id);
CREATE INDEX idx_password_reset_expira_en ON password_reset_tokens(expira_en);
CREATE INDEX idx_password_reset_usado ON password_reset_tokens(usado);

-- Índice compuesto para la query de cleanup
CREATE INDEX idx_password_reset_cleanup ON password_reset_tokens(expira_en, usado);

COMMENT ON TABLE password_reset_tokens IS 'Tokens de recuperación de contraseña (expiran en 1 hora)';
COMMENT ON COLUMN password_reset_tokens.token IS 'Token seguro de 256 bits (Base64 URL-safe)';
COMMENT ON COLUMN password_reset_tokens.expira_en IS 'Fecha de expiración (1 hora desde creación)';
COMMENT ON COLUMN password_reset_tokens.usado IS 'Indica si el token ya fue utilizado';
