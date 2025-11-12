package uy.um.faltauno.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.spring.pubsub.core.PubSubConfiguration;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.DefaultPublisherFactory;
import com.google.cloud.spring.pubsub.support.DefaultSubscriberFactory;
import com.google.cloud.spring.pubsub.support.PublisherFactory;
import com.google.cloud.spring.pubsub.support.SubscriberFactory;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpPubSubConfig {

    @Bean
    public GcpProjectIdProvider gcpProjectIdProvider() {
        return () -> System.getenv("GOOGLE_CLOUD_PROJECT");
    }

    @Bean
    public CredentialsProvider credentialsProvider() throws IOException {
        return () -> GoogleCredentials.getApplicationDefault();
    }

    @Bean
    public ExecutorProvider executorProvider() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
        return FixedExecutorProvider.create(executor);
    }

    @Bean
    public TransportChannelProvider transportChannelProvider() {
        return InstantiatingGrpcChannelProvider.newBuilder().build();
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
