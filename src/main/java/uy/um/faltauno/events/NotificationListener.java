package uy.um.faltauno.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    @RabbitListener(queues = "notificaciones.queue")
    public void onEvent(Map<String, Object> event) {
        String type = (String) event.get("event");
        
        log.info("📩 Evento recibido: {} - Payload: {}", type, event);
        
        // Aquí se puede implementar lógica adicional según el tipo de evento:
        // - Enviar emails
        // - Push notifications
        // - Webhooks a servicios externos
        // - Analytics/métricas
        // - Logs en sistemas externos
        
        switch (type) {
            case "PARTIDO_CREADO":
                procesarPartidoCreado(event);
                break;
            case "PARTIDO_CANCELADO":
                procesarPartidoCancelado(event);
                break;
            case "PARTIDO_COMPLETADO":
                procesarPartidoCompletado(event);
                break;
            default:
                log.warn("Tipo de evento desconocido: {}", type);
        }
    }

    private void procesarPartidoCreado(Map<String, Object> event) {
        String partidoId = (String) event.get("partidoId");
        String tipoPartido = (String) event.get("tipoPartido");
        
        log.info("✅ Partido creado: {} - Tipo: {}", partidoId, tipoPartido);
        
        // TODO: Implementar acciones:
        // - Enviar email de confirmación al organizador
        // - Notificar a usuarios cercanos (según ubicación)
        // - Registrar métrica en sistema de analytics
    }

    private void procesarPartidoCancelado(Map<String, Object> event) {
        String partidoId = (String) event.get("partidoId");
        String motivo = (String) event.get("motivo");
        Integer jugadoresAfectados = (Integer) event.get("jugadoresAfectados");
        
        log.warn("❌ Partido cancelado: {} - Motivo: {} - Jugadores afectados: {}", 
                 partidoId, motivo, jugadoresAfectados);
        
        // TODO: Implementar acciones:
        // - Enviar emails de cancelación masivos
        // - Push notifications urgentes
        // - Registrar en sistema de reembolsos (si aplica)
    }

    private void procesarPartidoCompletado(Map<String, Object> event) {
        String partidoId = (String) event.get("partidoId");
        Integer jugadores = (Integer) event.get("jugadoresParticipantes");
        
        log.info("🏁 Partido completado: {} - Participantes: {}", partidoId, jugadores);
        
        // TODO: Implementar acciones:
        // - Enviar recordatorio para calificar
        // - Actualizar estadísticas de usuarios
        // - Generar reportes de actividad
    }
}
