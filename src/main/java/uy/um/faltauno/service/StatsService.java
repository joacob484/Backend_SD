package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import uy.um.faltauno.config.CacheNames;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.repository.UsuarioRepository;
import uy.um.faltauno.repository.ReviewRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para calcular estadísticas de la aplicación
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private final UsuarioRepository usuarioRepository;
    private final PartidoRepository partidoRepository;
    private final ReviewRepository reviewRepository;

    /**
     * Obtener estadísticas generales de la comunidad
     * Se cachea por 5 minutos para no sobrecargar la DB
     */
    @Cacheable(value = CacheNames.COMMUNITY_STATS, unless = "#result == null")
    public Map<String, Object> obtenerEstadisticasComunidad() {
        log.debug("Calculando estadísticas de la comunidad...");
        
        Map<String, Object> stats = new HashMap<>();

        try {
            // Total de usuarios registrados
            long totalUsuarios = usuarioRepository.count();
            stats.put("totalUsers", totalUsuarios);

            // Usuarios activos (con al menos 1 partido en los últimos 30 días)
            LocalDateTime hace30Dias = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
            // Contar usuarios que organizaron partidos recientes
            long usuariosActivos = partidoRepository.countDistinctOrganizadorByFechaGreaterThanEqual(hace30Dias.toLocalDate());
            stats.put("activeUsers", usuariosActivos);

            // Nuevos miembros (registrados en los últimos 7 días)
            LocalDateTime hace7Dias = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
            long nuevosMiembros = usuarioRepository.countByCreatedAtAfter(hace7Dias);
            stats.put("newMembers", nuevosMiembros);

            // Total de partidos
            long totalPartidos = partidoRepository.count();
            stats.put("totalMatches", totalPartidos);

            // Partidos esta semana (últimos 7 días)
            long partidosEstaSemana = partidoRepository.countPartidosEntreFechas(
                hace7Dias.toLocalDate(), 
                LocalDate.now().plusDays(1)
            );
            stats.put("matchesThisWeek", partidosEstaSemana);

            // Partidos completados
            long partidosCompletados = partidoRepository.countByEstado("COMPLETADO");
            stats.put("completedMatches", partidosCompletados);

            // Total de reviews
            long totalReviews = reviewRepository.count();
            stats.put("totalReviews", totalReviews);

            // Promedio global de calificación
            Double promedioGlobal = reviewRepository.findAverageCalificacionGlobal();
            stats.put("averageRating", promedioGlobal != null ? Math.round(promedioGlobal * 10.0) / 10.0 : 0.0);

            log.debug("Estadísticas calculadas: {} usuarios, {} partidos, {} reviews", 
                totalUsuarios, totalPartidos, totalReviews);

        } catch (Exception e) {
            log.error("Error calculando estadísticas de la comunidad", e);
            // Devolver stats vacías en caso de error
            stats.put("totalUsers", 0);
            stats.put("activeUsers", 0);
            stats.put("newMembers", 0);
            stats.put("totalMatches", 0);
            stats.put("matchesThisWeek", 0);
            stats.put("completedMatches", 0);
            stats.put("totalReviews", 0);
            stats.put("averageRating", 0.0);
        }

        return stats;
    }

    /**
     * Obtener estadísticas detalladas del sistema
     */
    @Cacheable(value = CacheNames.SYSTEM_STATS, unless = "#result == null")
    public Map<String, Object> obtenerEstadisticasSistema() {
        log.debug("Calculando estadísticas del sistema...");
        
        Map<String, Object> stats = new HashMap<>();

        try {
            // Incluir stats de la comunidad
            stats.putAll(obtenerEstadisticasComunidad());

            // Partidos por estado
            Map<String, Long> partidosPorEstado = new HashMap<>();
            partidosPorEstado.put("PENDIENTE", partidoRepository.countByEstado("PENDIENTE"));
            partidosPorEstado.put("CONFIRMADO", partidoRepository.countByEstado("CONFIRMADO"));
            partidosPorEstado.put("EN_CURSO", partidoRepository.countByEstado("EN_CURSO"));
            partidosPorEstado.put("COMPLETADO", partidoRepository.countByEstado("COMPLETADO"));
            partidosPorEstado.put("CANCELADO", partidoRepository.countByEstado("CANCELADO"));
            stats.put("matchesByStatus", partidosPorEstado);

            // Tasa de completitud de partidos
            long totalPartidosNoCancel = partidoRepository.count() - partidosPorEstado.get("CANCELADO");
            double tasaCompletitud = totalPartidosNoCancel > 0 
                ? (double) partidosPorEstado.get("COMPLETADO") / totalPartidosNoCancel * 100.0 
                : 0.0;
            stats.put("completionRate", Math.round(tasaCompletitud * 10.0) / 10.0);

        } catch (Exception e) {
            log.error("Error calculando estadísticas del sistema", e);
        }

        return stats;
    }
}
