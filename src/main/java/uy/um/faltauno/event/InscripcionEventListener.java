package uy.um.faltauno.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uy.um.faltauno.service.NotificacionService;

/**
 * Listener para eventos de inscripción.
 * Se ejecuta DESPUÉS del commit de la transacción principal.
 * Si falla, NO afecta la inscripción guardada.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InscripcionEventListener {
    
    private final NotificacionService notificacionService;
    
    /**
     * Manejar evento de inscripción aceptada.
     * 
     * CRÍTICO:
     * - @TransactionalEventListener(phase = AFTER_COMMIT): se ejecuta SOLO si la transacción es exitosa
     * - @Async: se ejecuta en thread separado para no bloquear
     * - Si falla, solo se loggea el error, NO afecta la inscripción
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInscripcionAceptada(InscripcionAceptadaEvent event) {
        try {
            log.info("[InscripcionEventListener] 📧 Procesando notificaciones para inscripción aceptada");
            
            // Notificar al jugador que fue aceptado
            notificacionService.notificarInscripcionAceptada(
                    event.getUsuarioId(),
                    event.getPartidoId(),
                    event.getNombrePartido()
            );
            
            // Notificar al organizador si el partido se llenó
            if (event.getJugadoresActuales() >= event.getCantidadJugadores()) {
                log.info("[InscripcionEventListener] ⚽ Partido completo ({}/{})", 
                        event.getJugadoresActuales(), event.getCantidadJugadores());
                
                notificacionService.notificarPartidoListo(
                        event.getOrganizadorId(),
                        event.getPartidoId(),
                        event.getNombrePartido()
                );
            }
            
            log.info("[InscripcionEventListener] ✅ Notificaciones enviadas exitosamente");
            
        } catch (Exception e) {
            // ✅ CRÍTICO: Solo loggear, NO propagar el error
            // La inscripción ya está guardada, las notificaciones son secundarias
            log.error("[InscripcionEventListener] ❌ Error enviando notificaciones (inscripción OK): {}", 
                    e.getMessage(), e);
        }
    }
}
