-- V26: Agregar rol a usuarios para sistema de administración
ALTER TABLE usuario ADD COLUMN rol VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Crear índice para mejorar queries por rol
CREATE INDEX idx_usuario_rol ON usuario(rol);

-- Agregar constraint para validar roles permitidos
ALTER TABLE usuario ADD CONSTRAINT check_rol CHECK (rol IN ('USER', 'ADMIN'));

-- Comentarios
COMMENT ON COLUMN usuario.rol IS 'Rol del usuario: USER (normal) o ADMIN (administrador)';
