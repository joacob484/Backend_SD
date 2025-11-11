-- Add ban_until field to usuario table for temporary bans
ALTER TABLE usuario
ADD COLUMN IF NOT EXISTS ban_until TIMESTAMP;

COMMENT ON COLUMN usuario.ban_until IS 'Fecha hasta la cual el usuario está baneado (NULL = baneo permanente)';

-- Index for checking if ban has expired
CREATE INDEX IF NOT EXISTS idx_usuario_ban_until ON usuario(ban_until) WHERE ban_until IS NOT NULL;
