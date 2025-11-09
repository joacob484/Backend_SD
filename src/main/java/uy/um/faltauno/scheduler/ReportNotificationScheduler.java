package uy.um.faltauno.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uy.um.faltauno.dto.ReportDTO;
import uy.um.faltauno.entity.Notificacion;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.NotificacionRepository;
import uy.um.faltauno.repository.UsuarioRepository;
import uy.um.faltauno.service.EmailService;
import uy.um.faltauno.service.ReportService;

import java.util.List;

/**
 * Scheduler para notificaciones semanales de reportes pendientes a admins
 * Se ejecuta todos los lunes a las 9:00 AM
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportNotificationScheduler {

    private final ReportService reportService;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;
    private final EmailService emailService;

    /**
     * Ejecutar todos los lunes a las 9:00 AM
     * Cron: segundo minuto hora día mes día-de-semana
     * 0 0 9 * * MON = A las 9:00 AM todos los lunes
     */
    @Scheduled(cron = "0 0 9 * * MON", zone = "America/Montevideo")
    public void notifyAdminsOfPendingReports() {
        log.info("⏰ [SCHEDULER] Ejecutando notificación semanal de reportes pendientes");
        
        try {
            // Obtener reportes pendientes
            long pendingCount = reportService.countPendingReports();
            
            if (pendingCount == 0) {
                log.info("✅ [SCHEDULER] No hay reportes pendientes. No se envían notificaciones.");
                return;
            }
            
            log.info("📊 [SCHEDULER] {} reportes pendientes encontrados", pendingCount);
            
            // Obtener lista detallada de reportes pendientes
            List<ReportDTO> pendingReports = reportService.getPendingReports();
            
            // Obtener todos los administradores
            List<Usuario> admins = usuarioRepository.findAllActive().stream()
                    .filter(u -> "ADMIN".equals(u.getRol()))
                    .filter(u -> u.getBannedAt() == null) // No notificar a admins baneados
                    .toList();
            
            if (admins.isEmpty()) {
                log.warn("⚠️ [SCHEDULER] No se encontraron administradores para notificar");
                return;
            }
            
            log.info("👥 [SCHEDULER] Notificando a {} administradores", admins.size());
            
            // Crear notificaciones para cada admin
            for (Usuario admin : admins) {
                createNotificationForAdmin(admin, pendingCount, pendingReports);
                
                // Enviar email si las notificaciones están habilitadas
                if (Boolean.TRUE.equals(admin.getNotifEmailGenerales())) {
                    sendEmailToAdmin(admin, pendingCount, pendingReports);
                } else {
                    log.debug("📧 [SCHEDULER] Admin {} tiene notificaciones por email deshabilitadas", admin.getEmail());
                }
            }
            
            log.info("✅ [SCHEDULER] Notificaciones semanales enviadas exitosamente");
            
        } catch (Exception e) {
            log.error("❌ [SCHEDULER] Error al enviar notificaciones semanales de reportes", e);
        }
    }

    /**
     * Crear notificación in-app para el admin
     */
    private void createNotificationForAdmin(Usuario admin, long pendingCount, List<ReportDTO> reports) {
        try {
            String titulo = String.format("📊 Reportes Pendientes: %d", pendingCount);
            
            StringBuilder mensaje = new StringBuilder();
            mensaje.append(String.format("Hay %d reportes pendientes de revisión.\n\n", pendingCount));
            
            // Agregar resumen de los primeros 3 reportes
            int limit = Math.min(3, reports.size());
            for (int i = 0; i < limit; i++) {
                ReportDTO report = reports.get(i);
                mensaje.append(String.format("• %s reportó a %s por: %s\n",
                        report.getReporter().getNombre() + " " + report.getReporter().getApellido(),
                        report.getReportedUser().getNombre() + " " + report.getReportedUser().getApellido(),
                        report.getReasonDisplayName()));
            }
            
            if (reports.size() > 3) {
                mensaje.append(String.format("\n...y %d más", reports.size() - 3));
            }
            
            Notificacion notificacion = Notificacion.builder()
                    .usuarioId(admin.getId())
                    .tipo(Notificacion.TipoNotificacion.PARTIDO_LISTO) // Reutilizamos, o podemos agregar un nuevo tipo
                    .titulo(titulo)
                    .mensaje(mensaje.toString())
                    .urlAccion("/admin?tab=reports")
                    .prioridad(Notificacion.Prioridad.ALTA)
                    .leida(false)
                    .build();
            
            notificacionRepository.save(notificacion);
            
            log.info("📬 [SCHEDULER] Notificación creada para admin {}", admin.getEmail());
            
        } catch (Exception e) {
            log.error("❌ [SCHEDULER] Error al crear notificación para admin {}", admin.getEmail(), e);
        }
    }

    /**
     * Enviar email al admin
     */
    private void sendEmailToAdmin(Usuario admin, long pendingCount, List<ReportDTO> reports) {
        try {
            String titulo = String.format("🚨 %d reportes pendientes de revisión", pendingCount);
            
            StringBuilder mensaje = new StringBuilder();
            mensaje.append(String.format("Hay <strong>%d reportes pendientes</strong> de revisión en el panel de administración.", pendingCount));
            
            mensaje.append("<h3>Resumen de Reportes Pendientes:</h3>");
            mensaje.append("<ul>");
            
            // Agregar los primeros 5 reportes
            int limit = Math.min(5, reports.size());
            for (int i = 0; i < limit; i++) {
                ReportDTO report = reports.get(i);
                mensaje.append(String.format("<li><strong>%s %s</strong> reportó a <strong>%s %s</strong><br>",
                        report.getReporter().getNombre(), 
                        report.getReporter().getApellido(),
                        report.getReportedUser().getNombre(),
                        report.getReportedUser().getApellido()));
                mensaje.append(String.format("Razón: %s</li>", report.getReasonDisplayName()));
            }
            
            if (reports.size() > 5) {
                mensaje.append(String.format("<li>...y %d reportes más</li>", reports.size() - 5));
            }
            
            mensaje.append("</ul>");
            
            mensaje.append("<p style=\"color: #666; font-size: 0.9em; margin-top: 30px;\">Este es un resumen semanal automático. Puedes desactivar estas notificaciones en tu configuración de cuenta.</p>");
            
            // Use enviarNotificacionEmail - it handles email preferences and HTML formatting
            emailService.enviarNotificacionEmail(
                admin,
                Notificacion.TipoNotificacion.PARTIDO_LISTO, // Reusing existing type until ADMIN_REPORT is added
                titulo,
                mensaje.toString(),
                "https://faltauno.com/admin?tab=reports"
            );
            
            log.info("📧 [SCHEDULER] Email enviado a admin {}", admin.getEmail());
            
        } catch (Exception e) {
            log.error("❌ [SCHEDULER] Error al enviar email a admin {}", admin.getEmail(), e);
        }
    }
    
    /**
     * OPCIONAL: Tarea de limpieza de reportes muy antiguos resueltos
     * Ejecutar el primer día de cada mes a las 2:00 AM
     */
    @Scheduled(cron = "0 0 2 1 * *", zone = "America/Montevideo")
    public void cleanupOldResolvedReports() {
        log.info("🧹 [SCHEDULER] Limpieza de reportes antiguos resueltos (>6 meses)");
        // TODO: Implementar si se desea eliminar reportes antiguos
        // Por ahora solo loggear
        log.info("ℹ️ [SCHEDULER] Cleanup de reportes no implementado - se mantienen todos los registros");
    }
}
