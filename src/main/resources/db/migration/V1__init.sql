-- V2__init.sql
-- Extensiones necesarias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Tabla usuario
CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100),
    apellido VARCHAR(100),
    email VARCHAR(120) UNIQUE,
    password VARCHAR(255),
    edad INT,
    ubicacion VARCHAR(255),
    celular VARCHAR(30),
    altura NUMERIC(5,2),
    peso NUMERIC(5,2),
    posicion VARCHAR(50), -- ARQUERO, ZAGUERO, LATERAL, MEDIOCAMPISTA, VOLANTE, DELANTERO
    foto_perfil VARCHAR(255),
    cedula VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tabla partido
CREATE TABLE partido (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_partido VARCHAR(5) NOT NULL,          -- F5, F7, F8, F9, F11
    genero VARCHAR(10) NOT NULL,               -- Mixto, Hombres, Mujeres
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    duracion_minutos INT NOT NULL,
    nombre_ubicacion VARCHAR(255) NOT NULL,
    direccion_ubicacion VARCHAR(255),
    latitud NUMERIC,
    longitud NUMERIC,
    cantidad_jugadores INT NOT NULL,
    precio_total NUMERIC(10,2) DEFAULT 0,
    descripcion VARCHAR(500),
    organizador_id UUID NOT NULL REFERENCES usuario(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_partido_fecha_hora ON partido(fecha, hora);
CREATE INDEX idx_partido_ubicacion ON partido(latitud, longitud);

-- Tabla inscripcion
CREATE TABLE inscripcion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partido_id UUID NOT NULL REFERENCES partido(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE', -- o CONFIRMADO según lógica
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (partido_id, usuario_id)
);

CREATE INDEX idx_inscripcion_partido ON inscripcion(partido_id);
CREATE INDEX idx_inscripcion_usuario ON inscripcion(usuario_id);

-- Tabla review (reseñas entre usuarios)
CREATE TABLE review (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partido_id UUID NOT NULL REFERENCES partido(id) ON DELETE CASCADE,
    usuario_que_califica_id UUID NOT NULL REFERENCES usuario(id),
    usuario_calificado_id UUID NOT NULL REFERENCES usuario(id),
    nivel INT NOT NULL CHECK (nivel BETWEEN 1 AND 5),
    deportividad INT NOT NULL CHECK (deportividad BETWEEN 1 AND 5),
    companerismo INT NOT NULL CHECK (companerismo BETWEEN 1 AND 5),
    comentario VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (partido_id, usuario_que_califica_id, usuario_calificado_id)
);

CREATE INDEX idx_review_usuario_calificado ON review(usuario_calificado_id);

CREATE TABLE amistad (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuario(id),
    amigo_id UUID NOT NULL REFERENCES usuario(id),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE, ACEPTADO, RECHAZADO
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(usuario_id, amigo_id)
);

CREATE TABLE mensaje (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    remitente_id UUID NOT NULL REFERENCES usuario(id),
    destinatario_id UUID NOT NULL REFERENCES usuario(id),
    partido_id UUID REFERENCES partido(id),
    contenido VARCHAR(500) NOT NULL,
    leido BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);