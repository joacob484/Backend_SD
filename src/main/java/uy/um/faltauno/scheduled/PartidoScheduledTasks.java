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
     * 1. Cancelar partidos DISPONIBLES que llegaron a su fecha/hora (independiente de cupos)
     * 2. Completar partidos CONFIRMADOS que ya pasaron su fecha/hora
     */
    @Scheduled(fixedRate = 300000) // 5 minutos
    @Transactional
    public void procesarPartidosVencidos() {
        LocalDateTime ahora = LocalDateTime.now();
        
        // 1. Cancelar TODOS los partidos DISPONIBLES que llegaron a su fecha/hora
        // (incluso si tienen cupos llenos pero no fueron confirmados)
        List<Partido> disponiblesVencidos = partidoRepository
                .findByEstadoAndFechaHoraBefore("DISPONIBLE", ahora);
        
        for (Partido partido : disponiblesVencidos) {
            long inscritos = inscripcionRepository.countByPartidoIdAndEstado(
                    partido.getId(), "ACEPTADO");
            
            if (inscritos >= partido.getCantidadJugadores()) {
                log.warn("⚠️ Cancelando partido {} con cupos llenos ({}/{}) por falta de confirmación del organizador",
                        partido.getId(), inscritos, partido.getCantidadJugadores());
            } else {
                log.info("Cancelando partido {} por falta de jugadores ({}/{})",
                        partido.getId(), inscritos, partido.getCantidadJugadores());
            }
            
            partido.setEstado("CANCELADO");
            partidoRepository.save(partido);
        }
        
        // 2. Completar partidos CONFIRMADOS que ya pasaron
        List<Partido> confirmadosVencidos = partidoRepository
                .findByEstadoAndFechaHoraBefore("CONFIRMADO", ahora);
        
        for (Partido partido : confirmadosVencidos) {
            log.info("Completando partido {} automáticamente", partido.getId());
            partido.setEstado("COMPLETADO");
            partidoRepository.save(partido);
        }
        
        if (!disponiblesVencidos.isEmpty() || !confirmadosVencidos.isEmpty()) {
            log.info("Procesados {} partidos: {} cancelados, {} completados",
                    disponiblesVencidos.size() + confirmadosVencidos.size(),
                    disponiblesVencidos.size(),
                    confirmadosVencidos.size());
        }
    }
}
