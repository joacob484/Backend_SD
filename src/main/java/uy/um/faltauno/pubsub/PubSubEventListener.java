

package uy.um.faltauno.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uy.um.faltauno.service.EmailService;
import uy.um.faltauno.service.PushNotificationService;

import javax.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
public class PubSubEventListener {

    private final PubSubTemplate pubSubTemplate;
    private final EmailService emailService;
    private final PushNotificationService pushService;

    @PostConstruct
    public void subscribe() {
        pubSubTemplate.subscribe("faltauno-events-sub", (message) -> {
            String data = message.getPubsubMessage().getData().toStringUtf8();
            log.info("[PubSub] Mensaje recibido: {}", data);

            try {
                // Ejemplo de mensaje esperado:
                // {
                //   "type": "EMAIL" | "PUSH",
                //   "payload": { ... }
                // }
                com.fasterxml.jackson.databind.JsonNode json =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(data);
                String type = json.has("type") ? json.get("type").asText() : "";
                com.fasterxml.jackson.databind.JsonNode payload = json.get("payload");

                switch (type) {
                    case "EMAIL" -> {
                        // Extraer datos del payload para email
                        // Debes adaptar esto según tu estructura real
                        String email = payload.has("email") ? payload.get("email").asText() : null;
                        String nombre = payload.has("nombre") ? payload.get("nombre").asText() : "";
                        String titulo = payload.has("titulo") ? payload.get("titulo").asText() : "";
                        String mensaje = payload.has("mensaje") ? payload.get("mensaje").asText() : "";
                        String urlAccion = payload.has("urlAccion") ? payload.get("urlAccion").asText() : null;
                        String tipoStr = payload.has("tipo") ? payload.get("tipo").asText() : "GENERIC";

                        // Aquí deberías buscar el usuario y el tipo de notificación real
                        uy.um.faltauno.entity.Usuario usuario = new uy.um.faltauno.entity.Usuario();
                        usuario.setEmail(email);
                        usuario.setNombre(nombre);
                        uy.um.faltauno.entity.Notificacion.TipoNotificacion tipoNotif = uy.um.faltauno.entity.Notificacion.TipoNotificacion.valueOf(tipoStr);

                        emailService.enviarNotificacionEmail(usuario, tipoNotif, titulo, mensaje, urlAccion);
                        log.info("[PubSub] EmailService.enviarNotificacionEmail ejecutado");
                    }
                    case "PUSH" -> {
                        // Extraer datos del payload para push
                        String userId = payload.has("userId") ? payload.get("userId").asText() : "";
                        String titulo = payload.has("titulo") ? payload.get("titulo").asText() : "";
                        String mensaje = payload.has("mensaje") ? payload.get("mensaje").asText() : "";
                        pushService.enviarPush(userId, titulo, mensaje);
                        log.info("[PubSub] PushNotificationService.enviarPush ejecutado");
                    }
                    default -> log.warn("[PubSub] Tipo de evento no soportado: {}", type);
                }
            } catch (Exception e) {
                log.error("[PubSub] Error procesando mensaje: {}", e.getMessage(), e);
            }
            message.ack();
        });
    }
}
