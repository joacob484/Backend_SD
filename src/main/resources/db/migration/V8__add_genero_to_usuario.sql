-- V8: Add genero column to usuario table
-- This migration adds a gender field to support user profile information

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS genero VARCHAR(20);

-- Add comment for documentation
COMMENT ON COLUMN usuario.genero IS 'Género del usuario: Masculino, Femenino, Otro';
