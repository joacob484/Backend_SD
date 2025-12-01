package uy.um.faltauno.events;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
@ConditionalOnProperty(name = "gcp.pubsub.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class NotificationListener implements MessageReceiver {

    @Value("${gcp.pubsub.subscription:faltauno-events-sub}")
    private String subscriptionId;
    
    @Value("${spring.cloud.gcp.project-id:${GCP_PROJECT_ID:master-might-274420}}")
    private String projectId;
    
    private Subscriber subscriber;

    @PostConstruct
    public void scheduleStartup() {
        startPubSubListenerAsync();
    }
    
    @Async
    public void startPubSubListenerAsync() {
        try {
            ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);
            log.info("🚀 Starting Pub/Sub listener for subscription: {}", subscriptionName);
            
            subscriber = Subscriber.newBuilder(subscriptionName, this).build();
            subscriber.startAsync().awaitRunning();
            
            log.info("✅ Pub/Sub listener started successfully");
        } catch (Exception e) {
            log.error("❌ Failed to start Pub/Sub listener: {}", e.getMessage(), e);
        }
    }
    
    @PreDestroy
    public void stopPubSubListener() {
        if (subscriber != null) {
            log.info("🛑 Stopping Pub/Sub listener...");
            subscriber.stopAsync().awaitTerminated();
            log.info("✅ Pub/Sub listener stopped");
        }
    }

    @Override
    public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
        // Acknowledge immediately to prevent redelivery
        consumer.ack();

        String type = message.getAttributesOrDefault("type", "unknown");
        log.info("✅ Event received: {} - Payload: {}", type, message.getData().toStringUtf8());

        try {
            // ✅ Procesar según tipo
            switch (type) {
                case "partido.creado" -> onPartidoCreado(message);
                case "partido.cancelado" -> onPartidoCancelado(message);
                case "partido.completado" -> onPartidoCompletado(message);
                // Más eventos...
                default -> {
                    log.warn("Unknown event type: {}", type);
                }
            }
        } catch (Exception e) {
            log.error("Error processing Pub/Sub message: {}", message, e);
        }
    }

    private void onPartidoCreado(PubsubMessage message) {
        log.info("✅ Partido creado: {}", message.getData().toStringUtf8());
    }

    private void onPartidoCancelado(PubsubMessage message) {
        log.warn("❌ Partido cancelado: {}", message.getData().toStringUtf8());
    }

    private void onPartidoCompletado(PubsubMessage message) {
        log.info("✅ Partido completado: {}", message.getData().toStringUtf8());
    }
}
