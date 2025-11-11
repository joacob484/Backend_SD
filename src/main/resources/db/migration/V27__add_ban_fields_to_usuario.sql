-- Add ban-related fields to usuario table
-- These fields track if a user has been banned and why

ALTER TABLE usuario
ADD COLUMN IF NOT EXISTS banned_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS ban_reason TEXT,
ADD COLUMN IF NOT EXISTS banned_by UUID;

-- Add index for quick ban status checks
CREATE INDEX IF NOT EXISTS idx_usuario_banned_at ON usuario(banned_at) WHERE banned_at IS NOT NULL;

-- Add foreign key for banned_by (references another user - the admin who banned)
-- Note: PostgreSQL doesn't support IF NOT EXISTS for constraints, so we check first
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_usuario_banned_by'
    ) THEN
        ALTER TABLE usuario
        ADD CONSTRAINT fk_usuario_banned_by FOREIGN KEY (banned_by) REFERENCES usuario(id);
    END IF;
END $$;

COMMENT ON COLUMN usuario.banned_at IS 'Timestamp when user was banned, NULL if not banned';
COMMENT ON COLUMN usuario.ban_reason IS 'Reason provided by admin for banning this user';
COMMENT ON COLUMN usuario.banned_by IS 'UUID of admin user who issued the ban';
