package uy.um.faltauno.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.spring.pubsub.core.PubSubConfiguration;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.DefaultPublisherFactory;
import com.google.cloud.spring.pubsub.support.DefaultSubscriberFactory;
import com.google.cloud.spring.pubsub.support.PublisherFactory;
import com.google.cloud.spring.pubsub.support.SubscriberFactory;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpPubSubConfig {

    @Bean
    public GcpProjectIdProvider gcpProjectIdProvider() {
        return () -> System.getenv("GOOGLE_CLOUD_PROJECT");
    }

    @Bean
    public PubSubConfiguration pubSubConfiguration() {
        return new PubSubConfiguration();
    }

    @Bean
    public PublisherFactory publisherFactory(GcpProjectIdProvider projectIdProvider, CredentialsProvider credentialsProvider, TransportChannelProvider transportChannelProvider) {
        DefaultPublisherFactory factory = new DefaultPublisherFactory(projectIdProvider);
        factory.setCredentialsProvider(credentialsProvider);
        factory.setChannelProvider(transportChannelProvider);
        return factory;
    }

    @Bean
    public SubscriberFactory subscriberFactory(GcpProjectIdProvider projectIdProvider, CredentialsProvider credentialsProvider, ExecutorProvider executorProvider, PubSubConfiguration pubSubConfiguration) {
        DefaultSubscriberFactory factory = new DefaultSubscriberFactory(projectIdProvider, pubSubConfiguration);
        factory.setCredentialsProvider(credentialsProvider);
        factory.setExecutorProvider(executorProvider);
        return factory;
    }

    @Bean
    public PubSubTemplate pubSubTemplate(PublisherFactory publisherFactory, SubscriberFactory subscriberFactory) {
        return new PubSubTemplate(publisherFactory, subscriberFactory);
    }
}
