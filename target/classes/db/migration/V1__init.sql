CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE usuario (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nombre VARCHAR(100) NOT NULL,
  email  VARCHAR(120) UNIQUE NOT NULL,
  telefono VARCHAR(30),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cancha (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255),
    direccion VARCHAR(255),
    lat NUMERIC,
    lng NUMERIC
);


CREATE TABLE partido (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  cancha_id BIGINT NOT NULL REFERENCES cancha(id)
  fecha_hora TIMESTAMPTZ NOT NULL,
  cupo_total INT NOT NULL,
  cupo_actual INT NOT NULL DEFAULT 0,
  estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
  created_by UUID NOT NULL REFERENCES usuario(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE inscripcion (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  partido_id UUID NOT NULL REFERENCES partido(id) ON DELETE CASCADE,
  usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (partido_id, usuario_id)
);

CREATE TABLE rating (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  partido_id UUID NOT NULL REFERENCES partido(id) ON DELETE CASCADE,
  rater_id UUID NOT NULL REFERENCES usuario(id),
  rated_user_id UUID NOT NULL REFERENCES usuario(id),
  score INT NOT NULL CHECK (score BETWEEN 1 AND 5),
  comentario VARCHAR(300),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (partido_id, rater_id, rated_user_id)
);
