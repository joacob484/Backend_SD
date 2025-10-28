package uy.um.faltauno.events;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.PubsubMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener implements MessageReceiver {

    public void startPubSubListener() {
        String subscriptionId = "faltauno-events-sub";
        Subscriber subscriber = Subscriber.newBuilder(subscriptionId, this::receiveMessage).build();
        subscriber.startAsync().awaitRunning();
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
