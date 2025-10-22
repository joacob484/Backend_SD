package uy.um.faltauno.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uy.um.faltauno.entity.Notificacion;
import uy.um.faltauno.entity.Usuario;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username:#{null}}")
    private String fromEmail;
    
    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // Inicialización
    @jakarta.annotation.PostConstruct
    public void init() {
        if (isEmailConfigured()) {
            log.info("╔═══════════════════════════════════════════════════════════╗");
            log.info("║  📧 Sistema de Email ACTIVADO                             ║");
            log.info("║  Servidor: smtp.gmail.com                                 ║");
            log.info("║  Usuario: {}                                              ║", fromEmail);
            log.info("║  Los usuarios recibirán notificaciones por email         ║");
            log.info("╚═══════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔═══════════════════════════════════════════════════════════╗");
            log.info("║  📭 Sistema de Email DESACTIVADO                          ║");
            log.info("║  Las notificaciones solo se enviarán in-app              ║");
            log.info("║  Para activar: Configurar MAIL_* en .env                 ║");
            log.info("║  Ver: EMAIL_SETUP_GUIDE.md                                ║");
            log.info("╚═══════════════════════════════════════════════════════════╝");
        }
    }

    /**
     * Verifica si el email está configurado
     */
    private boolean isEmailConfigured() {
        return fromEmail != null && !fromEmail.isBlank() && !fromEmail.equals("noreply@faltauno.com");
    }

    /**
     * Enviar notificación por email de forma asíncrona
     */
    @Async
    public void enviarNotificacionEmail(
            Usuario usuario,
            Notificacion.TipoNotificacion tipo,
            String titulo,
            String mensaje,
            String urlAccion
    ) {
        // Verificar si el email está configurado
        if (!isEmailConfigured()) {
            log.debug("[EmailService] Email no configurado. Saltando envío.");
            return;
        }

        try {
            // Verificar si el usuario tiene habilitadas las notificaciones por email para este tipo
            if (!debeEnviarEmail(usuario, tipo)) {
                log.debug("[EmailService] Usuario {} tiene deshabilitadas las notificaciones de tipo {}", 
                    usuario.getEmail(), tipo);
                return;
            }

            String asunto = construirAsunto(tipo, titulo);
            String cuerpoHtml = construirCuerpoEmail(usuario, tipo, titulo, mensaje, urlAccion);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, "Falta Uno");
            helper.setTo(usuario.getEmail());
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);

            mailSender.send(mimeMessage);
            
            log.info("[EmailService] ✅ Email enviado a {}: tipo={}", usuario.getEmail(), tipo);

        } catch (MessagingException e) {
            log.error("[EmailService] ❌ Error enviando email a {}: {}", usuario.getEmail(), e.getMessage());
        } catch (Exception e) {
            log.error("[EmailService] ❌ Error inesperado enviando email: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica si se debe enviar email según las preferencias del usuario
     */
    private boolean debeEnviarEmail(Usuario usuario, Notificacion.TipoNotificacion tipo) {
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            return false;
        }

        return switch (tipo) {
            case INVITACION_PARTIDO -> 
                usuario.getNotifEmailInvitaciones() != null && usuario.getNotifEmailInvitaciones();
            
            case SOLICITUD_AMISTAD, AMISTAD_ACEPTADA -> 
                usuario.getNotifEmailSolicitudesAmistad() != null && usuario.getNotifEmailSolicitudesAmistad();
            
            case PARTIDO_CANCELADO, PARTIDO_COMPLETADO, INSCRIPCION_ACEPTADA, 
                 INSCRIPCION_RECHAZADA, JUGADOR_SALIO, PARTIDO_PROXIMO, 
                 INVITACION_ACEPTADA, INVITACION_RECHAZADA -> 
                usuario.getNotifEmailActualizacionesPartido() != null && usuario.getNotifEmailActualizacionesPartido();
            
            case REVIEW_PENDIENTE -> 
                usuario.getNotifEmailSolicitudesReview() != null && usuario.getNotifEmailSolicitudesReview();
            
            case NUEVO_MENSAJE -> 
                usuario.getNotifEmailNuevosMensajes() != null && usuario.getNotifEmailNuevosMensajes();
            
            case JUGADOR_UNIDO -> 
                usuario.getNotifEmailGenerales() != null && usuario.getNotifEmailGenerales();
        };
    }

    /**
     * Construir asunto del email
     */
    private String construirAsunto(Notificacion.TipoNotificacion tipo, String titulo) {
        return "[Falta Uno] " + titulo;
    }

    /**
     * Construir cuerpo HTML del email
     */
    private String construirCuerpoEmail(
            Usuario usuario,
            Notificacion.TipoNotificacion tipo,
            String titulo,
            String mensaje,
            String urlAccion
    ) {
        String accionUrl = urlAccion != null ? frontendUrl + urlAccion : frontendUrl;
        String emoji = obtenerEmoji(tipo);
        String colorPrincipal = obtenerColorPrincipal(tipo);
        
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f5f5f5; padding: 20px 0;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                                <!-- Header -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, %s 0%%, %s 100%%); padding: 32px; text-align: center; border-radius: 12px 12px 0 0;">
                                        <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;">
                                            %s Falta Uno
                                        </h1>
                                    </td>
                                </tr>
                                
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 32px;">
                                        <p style="color: #666; font-size: 16px; margin: 0 0 8px 0;">
                                            Hola <strong>%s</strong>,
                                        </p>
                                        
                                        <div style="background-color: #f8f9fa; border-left: 4px solid %s; padding: 20px; margin: 24px 0; border-radius: 4px;">
                                            <h2 style="color: #333; margin: 0 0 12px 0; font-size: 20px;">
                                                %s %s
                                            </h2>
                                            <p style="color: #555; font-size: 15px; margin: 0; line-height: 1.6;">
                                                %s
                                            </p>
                                        </div>
                                        
                                        <!-- Action Button -->
                                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 24px 0;">
                                            <tr>
                                                <td align="center">
                                                    <a href="%s" style="display: inline-block; background-color: %s; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: bold; font-size: 16px;">
                                                        Ver detalles
                                                    </a>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <p style="color: #999; font-size: 13px; margin: 24px 0 0 0; line-height: 1.5;">
                                            Esta es una notificación automática. Puedes gestionar tus preferencias de notificación en 
                                            <a href="%s/settings" style="color: %s; text-decoration: none;">Configuración</a>.
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f8f9fa; padding: 20px 32px; text-align: center; border-radius: 0 0 12px 12px;">
                                        <p style="color: #999; font-size: 13px; margin: 0 0 8px 0;">
                                            © 2025 Falta Uno. Todos los derechos reservados.
                                        </p>
                                        <p style="color: #999; font-size: 12px; margin: 0;">
                                            <a href="%s/help" style="color: #999; text-decoration: none;">Centro de Ayuda</a> • 
                                            <a href="%s/settings" style="color: #999; text-decoration: none;">Preferencias</a>
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(
                titulo,
                colorPrincipal, obtenerColorSecundario(tipo), // Header gradient
                emoji,
                usuario.getNombre(),
                colorPrincipal, // Border color
                emoji, titulo,
                mensaje,
                accionUrl,
                colorPrincipal, // Button color
                frontendUrl, colorPrincipal, // Settings link
                frontendUrl, frontendUrl // Footer links
            );
    }

    /**
     * Obtener emoji según tipo de notificación
     */
    private String obtenerEmoji(Notificacion.TipoNotificacion tipo) {
        return switch (tipo) {
            case INVITACION_PARTIDO -> "⚽";
            case SOLICITUD_AMISTAD -> "👋";
            case AMISTAD_ACEPTADA -> "🤝";
            case INSCRIPCION_ACEPTADA -> "✅";
            case INSCRIPCION_RECHAZADA -> "❌";
            case PARTIDO_CANCELADO -> "🚫";
            case PARTIDO_COMPLETADO, REVIEW_PENDIENTE -> "⭐";
            case JUGADOR_UNIDO -> "➕";
            case JUGADOR_SALIO -> "➖";
            case NUEVO_MENSAJE -> "💬";
            case PARTIDO_PROXIMO -> "⏰";
            case INVITACION_ACEPTADA, INVITACION_RECHAZADA -> "📩";
        };
    }

    /**
     * Obtener color principal según tipo de notificación
     */
    private String obtenerColorPrincipal(Notificacion.TipoNotificacion tipo) {
        return switch (tipo) {
            case INVITACION_PARTIDO, PARTIDO_PROXIMO -> "#10b981"; // Verde
            case SOLICITUD_AMISTAD, AMISTAD_ACEPTADA -> "#3b82f6"; // Azul
            case INSCRIPCION_ACEPTADA, INVITACION_ACEPTADA -> "#10b981"; // Verde
            case INSCRIPCION_RECHAZADA, INVITACION_RECHAZADA, PARTIDO_CANCELADO, JUGADOR_SALIO -> "#ef4444"; // Rojo
            case PARTIDO_COMPLETADO, REVIEW_PENDIENTE -> "#f59e0b"; // Naranja
            case NUEVO_MENSAJE -> "#8b5cf6"; // Púrpura
            case JUGADOR_UNIDO -> "#06b6d4"; // Cyan
        };
    }

    /**
     * Obtener color secundario para gradiente
     */
    private String obtenerColorSecundario(Notificacion.TipoNotificacion tipo) {
        return switch (tipo) {
            case INVITACION_PARTIDO, PARTIDO_PROXIMO -> "#059669";
            case SOLICITUD_AMISTAD, AMISTAD_ACEPTADA -> "#2563eb";
            case INSCRIPCION_ACEPTADA, INVITACION_ACEPTADA -> "#059669";
            case INSCRIPCION_RECHAZADA, INVITACION_RECHAZADA, PARTIDO_CANCELADO, JUGADOR_SALIO -> "#dc2626";
            case PARTIDO_COMPLETADO, REVIEW_PENDIENTE -> "#d97706";
            case NUEVO_MENSAJE -> "#7c3aed";
            case JUGADOR_UNIDO -> "#0891b2";
        };
    }

    /**
     * Enviar email de bienvenida
     */
    @Async
    public void enviarEmailBienvenida(Usuario usuario) {
        // Verificar si el email está configurado
        if (!isEmailConfigured()) {
            log.debug("[EmailService] Email no configurado. Saltando envío de bienvenida.");
            return;
        }

        try {
            String asunto = "[Falta Uno] ¡Bienvenido a la comunidad!";
            String cuerpoHtml = construirEmailBienvenida(usuario);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, "Falta Uno");
            helper.setTo(usuario.getEmail());
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);

            mailSender.send(mimeMessage);
            
            log.info("[EmailService] ✅ Email de bienvenida enviado a {}", usuario.getEmail());

        } catch (Exception e) {
            log.error("[EmailService] ❌ Error enviando email de bienvenida: {}", e.getMessage());
        }
    }

    /**
     * Construir email de bienvenida
     */
    private String construirEmailBienvenida(Usuario usuario) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f5f5f5;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f5f5f5; padding: 20px 0;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                                <tr>
                                    <td style="background: linear-gradient(135deg, #10b981 0%%, #059669 100%%); padding: 40px; text-align: center; border-radius: 12px 12px 0 0;">
                                        <h1 style="color: #ffffff; margin: 0; font-size: 32px; font-weight: bold;">
                                            ⚽ ¡Bienvenido a Falta Uno!
                                        </h1>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 40px;">
                                        <p style="color: #666; font-size: 18px; margin: 0 0 16px 0;">
                                            Hola <strong>%s</strong>,
                                        </p>
                                        <p style="color: #555; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;">
                                            ¡Nos alegra tenerte en nuestra comunidad! Falta Uno es tu plataforma para encontrar partidos, 
                                            hacer amigos y disfrutar del deporte que más amas.
                                        </p>
                                        <div style="background-color: #f0fdf4; border-left: 4px solid #10b981; padding: 20px; margin: 24px 0; border-radius: 4px;">
                                            <h3 style="color: #166534; margin: 0 0 12px 0;">🚀 Próximos pasos:</h3>
                                            <ul style="color: #166534; margin: 0; padding-left: 20px; line-height: 1.8;">
                                                <li>Completa tu perfil con tu posición favorita</li>
                                                <li>Busca partidos cerca de ti</li>
                                                <li>Conecta con otros jugadores</li>
                                                <li>¡Empieza a jugar!</li>
                                            </ul>
                                        </div>
                                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 32px 0;">
                                            <tr>
                                                <td align="center">
                                                    <a href="%s/profile-setup" style="display: inline-block; background-color: #10b981; color: #ffffff; text-decoration: none; padding: 16px 36px; border-radius: 8px; font-weight: bold; font-size: 16px;">
                                                        Completar mi perfil
                                                    </a>
                                                </td>
                                            </tr>
                                        </table>
                                        <p style="color: #999; font-size: 14px; margin: 24px 0 0 0;">
                                            ¿Necesitas ayuda? Visita nuestro <a href="%s/help" style="color: #10b981;">Centro de Ayuda</a>.
                                        </p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="background-color: #f8f9fa; padding: 24px; text-align: center; border-radius: 0 0 12px 12px;">
                                        <p style="color: #999; font-size: 13px; margin: 0;">
                                            © 2025 Falta Uno. Nos vemos en la cancha! ⚽
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(usuario.getNombre(), frontendUrl, frontendUrl);
    }
}
