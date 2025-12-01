package uy.um.faltauno.config;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.TopicName;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.IOException;

@Configuration
@ConditionalOnProperty(name = "gcp.pubsub.enabled", havingValue = "true")
public class PubSubConfig implements DisposableBean {

    private volatile Publisher publisher;

    @Value("${gcp.pubsub.topic:}")
    private String pubsubTopic;

    @Bean
    @Lazy  // Don't initialize until first use - prevents blocking startup
    public Publisher publisher() throws IOException {
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
        return publisher;
    }

    @Override
    public void destroy() throws Exception {
        if (this.publisher != null) {
            this.publisher.shutdown();
        }
    }
}
