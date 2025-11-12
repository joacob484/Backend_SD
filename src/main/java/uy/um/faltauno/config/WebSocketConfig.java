package uy.um.faltauno.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración de WebSocket con STOMP para actualizaciones en tiempo real
 * 
 * ARQUITECTURA:
 * - Desarrollo: SimpleBroker in-memory (websocket.broker.type=simple)
 * - Producción: RabbitMQ broker relay (websocket.broker.type=rabbitmq)
 *   * Permite escalar a múltiples instancias
 *   * Los mensajes se distribuyen vía RabbitMQ a todas las instancias
 * 
 * Endpoints:
 * - /ws - WebSocket handshake endpoint (con SockJS fallback)
 * 
 * Destinos de suscripción (clientes reciben):
 * - /topic/partidos/{partidoId} - Actualizaciones de un partido específico
 * - /topic/partidos/{partidoId}/chat - Mensajes de chat
 * - /user/queue/notifications - Notificaciones personales
 * 
 * Destinos de envío (servidor recibe):
 * - /app/partidos/{partidoId}/typing - Eventos de "usuario escribiendo"
 * 
 * Variables de entorno:
 * - WEBSOCKET_BROKER_TYPE: simple | rabbitmq (default: simple)
 * - RABBITMQ_HOST: hostname del broker (default: localhost)
 * - RABBITMQ_PORT: puerto STOMP (default: 61613)
 * - RABBITMQ_USERNAME: usuario (default: guest)
 * - RABBITMQ_PASSWORD: contraseña (default: guest)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.broker.type:simple}")
    private String brokerType;

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.port:61613}")
    private int rabbitPort;

    @Value("${spring.rabbitmq.username:guest}")
    private String rabbitUsername;

    @Value("${spring.rabbitmq.password:guest}")
    private String rabbitPassword;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        if ("rabbitmq".equalsIgnoreCase(brokerType)) {
            // ✅ RabbitMQ broker relay - PRODUCCIÓN (múltiples instancias)
            // Requiere RabbitMQ con plugin STOMP habilitado
            config.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(rabbitHost)
                    .setRelayPort(rabbitPort)
                    .setClientLogin(rabbitUsername)
                    .setClientPasscode(rabbitPassword)
                    .setSystemLogin(rabbitUsername)
                    .setSystemPasscode(rabbitPassword)
                    .setVirtualHost("/");
        } else {
            // ⚠️ Simple broker - DESARROLLO (single instance only)
            // NO escala a múltiples instancias - los mensajes solo llegan dentro de la misma JVM
            config.enableSimpleBroker("/topic", "/queue");
        }
        
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
