-- V30: Crear tabla chat_visits para tracking de mensajes no leídos

CREATE TABLE chat_visits (
    id BIGSERIAL PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    partido_id UUID NOT NULL REFERENCES partido(id) ON DELETE CASCADE,
    last_visit_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_chat_visit_usuario_partido UNIQUE (usuario_id, partido_id)
);

-- Índices para optimizar consultas
CREATE INDEX idx_chat_visit_usuario ON chat_visits(usuario_id);
CREATE INDEX idx_chat_visit_partido ON chat_visits(partido_id);
CREATE INDEX idx_chat_visit_last_visit ON chat_visits(last_visit_at);

COMMENT ON TABLE chat_visits IS 'Registra última visita de usuario a chat de partido para calcular mensajes no leídos';
COMMENT ON COLUMN chat_visits.last_visit_at IS 'Timestamp de última vez que usuario abrió el chat';
