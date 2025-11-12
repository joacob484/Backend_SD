package uy.um.faltauno.config;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.PublisherFactory;
import com.google.cloud.spring.pubsub.support.SubscriberFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpPubSubConfig {

    @Bean
    public PubSubTemplate pubSubTemplate(PublisherFactory publisherFactory, SubscriberFactory subscriberFactory) {
        return new PubSubTemplate(publisherFactory, subscriberFactory);
    }
}
