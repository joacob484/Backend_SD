-- Migración para corregir el tipo de ID de INTEGER a UUID en partido
-- MANTIENE LOS IDs EXISTENTES convirtiéndolos a UUIDs determinísticos

-- 1. Crear tabla de mapeo para convertir INTEGER IDs a UUIDs determinísticos
CREATE TEMP TABLE id_mapping AS
SELECT 
    id::TEXT as old_id_text,
    id as old_id_int,
    -- Genera UUID determinístico basado en el ID entero usando MD5
    -- Formato: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx donde y = 8,9,a,b
    (
        lpad(to_hex(id), 8, '0') || '-' ||
        lpad(to_hex(id), 4, '0') || '-' ||
        '4' || lpad(to_hex(id), 3, '0') || '-' ||
        '8' || lpad(to_hex(id), 3, '0') || '-' ||
        lpad(to_hex(id), 12, '0')
    )::UUID as new_id_uuid
FROM partido;

-- 2. Eliminar todas las foreign keys que apuntan a partido(id)
ALTER TABLE IF EXISTS inscripcion DROP CONSTRAINT IF EXISTS inscripcion_partido_id_fkey;
ALTER TABLE IF EXISTS review DROP CONSTRAINT IF EXISTS review_partido_id_fkey;
ALTER TABLE IF EXISTS mensaje DROP CONSTRAINT IF EXISTS mensaje_partido_id_fkey;
ALTER TABLE IF EXISTS jugadores_partido DROP CONSTRAINT IF EXISTS jugadores_partido_partido_id_fkey;

-- 3. Añadir columnas temporales para el mapeo en tablas relacionadas
ALTER TABLE IF EXISTS inscripcion ADD COLUMN IF NOT EXISTS partido_id_new UUID;
ALTER TABLE IF EXISTS review ADD COLUMN IF NOT EXISTS partido_id_new UUID;
ALTER TABLE IF EXISTS mensaje ADD COLUMN IF NOT EXISTS partido_id_new UUID;
ALTER TABLE IF EXISTS jugadores_partido ADD COLUMN IF NOT EXISTS partido_id_new UUID;

-- 4. Actualizar las columnas temporales con los nuevos UUIDs
UPDATE inscripcion i
SET partido_id_new = m.new_id_uuid
FROM id_mapping m
WHERE i.partido_id::TEXT = m.old_id_text;

UPDATE review r
SET partido_id_new = m.new_id_uuid
FROM id_mapping m
WHERE r.partido_id::TEXT = m.old_id_text;

UPDATE mensaje msg
SET partido_id_new = m.new_id_uuid
FROM id_mapping m
WHERE msg.partido_id::TEXT = m.old_id_text;

UPDATE jugadores_partido jp
SET partido_id_new = m.new_id_uuid
FROM id_mapping m
WHERE jp.partido_id::TEXT = m.old_id_text;

-- 5. Eliminar índices relacionados con partido.id
DROP INDEX IF EXISTS idx_inscripcion_partido;
DROP INDEX IF EXISTS idx_partido_fecha_hora;
DROP INDEX IF EXISTS idx_partido_ubicacion;

