package uy.um.faltauno.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tareas programadas para mantenimiento del sistema
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final RateLimitingFilter rateLimitingFilter;

    /**
     * Limpia entradas antiguas del rate limiting cada 5 minutos
     */
    @Scheduled(fixedRate = 300000) // 5 minutos
    public void cleanupRateLimiting() {
        log.debug("🧹 Limpiando entradas antiguas de rate limiting");
        rateLimitingFilter.cleanupOldEntries();
    }
}
