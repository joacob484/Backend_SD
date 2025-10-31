-- V13: Agregar campo last_activity_at para tracking de sesión activa
-- Fecha: 2025-10-31
-- Autor: Sistema
-- Descripción: Permite calcular "Usuarios activos ahora" en tiempo real

ALTER TABLE usuario
ADD COLUMN last_activity_at TIMESTAMP;

-- Inicializar con created_at para usuarios existentes
UPDATE usuario
SET last_activity_at = created_at
WHERE last_activity_at IS NULL;

-- Crear índice para optimizar consultas de usuarios activos
CREATE INDEX idx_usuario_last_activity 
ON usuario(last_activity_at) 
WHERE last_activity_at IS NOT NULL;

-- Comentarios
COMMENT ON COLUMN usuario.last_activity_at IS 'Última actividad del usuario (actualizada por middleware en cada request autenticado)';
COMMENT ON INDEX idx_usuario_last_activity IS 'Optimiza consultas de usuarios activos en ventana de tiempo';
