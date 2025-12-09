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
    
    @Value("${frontend.url:https://faltauno-frontend-169771742214.us-central1.run.app}")
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
                 INVITACION_ACEPTADA, INVITACION_RECHAZADA, JUGADOR_UNIDO,
                 NUEVA_SOLICITUD -> 
                usuario.getNotifEmailActualizacionesPartido() != null && usuario.getNotifEmailActualizacionesPartido();
            
            case REVIEW_PENDIENTE -> 
                usuario.getNotifEmailSolicitudesReview() != null && usuario.getNotifEmailSolicitudesReview();
            
            case NUEVO_MENSAJE -> 
                usuario.getNotifEmailNuevosMensajes() != null && usuario.getNotifEmailNuevosMensajes();
            
            default -> 
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
        String colorPrincipal = "#4caf50"; // Verde fútbol - color primario del branding
        
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #f9fafb;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                    <tr>
                        <td align="center" style="padding: 40px 16px;">
                            <table role="presentation" style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; background-color: #ffffff;">
                                <!-- Header -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #4caf50 0%%, #388e3c 100%%); padding: 32px 24px; text-align: center;">
                                        <img src="%s/logo.png" alt="Falta Uno" style="width: 80px; height: 80px; margin: 0 auto;" />
                                        <h1 style="color: #ffffff; font-size: 28px; font-weight: 700; margin: 16px 0 0 0;">Falta Uno</h1>
                                        <p style="color: rgba(255, 255, 255, 0.9); font-size: 14px; margin: 8px 0 0 0;">Encuentra tu partido de fútbol</p>
                                    </td>
                                </tr>
                                
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 32px 24px;">
                                        <h2 style="color: #1f2937; font-size: 24px; font-weight: 600; margin: 0 0 16px 0;">
                                            ¡Hola %s!
                                        </h2>
                                        
                                        <div style="background-color: #f9fafb; padding: 20px; border-radius: 8px; margin: 24px 0; border-left: 4px solid %s;">
                                            <h3 style="color: %s; font-size: 20px; font-weight: 600; margin: 0 0 12px 0;">
                                                %s %s
                                            </h3>
                                            <p style="color: #1f2937; font-size: 16px; margin: 0; line-height: 1.6;">
                                                %s
                                            </p>
                                        </div>
                                        
                                        <!-- Action Button -->
                                        <div style="text-align: center; margin: 24px 0;">
                                            <a href="%s" style="display: inline-block; background-color: #4caf50; color: #ffffff; padding: 14px 32px; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 16px;">
                                                Ver Detalles
                                            </a>
                                        </div>
                                        
                                        <hr style="border: 0; border-top: 1px solid #e5e7eb; margin: 24px 0;" />
                                        
                                        <p style="color: #1f2937; font-size: 16px; line-height: 1.6; margin: 0 0 16px 0;">
                                            ¡Nos vemos en la cancha! ⚽
                                        </p>
                                        
                                        <p style="color: #6b7280; font-size: 14px; margin: 0; line-height: 1.5;">
                                            Puedes gestionar tus preferencias de notificación en 
                                            <a href="%s/settings" style="color: #4caf50; text-decoration: none; font-weight: 500;">Configuración</a>.
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f9fafb; padding: 24px; text-align: center; border-top: 1px solid #e5e7eb;">
                                        <p style="color: #6b7280; font-size: 14px; margin: 8px 0;">
                                            © 2025 Falta Uno. Todos los derechos reservados.
                                        </p>
                                        <p style="color: #6b7280; font-size: 14px; margin: 8px 0;">
                                            <a href="%s/help" style="color: #4caf50; text-decoration: none; font-weight: 500;">Centro de Ayuda</a> • 
                                            <a href="%s/terms" style="color: #4caf50; text-decoration: none; font-weight: 500;">Términos</a> • 
                                            <a href="%s/privacy" style="color: #4caf50; text-decoration: none; font-weight: 500;">Privacidad</a>
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
                frontendUrl, // Logo URL
                usuario.getNombre(),
                colorPrincipal, // Border color
                colorPrincipal, // Title color
                emoji, titulo,
                mensaje,
                accionUrl,
                frontendUrl, // Settings link
                frontendUrl, frontendUrl, frontendUrl // Footer links
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
            case NUEVA_SOLICITUD -> "🔔";
            default -> "ℹ️";
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
        // Usar nombre si está disponible, sino usar la parte del email antes del @
        String nombreDisplay = (usuario.getNombre() != null && !usuario.getNombre().isBlank())
                ? usuario.getNombre()
                : usuario.getEmail().split("@")[0];
        
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Bienvenido a Falta Uno</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #f9fafb;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                    <tr>
                        <td align="center" style="padding: 40px 16px;">
                            <table role="presentation" style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; background-color: #ffffff;">
                                <!-- Header -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #4caf50 0%%, #388e3c 100%%); padding: 32px 24px; text-align: center;">
                                        <img src="%s/logo.png" alt="Falta Uno" style="width: 80px; height: 80px; margin: 0 auto;" />
                                        <h1 style="color: #ffffff; font-size: 28px; font-weight: 700; margin: 16px 0 0 0;">¡Bienvenido a Falta Uno!</h1>
                                        <p style="color: rgba(255, 255, 255, 0.9); font-size: 14px; margin: 8px 0 0 0;">Encuentra tu partido de fútbol</p>
                                    </td>
                                </tr>
                                
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 32px 24px;">
                                        <h2 style="color: #1f2937; font-size: 24px; font-weight: 600; margin: 0 0 16px 0;">
                                            ¡Hola %s! 👋
                                        </h2>
                                        <p style="color: #1f2937; font-size: 16px; line-height: 1.6; margin: 0 0 16px 0;">
                                            ¡Nos alegra mucho que te hayas unido a la comunidad de <strong>Falta Uno</strong>! 
                                            Ahora formas parte de la plataforma que conecta jugadores y organiza partidos de fútbol.
                                        </p>
                                        
                                        <div style="background-color: #f9fafb; padding: 20px; border-radius: 8px; margin: 24px 0; border-left: 4px solid #4caf50;">
                                            <h3 style="color: #4caf50; font-size: 18px; font-weight: 600; margin: 0 0 12px 0;">
                                                ⚽ ¿Qué puedes hacer?
                                            </h3>
                                            <ul style="color: #1f2937; font-size: 15px; line-height: 1.8; margin: 0; padding-left: 20px;">
                                                <li>Buscar y unirte a partidos cerca de ti</li>
                                                <li>Organizar tus propios partidos</li>
                                                <li>Conectar con otros jugadores</li>
                                                <li>Calificar y ser calificado después de cada partido</li>
                                                <li>Gestionar tus invitaciones y solicitudes</li>
                                            </ul>
                                        </div>
                                        
                                        <!-- Action Button -->
                                        <div style="text-align: center; margin: 24px 0;">
                                            <a href="%s" style="display: inline-block; background-color: #4caf50; color: #ffffff; padding: 14px 32px; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 16px;">
                                                Explorar Partidos
                                            </a>
                                        </div>
                                        
                                        <p style="color: #1f2937; font-size: 16px; line-height: 1.6; margin: 24px 0 0 0;">
                                            ¡Nos vemos en la cancha! ⚽
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f9fafb; padding: 24px; text-align: center; border-top: 1px solid #e5e7eb;">
                                        <p style="color: #6b7280; font-size: 14px; margin: 8px 0;">
                                            © 2025 Falta Uno. Todos los derechos reservados.
                                        </p>
                                        <p style="color: #6b7280; font-size: 14px; margin: 8px 0;">
                                            <a href="%s/help" style="color: #4caf50; text-decoration: none; font-weight: 500;">Centro de Ayuda</a> • 
                                            <a href="%s/settings" style="color: #4caf50; text-decoration: none; font-weight: 500;">Configuración</a> • 
                                            <a href="%s/terms" style="color: #4caf50; text-decoration: none; font-weight: 500;">Términos</a>
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(frontendUrl, nombreDisplay, frontendUrl, frontendUrl, frontendUrl, frontendUrl);
    }

    /**
     * Enviar código de verificación de email
     */
    @Async
    public void enviarCodigoVerificacion(
            String email,
            String nombre,
            String codigo,
            int minutosExpiracion
    ) {
        // Verificar si el email está configurado
        if (!isEmailConfigured()) {
            log.debug("[EmailService] Email no configurado. Saltando envío.");
            return;
        }

        try {
            String asunto = "[Falta Uno] Código de verificación";
            String cuerpoHtml = construirEmailVerificacion(nombre, codigo, minutosExpiracion);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, "Falta Uno");
            helper.setTo(email);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);

            mailSender.send(mimeMessage);
            
            log.info("[EmailService] ✅ Código de verificación enviado a {}", email);

        } catch (MessagingException e) {
            log.error("[EmailService] ❌ Error enviando código de verificación a {}: {}", email, e.getMessage());
            throw new RuntimeException("Error al enviar el código de verificación", e);
        } catch (Exception e) {
            log.error("[EmailService] ❌ Error inesperado enviando código: {}", e.getMessage(), e);
            throw new RuntimeException("Error al enviar el código de verificación", e);
        }
    }

    /**
     * Construir HTML para email de verificación
     */
    private String construirEmailVerificacion(String nombre, String codigo, int minutosExpiracion) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Código de Verificación</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #f9fafb;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                    <tr>
                        <td align="center" style="padding: 40px 16px;">
                            <table role="presentation" style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; background-color: #ffffff;">
                                <!-- Header -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #4caf50 0%%, #388e3c 100%%); padding: 32px 24px; text-align: center;">
                                        <img src="%s/logo.png" alt="Falta Uno" style="width: 80px; height: 80px; margin: 0 auto;" />
                                        <h1 style="color: #ffffff; font-size: 28px; font-weight: 700; margin: 16px 0 0 0;">Falta Uno</h1>
                                        <p style="color: rgba(255, 255, 255, 0.9); font-size: 14px; margin: 8px 0 0 0;">Encuentra tu partido de fútbol</p>
                                    </td>
                                </tr>
                                
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 32px 24px;">
                                        <h2 style="color: #1f2937; font-size: 24px; font-weight: 600; margin: 0 0 16px 0;">
                                            ¡Hola%s!
                                        </h2>
                                        <p style="color: #1f2937; font-size: 16px; line-height: 1.6; margin: 0 0 16px 0;">
                                            Gracias por registrarte en <strong>Falta Uno</strong>. Para completar tu registro, 
                                            por favor verifica tu dirección de email usando el siguiente código:
                                        </p>
                                        
                                        <!-- Código de Verificación -->
                                        <div style="background-color: #f3f4f6; color: #4caf50; padding: 16px 24px; border-radius: 8px; font-size: 32px; font-weight: 700; letter-spacing: 8px; text-align: center; margin: 24px 0; border: 2px dashed #4caf50; font-family: 'Courier New', monospace;">
                                            %s
                                        </div>
                                        
                                        <p style="color: #1f2937; font-size: 16px; line-height: 1.6; margin: 0 0 16px 0;">
                                            Este código es válido por <strong>%d minutos</strong>.
                                        </p>
                                        
                                        <hr style="border: 0; border-top: 1px solid #e5e7eb; margin: 24px 0;" />
                                        
                                        <p style="color: #6b7280; font-size: 14px; line-height: 1.6; margin: 0;">
                                            <strong>💡 Consejo de seguridad:</strong> Nunca compartas este código con nadie. 
                                            Si no creaste esta cuenta, puedes ignorar este email de forma segura.
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f9fafb; padding: 24px; text-align: center; border-top: 1px solid #e5e7eb;">
                                        <p style="color: #6b7280; font-size: 14px; margin: 8px 0;">
                                            © 2025 Falta Uno. Todos los derechos reservados.
                                        </p>
                                        <p style="color: #6b7280; font-size: 14px; margin: 8px 0;">
                                            <a href="%s/help" style="color: #4caf50; text-decoration: none; font-weight: 500;">Centro de Ayuda</a> • 
                                            <a href="%s/terms" style="color: #4caf50; text-decoration: none; font-weight: 500;">Términos</a> • 
                                            <a href="%s/privacy" style="color: #4caf50; text-decoration: none; font-weight: 500;">Privacidad</a>
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
                frontendUrl, 
                nombre.isEmpty() ? "" : " " + nombre, 
                codigo, 
                minutosExpiracion, 
                frontendUrl, 
                frontendUrl, 
                frontendUrl
            );
    }

    /**
     * Enviar email de recuperación de contraseña
     */
    @Async
    public void enviarEmailRecuperacionPassword(Usuario usuario, String resetLink) {
        log.info("[EmailService] 🔐 === INICIO enviarEmailRecuperacionPassword ===");
        log.info("[EmailService] 👤 Usuario: {} ({})", usuario.getEmail(), usuario.getId());
        log.info("[EmailService] 🔗 Reset link: {}", resetLink);
        
        // Verificar si el email está configurado
        boolean configured = isEmailConfigured();
        log.info("[EmailService] 📧 Email configurado: {}", configured);
        log.info("[EmailService] 📝 fromEmail value: '{}'", fromEmail);
        
        if (!configured) {
            log.error("[EmailService] ❌ Email NO configurado. NO se puede enviar email de recuperación.");
            log.error("[EmailService] 💡 Configurar MAIL_USERNAME y MAIL_PASSWORD en variables de entorno");
            return;
        }

        log.info("[EmailService] ✅ Configuración OK. Procediendo a enviar email a: {}", usuario.getEmail());

        try {
            String nombre = usuario.getNombre() != null ? usuario.getNombre() : "";
            String asunto = "[Falta Uno] Recuperación de Contraseña";
            
            String cuerpoHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Recuperación de Contraseña</title>
                </head>
                <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f3f4f6;">
                    <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color: #f3f4f6; padding: 20px;">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0" border="0" style="background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #4caf50 0%%, #45a049 100%%); padding: 32px 24px; text-align: center;">
                                            <h1 style="color: #ffffff; font-size: 28px; font-weight: bold; margin: 0;">
                                                🔐 Recuperación de Contraseña
                                            </h1>
                                        </td>
                                    </tr>
                                    
                                    <!-- Saludo -->
                                    <tr>
                                        <td style="padding: 32px 24px 16px;">
                                            <p style="color: #374151; font-size: 16px; line-height: 1.6; margin: 0;">
                                                Hola%s,
                                            </p>
                                        </td>
                                    </tr>
                                    
                                    <!-- Mensaje -->
                                    <tr>
                                        <td style="padding: 0 24px 24px;">
                                            <p style="color: #374151; font-size: 16px; line-height: 1.6; margin: 0 0 16px;">
                                                Recibimos una solicitud para restablecer la contraseña de tu cuenta en <strong>Falta Uno</strong>.
                                            </p>
                                            <p style="color: #374151; font-size: 16px; line-height: 1.6; margin: 0;">
                                                Haz clic en el botón de abajo para crear una nueva contraseña:
                                            </p>
                                        </td>
                                    </tr>
                                    
                                    <!-- Botón de acción -->
                                    <tr>
                                        <td style="padding: 0 24px 32px; text-align: center;">
                                            <a href="%s" style="display: inline-block; background-color: #4caf50; color: #ffffff; text-decoration: none; font-size: 16px; font-weight: 600; padding: 14px 32px; border-radius: 8px; box-shadow: 0 2px 4px rgba(76, 175, 80, 0.3);">
                                                Restablecer Contraseña
                                            </a>
                                        </td>
                                    </tr>
                                    
                                    <!-- Info adicional -->
                                    <tr>
                                        <td style="padding: 0 24px 24px;">
                                            <div style="background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 16px; border-radius: 4px;">
                                                <p style="color: #92400e; font-size: 14px; line-height: 1.5; margin: 0;">
                                                    ⚠️ <strong>Este enlace expira en 1 hora</strong> por seguridad.
                                                </p>
                                            </div>
                                        </td>
                                    </tr>
                                    
                                    <!-- Enlace alternativo -->
                                    <tr>
                                        <td style="padding: 0 24px 24px;">
                                            <p style="color: #6b7280; font-size: 14px; line-height: 1.5; margin: 0;">
                                                Si el botón no funciona, copia y pega este enlace en tu navegador:
                                            </p>
                                            <p style="color: #4caf50; font-size: 12px; line-height: 1.5; margin: 8px 0 0; word-break: break-all;">
                                                %s
                                            </p>
                                        </td>
                                    </tr>
                                    
                                    <!-- Aviso de seguridad -->
                                    <tr>
                                        <td style="padding: 0 24px 32px;">
                                            <div style="background-color: #fee2e2; border-left: 4px solid #ef4444; padding: 16px; border-radius: 4px;">
                                                <p style="color: #7f1d1d; font-size: 14px; line-height: 1.5; margin: 0 0 8px;">
                                                    🛡️ <strong>¿No solicitaste esto?</strong>
                                                </p>
                                                <p style="color: #7f1d1d; font-size: 14px; line-height: 1.5; margin: 0;">
                                                    Si no solicitaste restablecer tu contraseña, puedes ignorar este email. Tu cuenta permanecerá segura.
                                                </p>
                                            </div>
                                        </td>
                                    </tr>
                                    
                                    <!-- Footer -->
                                    <tr>
                                        <td style="background-color: #f9fafb; padding: 24px; text-align: center; border-top: 1px solid #e5e7eb;">
                                            <p style="color: #6b7280; font-size: 14px; margin: 8px 0;">
                                                © 2025 Falta Uno. Todos los derechos reservados.
                                            </p>
                                            <p style="color: #6b7280; font-size: 14px; margin: 8px 0;">
                                                <a href="%s/help" style="color: #4caf50; text-decoration: none; font-weight: 500;">Centro de Ayuda</a> • 
                                                <a href="%s/terms" style="color: #4caf50; text-decoration: none; font-weight: 500;">Términos</a> • 
                                                <a href="%s/privacy" style="color: #4caf50; text-decoration: none; font-weight: 500;">Privacidad</a>
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
                    nombre.isEmpty() ? "" : " " + nombre,
                    resetLink,
                    resetLink,
                    frontendUrl,
                    frontendUrl,
                    frontendUrl
                );

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, "Falta Uno");
            helper.setTo(usuario.getEmail());
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);

            log.info("[EmailService] 📤 Enviando email via mailSender...");
            mailSender.send(mimeMessage);
            
            log.info("[EmailService] ✅✅✅ Email de recuperación ENVIADO EXITOSAMENTE a: {}", usuario.getEmail());
            log.info("[EmailService] 🔐 === FIN enviarEmailRecuperacionPassword (EXITOSO) ===");

        } catch (MessagingException e) {
            log.error("[EmailService] ❌ Error de mensajería enviando email de recuperación a {}: {}", 
                usuario.getEmail(), e.getMessage());
            log.error("[EmailService] 🐛 Stacktrace completo:", e);
            log.error("[EmailService] 🔐 === FIN enviarEmailRecuperacionPassword (ERROR) ===");
        } catch (Exception e) {
            log.error("[EmailService] ❌ Error inesperado enviando email de recuperación a {}: {}", 
                usuario.getEmail(), e.getMessage());
            log.error("[EmailService] 🐛 Stacktrace completo:", e);
            log.error("[EmailService] 🔐 === FIN enviarEmailRecuperacionPassword (ERROR) ===");
        }
    }
}

