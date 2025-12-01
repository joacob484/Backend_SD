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

    private Publisher publisher;

    @Value("${gcp.pubsub.topic:}")
    private String pubsubTopic;

    @Bean
    public Publisher publisher() throws IOException {
        if (pubsubTopic == null || pubsubTopic.isEmpty()) {
            throw new IllegalStateException("gcp.pubsub.topic must be set when gcp.pubsub.enabled=true");
        }
        // Get project ID from environment instead of blocking ServiceOptions call
        String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
        if (projectId == null) {
            projectId = System.getenv("GCP_PROJECT");
        }
        if (projectId == null) {
            projectId = "master-might-274420"; // fallback
        }
        TopicName topicName = TopicName.of(projectId, pubsubTopic);
        this.publisher = Publisher.newBuilder(topicName).build();
        return this.publisher;
    }

    @Override
    public void destroy() throws Exception {
        if (this.publisher != null) {
            this.publisher.shutdown();
        }
    }
}
