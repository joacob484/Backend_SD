package uy.um.faltauno.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE_PARTIDOS = "exchange.partidos";
    public static final String QUEUE_NOTIFICATIONS = "notificaciones.queue";
    public static final String ROUTING_PARTIDO_CREATED = "partidos.created";

    @Bean
    public TopicExchange partidosExchange() {
        return new TopicExchange(EXCHANGE_PARTIDOS);
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATIONS)
                .deadLetterExchange("") // configure DLX if needed
                .build();
    }

    @Bean
    public Binding bindingPartidoCreated(Queue notificationsQueue, TopicExchange partidosExchange) {
        return BindingBuilder.bind(notificationsQueue).to(partidosExchange).with(ROUTING_PARTIDO_CREATED);
    }
}