package uy.um.faltauno.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import uy.um.faltauno.config.CustomUserDetailsService;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.UsuarioRepository;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador WebSocket para manejar mensajes del cliente
 * 
 * Los clientes envían mensajes a /app/* y el servidor los procesa aquí
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final WebSocketEventPublisher webSocketEventPublisher;
    private final UsuarioRepository usuarioRepository;

    /**
     * Manejar evento de "usuario está escribiendo"
     * Cliente envía a: /app/partidos/{partidoId}/typing
     */
    @MessageMapping("/partidos/{partidoId}/typing")
    public void handleTyping(
            @DestinationVariable String partidoId,
            @Payload Map<String, Object> payload,
            Principal principal
    ) {
        try {
            // Obtener info del usuario desde el principal
            String userId = extractUserId(principal);
            String userName = extractUserName(userId);
            
            boolean isTyping = (boolean) payload.getOrDefault("isTyping", false);
            
            log.debug("[WebSocket] Usuario {} está escribiendo: {}", userName, isTyping);
            
            // Broadcast a todos los usuarios del chat
            webSocketEventPublisher.notifyTyping(partidoId, userId, userName, isTyping);
            
        } catch (Exception e) {
            log.error("[WebSocket] Error procesando typing event", e);
        }
    }

    /**
     * Extraer userId del principal
     */
    private String extractUserId(Principal principal) {
        if (principal instanceof CustomUserDetailsService.UserPrincipal userPrincipal) {
            return userPrincipal.getId().toString();
        }
        return principal != null ? principal.getName() : "unknown";
    }

    /**
     * Extraer userName desde la base de datos
     */
    private String extractUserName(String userId) {
        try {
            UUID id = UUID.fromString(userId);
            Usuario usuario = usuarioRepository.findById(id).orElse(null);
            if (usuario != null) {
                return usuario.getNombre() + " " + usuario.getApellido();
            }
        } catch (Exception e) {
            log.error("[WebSocket] Error al buscar usuario", e);
        }
        return "Usuario";
    }
}
