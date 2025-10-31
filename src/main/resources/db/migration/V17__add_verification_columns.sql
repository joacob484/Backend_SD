-- Add email verification columns to usuario table
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS verification_code VARCHAR(6);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS verification_code_expires_at TIMESTAMP;

-- Mark all OAuth users as verified
UPDATE usuario 
SET email_verified = TRUE 
WHERE provider = 'GOOGLE' AND email_verified = FALSE;

-- Create index for verification code lookups
CREATE INDEX IF NOT EXISTS idx_usuario_verification_code 
ON usuario(verification_code) 
WHERE verification_code IS NOT NULL;
