package uy.um.faltauno.config;

import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.TopicName;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.IOException;

@Configuration
@ConditionalOnProperty(name = "gcp.pubsub.enabled", havingValue = "true")
public class PubSubConfig implements DisposableBean {

    private volatile Publisher publisher;
    private final Object lock = new Object();

    @Value("${gcp.pubsub.topic:}")
    private String pubsubTopic;

    @Bean
    public Publisher publisher() {
        // Return a lazy-initializing proxy that only connects when first used
        return new Publisher() {
            private Publisher getDelegate() {
                if (publisher == null) {
                    synchronized (lock) {
                        if (publisher == null) {
                            try {
                                if (pubsubTopic == null || pubsubTopic.isEmpty()) {
                                    throw new IllegalStateException("gcp.pubsub.topic must be set when gcp.pubsub.enabled=true");
                                }
                                String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
                                if (projectId == null) {
                                    projectId = System.getenv("GCP_PROJECT");
                                }
                                if (projectId == null) {
                                    projectId = "master-might-274420";
                                }
                                TopicName topicName = TopicName.of(projectId, pubsubTopic);
                                publisher = Publisher.newBuilder(topicName).build();
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to create Publisher", e);
                            }
                        }
                    }
                }
                return publisher;
            }

            @Override
            public com.google.api.core.ApiFuture<String> publish(com.google.pubsub.v1.PubsubMessage message) {
                return getDelegate().publish(message);
            }

            @Override
            public void shutdown() {
                if (publisher != null) {
                    publisher.shutdown();
                }
            }

            @Override
            public com.google.api.gax.core.BackgroundResource.ShutdownBehavior getShutdownBehavior() {
                return getDelegate().getShutdownBehavior();
            }

            @Override
            public boolean isShutdown() {
                return publisher != null && publisher.isShutdown();
            }

            @Override
            public boolean isTerminated() {
                return publisher != null && publisher.isTerminated();
            }

            @Override
            public void shutdownNow() {
                if (publisher != null) {
                    publisher.shutdownNow();
                }
            }

            @Override
            public boolean awaitTermination(long duration, java.util.concurrent.TimeUnit unit) throws InterruptedException {
                return publisher == null || publisher.awaitTermination(duration, unit);
            }

            @Override
            public void close() {
                if (publisher != null) {
                    publisher.close();
                }
            }
        };
    }

    @Override
    public void destroy() throws Exception {
        if (this.publisher != null) {
            this.publisher.shutdown();
        }
    }
}
