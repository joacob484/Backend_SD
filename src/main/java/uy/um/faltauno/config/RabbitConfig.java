package uy.um.faltauno.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE_PARTIDOS = "exchange.partidos";
    public static final String QUEUE_NOTIFICATIONS = "notificaciones.queue";
    public static final String ROUTING_PARTIDO_CREATED = "partidos.created";
    public static final String ROUTING_PARTIDO_CANCELADO = "partidos.cancelado";
    public static final String ROUTING_PARTIDO_COMPLETADO = "partidos.completado";

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

    @Bean
    public Binding bindingPartidoCancelado(Queue notificationsQueue, TopicExchange partidosExchange) {
        return BindingBuilder.bind(notificationsQueue).to(partidosExchange).with(ROUTING_PARTIDO_CANCELADO);
    }

    @Bean
    public Binding bindingPartidoCompletado(Queue notificationsQueue, TopicExchange partidosExchange) {
        return BindingBuilder.bind(notificationsQueue).to(partidosExchange).with(ROUTING_PARTIDO_COMPLETADO);
    }

    /**
     * Binding con wildcard para capturar todos los eventos de partidos
     */
    @Bean
    public Binding bindingAllPartidos(Queue notificationsQueue, TopicExchange partidosExchange) {
        return BindingBuilder.bind(notificationsQueue).to(partidosExchange).with("partidos.*");
    }
}