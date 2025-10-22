-- V#__create_notificacion_table.sql
CREATE TABLE notificacion (
  id UUID PRIMARY KEY,
  usuario_id UUID NOT NULL,
  tipo VARCHAR(50) NOT NULL,
  titulo VARCHAR(255) NOT NULL,
  mensaje TEXT,
  entidad_id UUID,
  entidad_tipo VARCHAR(50),
  url_accion VARCHAR(500),
  leida BOOLEAN DEFAULT FALSE,
  fecha_lectura TIMESTAMP,
  prioridad VARCHAR(20) DEFAULT 'NORMAL',
  created_at TIMESTAMP NOT NULL,
  FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
CREATE INDEX idx_notificacion_usuario ON notificacion(usuario_id);
CREATE INDEX idx_notificacion_leida ON notificacion(leida);
CREATE INDEX idx_notificacion_created ON notificacion(created_at DESC);