-- 6. Crear tabla temporal con estructura correcta (UUID)
CREATE TABLE partido_temp (
    id UUID PRIMARY KEY,
    tipo_partido VARCHAR(20) NOT NULL,
    genero VARCHAR(10) NOT NULL,
    nivel VARCHAR(20),
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    duracion_minutos INT NOT NULL DEFAULT 90,
    nombre_ubicacion VARCHAR(255) NOT NULL,
    direccion_ubicacion VARCHAR(255),
    latitud NUMERIC(18,10),
    longitud NUMERIC(18,10),
    cantidad_jugadores INT NOT NULL,
    precio_total NUMERIC(10,2) DEFAULT 0,
    descripcion VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    organizador_id UUID NOT NULL REFERENCES usuario(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 7. Migrar datos CON LOS NUEVOS UUIDs MAPEADOS
INSERT INTO partido_temp (
    id, tipo_partido, genero, nivel, fecha, hora, duracion_minutos,
    nombre_ubicacion, direccion_ubicacion, latitud, longitud,
    cantidad_jugadores, precio_total, descripcion, estado, organizador_id, created_at
)
SELECT 
    m.new_id_uuid,
    p.tipo_partido, p.genero, p.nivel, p.fecha, p.hora, p.duracion_minutos,
    p.nombre_ubicacion, p.direccion_ubicacion, p.latitud, p.longitud,
    p.cantidad_jugadores, p.precio_total, p.descripcion, 
    COALESCE(p.estado, 'PENDIENTE'), p.organizador_id, p.created_at
FROM partido p
INNER JOIN id_mapping m ON p.id::TEXT = m.old_id_text;

-- 8. Eliminar tabla antigua
DROP TABLE partido;

-- 9. Renombrar tabla temporal
ALTER TABLE partido_temp RENAME TO partido;

-- 10. Actualizar las columnas partido_id en tablas relacionadas
-- Inscripción
ALTER TABLE IF EXISTS inscripcion DROP COLUMN IF EXISTS partido_id CASCADE;
ALTER TABLE IF EXISTS inscripcion RENAME COLUMN partido_id_new TO partido_id;
ALTER TABLE IF EXISTS inscripcion ALTER COLUMN partido_id SET NOT NULL;

-- Review
ALTER TABLE IF EXISTS review DROP COLUMN IF EXISTS partido_id CASCADE;
ALTER TABLE IF EXISTS review RENAME COLUMN partido_id_new TO partido_id;
ALTER TABLE IF EXISTS review ALTER COLUMN partido_id SET NOT NULL;

-- Mensaje
ALTER TABLE IF EXISTS mensaje DROP COLUMN IF EXISTS partido_id CASCADE;
ALTER TABLE IF EXISTS mensaje RENAME COLUMN partido_id_new TO partido_id;

-- Jugadores_partido
ALTER TABLE IF EXISTS jugadores_partido DROP COLUMN IF EXISTS partido_id CASCADE;
ALTER TABLE IF EXISTS jugadores_partido RENAME COLUMN partido_id_new TO partido_id;
ALTER TABLE IF EXISTS jugadores_partido ALTER COLUMN partido_id SET NOT NULL;

-- 11. Recrear índices
CREATE INDEX idx_partido_fecha_hora ON partido(fecha, hora);
CREATE INDEX idx_partido_ubicacion ON partido(latitud, longitud);
CREATE INDEX idx_partido_organizador ON partido(organizador_id);

-- 12. Recrear foreign keys en otras tablas

-- Inscripción
ALTER TABLE IF EXISTS inscripcion 
    ADD CONSTRAINT inscripcion_partido_id_fkey 
    FOREIGN KEY (partido_id) REFERENCES partido(id) ON DELETE CASCADE;

CREATE INDEX idx_inscripcion_partido ON inscripcion(partido_id);

-- Review
ALTER TABLE IF EXISTS review 
    ADD CONSTRAINT review_partido_id_fkey 
    FOREIGN KEY (partido_id) REFERENCES partido(id) ON DELETE CASCADE;

-- Mensaje
ALTER TABLE IF EXISTS mensaje 
    ADD CONSTRAINT mensaje_partido_id_fkey 
    FOREIGN KEY (partido_id) REFERENCES partido(id);

-- Jugadores_partido (si existe)
ALTER TABLE IF EXISTS jugadores_partido 
    ADD CONSTRAINT jugadores_partido_partido_id_fkey 
    FOREIGN KEY (partido_id) REFERENCES partido(id) ON DELETE CASCADE;

-- Comentario en la columna
COMMENT ON COLUMN partido.id IS 'UUID único del partido (NO es INTEGER)';

-- Verificación
DO $$
BEGIN
    IF (SELECT data_type FROM information_schema.columns 
        WHERE table_name = 'partido' AND column_name = 'id') != 'uuid' THEN
        RAISE EXCEPTION 'ERROR: La columna partido.id no es de tipo UUID!';
    END IF;
    RAISE NOTICE 'OK: La columna partido.id es de tipo UUID correctamente';
END $$;
