-- V23: Crear tabla de contactos importados
CREATE TABLE contactos (
    id BIGSERIAL PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nombre VARCHAR(255) NOT NULL,
    apellido VARCHAR(255),
    celular VARCHAR(50) NOT NULL,
    usuario_app_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    is_on_app BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT contactos_usuario_celular_unique UNIQUE (usuario_id, celular)
);

-- Índices para mejorar rendimiento
CREATE INDEX idx_contactos_usuario_id ON contactos(usuario_id);
CREATE INDEX idx_contactos_celular ON contactos(celular);
CREATE INDEX idx_contactos_usuario_app_id ON contactos(usuario_app_id) WHERE usuario_app_id IS NOT NULL;
CREATE INDEX idx_contactos_is_on_app ON contactos(is_on_app) WHERE is_on_app = TRUE;

-- Comentarios
COMMENT ON TABLE contactos IS 'Contactos importados del dispositivo del usuario';
COMMENT ON COLUMN contactos.usuario_id IS 'Usuario dueño del contacto';
COMMENT ON COLUMN contactos.nombre IS 'Nombre del contacto desde el teléfono';
COMMENT ON COLUMN contactos.apellido IS 'Apellido del contacto desde el teléfono';
COMMENT ON COLUMN contactos.celular IS 'Número de teléfono normalizado';
COMMENT ON COLUMN contactos.usuario_app_id IS 'Usuario de la app si el contacto está registrado';
COMMENT ON COLUMN contactos.is_on_app IS 'Indica si el contacto está registrado en la app';
