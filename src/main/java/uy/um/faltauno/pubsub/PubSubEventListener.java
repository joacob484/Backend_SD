

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
            // Aquí parseas el tipo y delegas a emailService o pushService
            // Ejemplo:
            // if (data.contains("EMAIL")) emailService.enviarNotificacionEmail(...);
            // if (data.contains("PUSH")) pushService.enviarPush(...);
            message.ack();
        });
    }
}
