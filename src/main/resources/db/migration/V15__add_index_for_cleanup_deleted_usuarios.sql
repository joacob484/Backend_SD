-- ======================================
-- V15: Índice para cleanup automático de usuarios eliminados
-- ======================================
-- Índice para encontrar rápidamente usuarios eliminados hace más de 30 días
-- que deben ser eliminados físicamente

CREATE INDEX idx_usuario_deleted_at_cleanup 
  ON usuario(deleted_at) 
  WHERE deleted_at IS NOT NULL;

COMMENT ON INDEX idx_usuario_deleted_at_cleanup IS 'Índice para cleanup automático de usuarios eliminados hace >30 días';
