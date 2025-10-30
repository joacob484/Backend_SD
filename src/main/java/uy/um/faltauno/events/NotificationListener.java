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
    public void startPubSubListener() {
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
        String type = message.getAttributesOrDefault("event", "unknown");
        System.out.printf("\u2705 Event received: %s - Payload: %s%n", type, message.getData().toStringUtf8());

        switch (type) {
            case "PARTIDO_CREADO":
                procesarPartidoCreado(message);
                break;
            case "PARTIDO_CANCELADO":
                procesarPartidoCancelado(message);
                break;
            case "PARTIDO_COMPLETADO":
                procesarPartidoCompletado(message);
                break;
            default:
                System.out.printf("Unknown event type: %s%n", type);
        }
        consumer.ack();
    }

    private void procesarPartidoCreado(PubsubMessage message) {
        System.out.printf("\u2705 Partido creado: %s%n", message.getData().toStringUtf8());
    }

    private void procesarPartidoCancelado(PubsubMessage message) {
        System.out.printf("\u274c Partido cancelado: %s%n", message.getData().toStringUtf8());
    }

    private void procesarPartidoCompletado(PubsubMessage message) {
        System.out.printf("\u2705 Partido completado: %s%n", message.getData().toStringUtf8());
    }
}
