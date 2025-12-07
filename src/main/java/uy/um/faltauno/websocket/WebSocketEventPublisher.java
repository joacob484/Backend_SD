package uy.um.faltauno.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import uy.um.faltauno.dto.InscripcionDTO;
import uy.um.faltauno.dto.PartidoDTO;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para publicar eventos WebSocket a clientes conectados
 * 
 * Uso:
 * - Inyectar en services que modifican partidos/inscripciones
 * - Llamar métodos notify* después de actualizar BD
 * - Clientes suscritos recibirán actualizaciones instantáneas
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notificar actualización de partido a todos los usuarios viendo ese partido
     */
    public void notifyPartidoUpdated(String partidoId, PartidoDTO partido) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "PARTIDO_UPDATED");
            payload.put("partidoId", partidoId);
            payload.put("partido", partido);
            payload.put("timestamp", System.currentTimeMillis());

            String destination = "/topic/partidos/" + partidoId;
            messagingTemplate.convertAndSend(destination, payload);
            
            log.info("[WebSocket] Notificado partido actualizado: {} a {}", partidoId, destination);
        } catch (Exception e) {
            log.error("[WebSocket] Error notificando partido actualizado", e);
        }
    }

    /**
     * Notificar nueva inscripción a partido
     */
    public void notifyInscripcionCreated(String partidoId, InscripcionDTO inscripcion) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "INSCRIPCION_CREATED");
            payload.put("partidoId", partidoId);
            payload.put("inscripcion", inscripcion);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/partidos/" + partidoId, payload);
            
            log.info("[WebSocket] Notificada nueva inscripción en partido: {}", partidoId);
        } catch (Exception e) {
            log.error("[WebSocket] Error notificando inscripción creada", e);
        }
    }

    /**
     * Notificar cambio en estado de inscripción (aceptada/rechazada)
     */
    public void notifyInscripcionStatusChanged(String partidoId, InscripcionDTO inscripcion, String newStatus) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "INSCRIPCION_STATUS_CHANGED");
            payload.put("partidoId", partidoId);
            payload.put("inscripcion", inscripcion);
            payload.put("newStatus", newStatus);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/partidos/" + partidoId, payload);
            
            log.info("[WebSocket] Notificado cambio de estado en inscripción: {} - {}", partidoId, newStatus);
        } catch (Exception e) {
            log.error("[WebSocket] Error notificando cambio de estado", e);
        }
    }

    /**
     * Notificar cancelación de inscripción
     */
    public void notifyInscripcionCancelled(String partidoId, String inscripcionId, String userId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "INSCRIPCION_CANCELLED");
            payload.put("partidoId", partidoId);
            payload.put("inscripcionId", inscripcionId);
            payload.put("userId", userId);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/partidos/" + partidoId, payload);
            
            log.info("[WebSocket] Notificada cancelación de inscripción en partido: {}", partidoId);
        } catch (Exception e) {
            log.error("[WebSocket] Error notificando cancelación", e);
        }
    }

    /**
     * Notificar partido cancelado
     */
    public void notifyPartidoCancelled(String partidoId, String reason) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "PARTIDO_CANCELLED");
            payload.put("partidoId", partidoId);
            payload.put("reason", reason);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/partidos/" + partidoId, payload);
            
            log.info("[WebSocket] Notificado partido cancelado: {}", partidoId);
        } catch (Exception e) {
            log.error("[WebSocket] Error notificando partido cancelado", e);
        }
    }

    /**
     * Notificar partido completado
     */
    public void notifyPartidoCompleted(String partidoId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "PARTIDO_COMPLETED");
            payload.put("partidoId", partidoId);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/partidos/" + partidoId, payload);
            
            log.info("[WebSocket] Notificado partido completado: {}", partidoId);
        } catch (Exception e) {
            log.error("[WebSocket] Error notificando partido completado", e);
        }
    }

    /**
     * Notificar a usuario específico (notificación personal)
     */
    public void notifyUser(String userId, String type, Object data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", type);
            payload.put("data", data);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", payload);
            
            log.info("[WebSocket] Notificación enviada a usuario: {} - {}", userId, type);
        } catch (Exception e) {
            log.error("[WebSocket] Error notificando usuario", e);
        }
    }

    /**
     * Notificar nuevo mensaje en chat de partido
     */
    public void notifyNewMessage(String partidoId, Object mensaje) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "NEW_MESSAGE");
            payload.put("partidoId", partidoId);
            payload.put("mensaje", mensaje);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/partidos/" + partidoId + "/chat", payload);
            
            log.info("[WebSocket] Nuevo mensaje notificado en partido: {}", partidoId);
        } catch (Exception e) {
            log.error("[WebSocket] Error notificando mensaje", e);
        }
    }

    /**
     * Notificar que un usuario está escribiendo
     * ✅ FIX: No enviar el evento al usuario que está escribiendo (solo a los demás)
     */
    public void notifyTyping(String partidoId, String userId, String userName, boolean isTyping) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "USER_TYPING");
            payload.put("partidoId", partidoId);
            payload.put("userId", userId);
            payload.put("userName", userName);
            payload.put("isTyping", isTyping);
            payload.put("timestamp", System.currentTimeMillis());

            // ✅ FIX: Usar broadcasting pero el frontend filtrará por userId
            // Spring WebSocket no tiene "send to all except" nativo, así que
            // enviamos a todos y el frontend descarta si userId === currentUser.id
            messagingTemplate.convertAndSend("/topic/partidos/" + partidoId + "/chat", payload);
            
            log.debug("[WebSocket] Typing event sent for user {} in partido {}", userName, partidoId);
        } catch (Exception e) {
            log.error("[WebSocket] Error notificando typing", e);
        }
    }

    /**
     * Notificar creación de partido a todos los usuarios (lista global)
     */
    public void notifyPartidoCreated(PartidoDTO partido) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "PARTIDO_CREATED");
            payload.put("partido", partido);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/partidos", payload);
            
            log.info("[WebSocket] Notificado partido creado: {}", partido.getId());
        } catch (Exception e) {
            log.error("[WebSocket] Error notificando partido creado", e);
        }
    }

    /**
     * Notificar cancelación de partido a todos los usuarios (lista global)
     */
    public void notifyPartidoCancelledGlobal(String partidoId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "PARTIDO_CANCELLED_GLOBAL");
            payload.put("partidoId", partidoId);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/partidos", payload);
            
            log.info("[WebSocket] Notificado partido cancelado globalmente: {}", partidoId);
        } catch (Exception e) {
            log.error("[WebSocket] Error notificando partido cancelado global", e);
        }
    }

    /**
     * Notificar nueva solicitud de amistad al receptor
     */
    public void notifyFriendRequest(String userId, Object solicitud) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "FRIEND_REQUEST_RECEIVED");
            payload.put("solicitud", solicitud);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", payload);
            
            log.info("[WebSocket] Notificada nueva solicitud de amistad a usuario: {}", userId);
        } catch (Exception e) {
            log.error("[WebSocket] Error notificando solicitud de amistad", e);
        }
    }

    /**
     * Notificar solicitud de amistad aceptada al emisor
     */
    public void notifyFriendRequestAccepted(String userId, String friendId, String friendName) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "FRIEND_REQUEST_ACCEPTED");
            payload.put("friendId", friendId);
            payload.put("friendName", friendName);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", payload);
            
            log.info("[WebSocket] Notificada solicitud aceptada a usuario: {}", userId);
        } catch (Exception e) {
            log.error("[WebSocket] Error notificando solicitud aceptada", e);
        }
    }

    /**
     * Notificar nueva notificación al usuario (actualizar contador)
     */
    public void notifyNewNotification(String userId, Object notificacion) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "NEW_NOTIFICATION");
            payload.put("notificacion", notificacion);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", payload);
            
            log.info("[WebSocket] Notificada nueva notificación a usuario: {}", userId);
        } catch (Exception e) {
            log.error("[WebSocket] Error notificando nueva notificación", e);
        }
    }
}
