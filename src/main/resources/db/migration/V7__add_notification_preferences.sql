-- V7__add_notification_preferences.sql
-- Añade campos de preferencias de notificaciones por email al usuario

-- Agregar columnas de preferencias de notificación
ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS notif_email_invitaciones BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS notif_email_solicitudes_amistad BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS notif_email_actualizaciones_partido BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS notif_email_solicitudes_review BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS notif_email_nuevos_mensajes BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS notif_email_generales BOOLEAN DEFAULT false;

-- Comentarios para documentar el propósito de cada columna
COMMENT ON COLUMN usuario.notif_email_invitaciones IS 'Recibir emails por invitaciones a partidos';
COMMENT ON COLUMN usuario.notif_email_solicitudes_amistad IS 'Recibir emails por solicitudes de amistad';
COMMENT ON COLUMN usuario.notif_email_actualizaciones_partido IS 'Recibir emails por actualizaciones de partidos';
COMMENT ON COLUMN usuario.notif_email_solicitudes_review IS 'Recibir emails para solicitudes de reseñas';
COMMENT ON COLUMN usuario.notif_email_nuevos_mensajes IS 'Recibir emails por nuevos mensajes';
COMMENT ON COLUMN usuario.notif_email_generales IS 'Recibir emails de actualizaciones generales';

-- Índice para consultas de preferencias de notificación
CREATE INDEX IF NOT EXISTS idx_usuario_email_notif ON usuario(id) 
    WHERE notif_email_invitaciones = true 
       OR notif_email_solicitudes_amistad = true 
       OR notif_email_actualizaciones_partido = true;
