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
        
        // FUTURO: Implementar acciones adicionales:
        // - Enviar email de confirmación al organizador (ya manejado por EmailService)
        // - Notificar a usuarios cercanos según ubicación (requiere servicio de geolocalización)
        // - Registrar métrica en sistema de analytics externo (Google Analytics, Mixpanel, etc.)
        
        // Por ahora, el log es suficiente para auditoría
    }

    private void procesarPartidoCancelado(Map<String, Object> event) {
        String partidoId = (String) event.get("partidoId");
        String motivo = (String) event.get("motivo");
        Integer jugadoresAfectados = (Integer) event.get("jugadoresAfectados");
        
        log.warn("❌ Partido cancelado: {} - Motivo: {} - Jugadores afectados: {}", 
                 partidoId, motivo, jugadoresAfectados);
        
        // FUTURO: Implementar acciones adicionales:
        // - Enviar emails de cancelación masivos a participantes (ya manejado por EmailService)
        // - Push notifications urgentes vía Firebase Cloud Messaging
        // - Sistema de reembolsos automáticos (si se implementan pagos)
        
        // Por ahora, el log y las notificaciones in-app son suficientes
    }

    private void procesarPartidoCompletado(Map<String, Object> event) {
        String partidoId = (String) event.get("partidoId");
        Integer jugadores = (Integer) event.get("jugadoresParticipantes");
        
        log.info("🏁 Partido completado: {} - Participantes: {}", partidoId, jugadores);
        
        // FUTURO: Implementar acciones adicionales:
        // - Enviar recordatorio para calificar jugadores (puede ser un job programado 24h después)
        // - Actualizar estadísticas agregadas de usuarios (partidos jugados, ratio participación)
        // - Generar reportes de actividad para dashboards administrativos
        
        // Por ahora, las calificaciones se solicitan in-app al finalizar el partido
    }
}
