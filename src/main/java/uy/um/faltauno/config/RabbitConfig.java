package uy.um.faltauno.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "app.rabbit.enabled", havingValue = "true", matchIfMissing = false)
public class RabbitConfig {

    // Exchanges
    public static final String EXCHANGE_PARTIDOS = "exchange.partidos";
    public static final String EXCHANGE_PARTIDOS_DLX = "exchange.partidos.dlx";

    // Queues
    public static final String QUEUE_NOTIFICATIONS = "notificaciones.queue";
    public static final String QUEUE_NOTIFICATIONS_DLQ = "notificaciones.queue.dlq";

    // Routing keys
    public static final String RK_PARTIDO_CREATED    = "partidos.created";
    public static final String RK_PARTIDO_CANCELADO  = "partidos.cancelado";
    public static final String RK_PARTIDO_COMPLETADO = "partidos.completado";
    // Si querés capturar todos (opcional):
    public static final String RK_ALL_PARTIDOS       = "partidos.*";

    // ---- Declaraciones “a prueba” ----

    @Bean
    public TopicExchange partidosExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_PARTIDOS)
                .durable(true).build();
    }

    @Bean
    public TopicExchange partidosDlx() {
        return ExchangeBuilder.topicExchange(EXCHANGE_PARTIDOS_DLX)
                .durable(true).build();
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATIONS)
                .withArgument("x-dead-letter-exchange", EXCHANGE_PARTIDOS_DLX)
                .withArgument("x-dead-letter-routing-key", QUEUE_NOTIFICATIONS_DLQ)
                .build();
    }

    @Bean
    public Queue notificationsDlq() {
        return QueueBuilder.durable(QUEUE_NOTIFICATIONS_DLQ).build();
    }

    @Bean
    public Binding bindingPartidoCreated(Queue notificationsQueue, TopicExchange partidosExchange) {
        return BindingBuilder.bind(notificationsQueue).to(partidosExchange).with(RK_PARTIDO_CREATED);
    }

    @Bean
    public Binding bindingPartidoCancelado(Queue notificationsQueue, TopicExchange partidosExchange) {
        return BindingBuilder.bind(notificationsQueue).to(partidosExchange).with(RK_PARTIDO_CANCELADO);
    }

    @Bean
    public Binding bindingPartidoCompletado(Queue notificationsQueue, TopicExchange partidosExchange) {
        return BindingBuilder.bind(notificationsQueue).to(partidosExchange).with(RK_PARTIDO_COMPLETADO);
    }

    // Opcional: un wildcard para “todo”. Si lo dejás, no duplica mensajes en la misma queue,
    // pero puede ocultar errores de routing porque “todo entra igual”.
    // Si querés máxima explicitud, ELIMINALO.
    @Bean
    public Binding bindingAllPartidos(Queue notificationsQueue, TopicExchange partidosExchange) {
        return BindingBuilder.bind(notificationsQueue).to(partidosExchange).with(RK_ALL_PARTIDOS);
    }

    @Bean
    public Binding bindingDlq(Queue notificationsDlq, TopicExchange partidosDlx) {
        // La DLQ enruta con la misma routing key fija
        return BindingBuilder.bind(notificationsDlq).to(partidosDlx).with(QUEUE_NOTIFICATIONS_DLQ);
    }

    // ---- Admin/Template/Listeners hardening ----

    @Bean
    public RabbitAdmin rabbitAdmin(org.springframework.amqp.rabbit.connection.ConnectionFactory cf) {
        RabbitAdmin admin = new RabbitAdmin(cf);
        admin.setAutoStartup(true);
        admin.setIgnoreDeclarationExceptions(true); // si falta permiso, no tumba el contexto
        return admin;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory cf,
                                         @Value("${spring.rabbitmq.template.reply-timeout:3000}") long replyTimeoutMs) {
        RabbitTemplate rt = new RabbitTemplate(cf);
        rt.setMandatory(true);
        rt.setReplyTimeout(replyTimeoutMs); // RPC nunca bloquea indefinido
        return rt;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            org.springframework.amqp.rabbit.connection.ConnectionFactory cf) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setPrefetchCount(10);
        f.setDefaultRequeueRejected(false);   // evita loops venenosos
        f.setMissingQueuesFatal(false);       // si falta una cola al boot, no tumbes la app
        return f;
    }
}