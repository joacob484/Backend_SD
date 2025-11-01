package uy.um.faltauno.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.repository.InscripcionRepository;
import uy.um.faltauno.repository.PartidoRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tareas programadas para gestión automática de partidos
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PartidoScheduledTasks {

    private final PartidoRepository partidoRepository;
    private final InscripcionRepository inscripcionRepository;

    /**
     * Ejecuta cada 5 minutos para:
     * 1. Cancelar partidos PENDIENTES que llegaron a su fecha/hora sin llenar cupos
     * 2. Completar partidos CONFIRMADOS que ya pasaron su fecha/hora
     */
    @Scheduled(fixedRate = 300000) // 5 minutos
    @Transactional
    public void procesarPartidosVencidos() {
        LocalDateTime ahora = LocalDateTime.now();
        
        // 1. Cancelar partidos PENDIENTES que no se llenaron
        List<Partido> pendientesVencidos = partidoRepository
                .findByEstadoAndFechaHoraBefore("PENDIENTE", ahora);
        
        for (Partido partido : pendientesVencidos) {
            long inscritos = inscripcionRepository.countByPartidoIdAndEstado(
                    partido.getId(), "CONFIRMADA");
            
            if (inscritos < partido.getCantidadJugadores()) {
                log.info("Cancelando partido {} por falta de jugadores ({}/{})",
                        partido.getId(), inscritos, partido.getCantidadJugadores());
                partido.setEstado("CANCELADO");
                partidoRepository.save(partido);
            }
        }
        
        // 2. Completar partidos CONFIRMADOS que ya pasaron
        List<Partido> confirmadosVencidos = partidoRepository
                .findByEstadoAndFechaHoraBefore("CONFIRMADO", ahora);
        
        for (Partido partido : confirmadosVencidos) {
            log.info("Completando partido {} automáticamente", partido.getId());
            partido.setEstado("COMPLETADO");
            partidoRepository.save(partido);
        }
        
        if (!pendientesVencidos.isEmpty() || !confirmadosVencidos.isEmpty()) {
            log.info("Procesados {} partidos: {} cancelados, {} completados",
                    pendientesVencidos.size() + confirmadosVencidos.size(),
                    pendientesVencidos.size(),
                    confirmadosVencidos.size());
        }
    }
}
