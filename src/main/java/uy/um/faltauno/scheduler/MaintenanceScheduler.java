package uy.um.faltauno.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uy.um.faltauno.service.UsuarioService;

/**
 * Scheduled tasks para mantenimiento de la aplicación.
 * 
 * - Cleanup de usuarios eliminados hace más de 30 días (diario a las 3:00 AM)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceScheduler {

    private final UsuarioService usuarioService;

    /**
     * Cleanup automático de usuarios eliminados hace más de 30 días.
     * Se ejecuta diariamente a las 3:00 AM (hora del servidor).
     * 
     * Cron: "0 0 3 * * *" = segundo 0, minuto 0, hora 3, todos los días
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredDeletedUsers() {
        log.info("🧹 [Scheduled] Iniciando cleanup de usuarios eliminados expirados...");
        
        try {
            int deletedCount = usuarioService.cleanupExpiredDeletedUsers();
            
            if (deletedCount > 0) {
                log.info("✅ [Scheduled] Cleanup completado: {} usuarios eliminados físicamente", deletedCount);
            } else {
                log.debug("✅ [Scheduled] Cleanup completado: No había usuarios expirados");
            }
            
        } catch (Exception e) {
            log.error("❌ [Scheduled] Error en cleanup de usuarios: {}", e.getMessage(), e);
        }
    }
}
