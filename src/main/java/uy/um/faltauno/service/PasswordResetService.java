package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.um.faltauno.entity.PasswordResetToken;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.PasswordResetTokenRepository;
import uy.um.faltauno.repository.UsuarioRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:https://faltauno-frontend-169771742214.us-central1.run.app}")
    private String frontendUrl;
    
    @Value("${spring.mail.username:#{null}}")
    private String mailUsername;

    private static final int MAX_TOKENS_POR_HORA = 3;
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Solicitar recuperación de contraseña.
     * Envía email con link de recuperación.
     */
    @Transactional
    public String solicitarRecuperacion(String email) {
        log.info("[PasswordReset] 🔍 Buscando usuario con email: {}", email);
        
        // Buscar usuario (incluso si está eliminado, puede querer recuperar contraseña)
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        
        if (usuario == null) {
            // Por seguridad, no revelar si el email existe o no al cliente
            // pero loguear para debugging
            log.warn("[PasswordReset] ❌ Usuario NO encontrado para email: {}", email);
            log.warn("[PasswordReset] ⚠️ No se enviará email porque el usuario no existe");
            return null; // Retornar null para no revelar existencia de usuario
        }
        
        log.info("[PasswordReset] ✅ Usuario encontrado: {} {} (ID: {})", 
                usuario.getNombre(), usuario.getApellido(), usuario.getId());

        // Verificar que no esté spameando (máximo 3 solicitudes por hora)
        LocalDateTime unaHoraAtras = LocalDateTime.now().minusHours(1);
        long tokensRecientes = passwordResetTokenRepository.contarTokensRecientesDelUsuario(usuario, unaHoraAtras);
        
        if (tokensRecientes >= MAX_TOKENS_POR_HORA) {
            log.warn("[PasswordReset] Usuario {} excedió límite de solicitudes ({})", email, tokensRecientes);
            throw new IllegalStateException("Has excedido el límite de solicitudes. Intenta nuevamente en una hora.");
        }

        // Invalidar tokens anteriores
        passwordResetTokenRepository.invalidarTokensDelUsuario(usuario);

        // Generar token seguro
        String token = generarTokenSeguro();

        // Crear registro de token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .usuario(usuario)
                .usado(false)
                .creadoEn(LocalDateTime.now())
                .expiraEn(LocalDateTime.now().plusHours(1))
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Construir link de reset
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        // ⚡ CORREGIDO: Usar valor inyectado por Spring en lugar de System.getenv
        boolean isEmailConfigured = mailUsername != null && !mailUsername.isBlank();
        
        log.info("[PasswordReset] 📧 Configuración de email: {}", 
                isEmailConfigured ? "CONFIGURADO" : "NO CONFIGURADO");
        log.info("[PasswordReset] 📝 MAIL_USERNAME: {}", 
                mailUsername != null && !mailUsername.isBlank() ? "***@***" : "(vacío)");

        if (isEmailConfigured) {
            // Enviar email (modo producción)
            log.info("[PasswordReset] 📤 Intentando enviar email a: {}", email);
            try {
                emailService.enviarEmailRecuperacionPassword(usuario, resetLink);
                log.info("[PasswordReset] ✅ Token generado y email ENVIADO a: {}", email);
            } catch (Exception e) {
                log.error("[PasswordReset] ❌ ERROR enviando email a {}: {}", email, e.getMessage(), e);
                log.error("[PasswordReset] 🐛 Stacktrace completo:", e);
                // No lanzar error - el token ya fue creado, el usuario puede intentar solicitar otro
            }
            return null; // No devolver link en producción
        } else {
            // Modo desarrollo: NO enviar email, devolver link
            log.warn("[PasswordReset] ⚠️ Email NO configurado - Devolviendo link directamente (SOLO DEV)");
            log.warn("[PasswordReset] 🔗 Reset link (SOLO DEV): {}", resetLink);
            log.warn("[PasswordReset] 💡 Para habilitar emails: configurar MAIL_USERNAME y MAIL_PASSWORD");
            return resetLink; // Devolver link para modo desarrollo
        }
    }

    /**
     * Verificar si un token es válido
     */
    @Transactional(readOnly = true)
    public boolean validarToken(String token) {
        return passwordResetTokenRepository.findByToken(token)
                .map(PasswordResetToken::esValido)
                .orElse(false);
    }

    /**
     * Restablecer contraseña usando token
     */
    @Transactional
    public void restablecerPassword(String token, String nuevaPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (!resetToken.esValido()) {
            throw new IllegalStateException("El token ha expirado o ya fue utilizado");
        }

        // Validar contraseña
        if (nuevaPassword == null || nuevaPassword.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }

        Usuario usuario = resetToken.getUsuario();
        
        // Actualizar contraseña
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);

        // Marcar token como usado
        resetToken.setUsado(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("[PasswordReset] ✅ Contraseña restablecida para usuario: {}", usuario.getEmail());
    }

    /**
     * Cleanup automático de tokens expirados (llamado por scheduler)
     */
    @Transactional
    public int limpiarTokensExpirados() {
        LocalDateTime ahora = LocalDateTime.now();
        int eliminados = passwordResetTokenRepository.eliminarTokensExpirados(ahora);
        
        if (eliminados > 0) {
            log.info("[PasswordReset] 🧹 Limpieza: {} tokens expirados eliminados", eliminados);
        }
        
        return eliminados;
    }

    /**
     * Generar token seguro de 32 bytes (256 bits)
     */
    private String generarTokenSeguro() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
