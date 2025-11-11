-- V32: Agregar verificación de celular

-- Agregar columnas de verificación de teléfono
ALTER TABLE usuario 
ADD COLUMN celular_verificado BOOLEAN DEFAULT FALSE,
ADD COLUMN codigo_verificacion VARCHAR(6),
ADD COLUMN codigo_verificacion_expira TIMESTAMP,
ADD COLUMN codigo_verificacion_intentos INTEGER DEFAULT 0;

-- Índice para búsqueda rápida por código
CREATE INDEX idx_usuario_codigo_verificacion ON usuario(codigo_verificacion) 
WHERE codigo_verificacion IS NOT NULL;

-- Índice para limpiar códigos expirados
CREATE INDEX idx_usuario_codigo_expira ON usuario(codigo_verificacion_expira) 
WHERE codigo_verificacion_expira IS NOT NULL;

COMMENT ON COLUMN usuario.celular_verificado IS 'Indica si el número de celular fue verificado';
COMMENT ON COLUMN usuario.codigo_verificacion IS 'Código de 6 dígitos enviado por SMS para verificación';
COMMENT ON COLUMN usuario.codigo_verificacion_expira IS 'Timestamp de expiración del código (15 minutos)';
COMMENT ON COLUMN usuario.codigo_verificacion_intentos IS 'Contador de intentos fallidos de verificación (máx 3)';
