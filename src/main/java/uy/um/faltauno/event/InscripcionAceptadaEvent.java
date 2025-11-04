package uy.um.faltauno.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Evento publicado cuando una inscripción es aceptada.
 * Se dispara DESPUÉS del commit de la transacción.
 */
@Getter
public class InscripcionAceptadaEvent extends ApplicationEvent {
    
    private final UUID usuarioId;
    private final UUID partidoId;
    private final String nombrePartido;
    private final long jugadoresActuales;
    private final int cantidadJugadores;
    private final UUID organizadorId;
    
    public InscripcionAceptadaEvent(
            Object source,
            UUID usuarioId,
            UUID partidoId,
            String nombrePartido,
            long jugadoresActuales,
            int cantidadJugadores,
            UUID organizadorId
    ) {
        super(source);
        this.usuarioId = usuarioId;
        this.partidoId = partidoId;
        this.nombrePartido = nombrePartido;
        this.jugadoresActuales = jugadoresActuales;
        this.cantidadJugadores = cantidadJugadores;
        this.organizadorId = organizadorId;
    }
}
