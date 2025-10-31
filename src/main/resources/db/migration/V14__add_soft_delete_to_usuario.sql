-- ======================================
-- V14: Soft Delete para Usuario
-- ======================================
-- En lugar de eliminar físicamente usuarios (lo cual rompe foreign keys),
-- agregamos un campo deleted_at para "marcar" usuarios como eliminados.
-- Esto preserva integridad referencial y permite auditoría.

-- Agregar campo deleted_at (soft delete)
ALTER TABLE usuario 
  ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;

-- Índice para filtrar usuarios activos (WHERE deleted_at IS NULL)
CREATE INDEX idx_usuario_deleted_at ON usuario(deleted_at) WHERE deleted_at IS NULL;

-- Comentario para documentación
COMMENT ON COLUMN usuario.deleted_at IS 'Timestamp de eliminación lógica. NULL = usuario activo, NOT NULL = usuario eliminado';
