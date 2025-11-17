package uy.um.faltauno.service;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "gcp.pubsub.enabled", havingValue = "true")
public class PubSubService {

    private final PubSubTemplate pubSubTemplate;

    @Value("${gcp.pubsub.topic:faltauno-events}")
    private String topicName;

    /**
     * Publica un evento en Pub/Sub para emails, push, etc.
     */
    public void publishEvent(String eventType, String payloadJson) {
        String message = "{" +
            "\"type\":\"" + eventType + "\"," +
            "\"payload\":" + payloadJson +
            "}";
        pubSubTemplate.publish(topicName, message);
        log.info("[PubSub] Evento publicado: {} -> {}", eventType, topicName);
    }
}
