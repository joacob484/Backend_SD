package uy.um.faltauno.config;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.Subscriber;
import uy.um.faltauno.events.NotificationListener;
import java.io.IOException;

public class PubSubConfig {

    public Publisher pubSubPublisher(String topicId) throws IOException {
        return Publisher.newBuilder(topicId).build();
    }

    public Subscriber pubSubSubscriber(String subscriptionId, NotificationListener listener) {
        return Subscriber.newBuilder(subscriptionId, listener).build();
    }
}