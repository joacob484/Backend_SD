package uy.um.faltauno.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración de WebSocket con STOMP para actualizaciones en tiempo real
 * 
 * Endpoints:
 * - /ws - WebSocket handshake endpoint
 * 
 * Destinos de suscripción (clientes reciben):
 * - /topic/partidos/{partidoId} - Actualizaciones de un partido específico
 * - /topic/partidos - Actualizaciones generales de partidos
 * - /topic/inscripciones/{partidoId} - Actualizaciones de inscripciones
 * - /user/queue/notifications - Notificaciones personales
 * 
 * Destinos de envío (servidor recibe):
 * - /app/* - Prefijo para mensajes del cliente al servidor
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilitar broker simple en memoria para enviar mensajes a clientes
        // /topic - broadcast a todos los suscritos
        // /queue - mensajes punto a punto
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefijo para mensajes desde cliente a servidor
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefijo para mensajes dirigidos a usuarios específicos
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint para handshake WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                    "http://localhost:3000",           // Desarrollo local
                    "http://localhost:3001",           // Desarrollo alternativo
                    "https://faltauno.com",            // Producción
                    "https://www.faltauno.com",        // Producción www
                    "https://*.faltauno.com",          // Subdominios
                    "https://faltauno-*.web.app"       // Firebase hosting
                )
                .withSockJS();  // Fallback para navegadores sin WebSocket nativo
    }
}
