package uy.um.faltauno.events;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class NotificationListener {

    @RabbitListener(queues = "notificaciones.queue")
    public void onEvent(Map<String,Object> event) {
        String type = (String) event.get("event");
        // lógica: enviar push, email, etc.
        System.out.println("Evento recibido: " + type + " payload: " + event);
    }
}
