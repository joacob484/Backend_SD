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
    private static final int CODE_EXPIRATION_MINUTES = 5;
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Solicitar recuperación de contraseña.
     * Envía email con código de 6 dígitos (igual que verificación).
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

        // Generar código de 6 dígitos (igual que verificación)
        String codigo = generarCodigoVerificacion();

        // Crear registro de token con código
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(codigo) // Ahora es un código de 6 dígitos
                .usuario(usuario)
                .usado(false)
                .creadoEn(LocalDateTime.now())
                .expiraEn(LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES))
                .build();

        passwordResetTokenRepository.save(resetToken);
        
        log.info("[PasswordReset] ✅ Código generado, expira en {} minutos", CODE_EXPIRATION_MINUTES);

        // ⚡ CORREGIDO: Usar valor inyectado por Spring en lugar de System.getenv
        boolean isEmailConfigured = mailUsername != null && !mailUsername.isBlank();
        
        log.info("[PasswordReset] 📧 Configuración de email: {}", 
                isEmailConfigured ? "CONFIGURADO" : "NO CONFIGURADO");
        log.info("[PasswordReset] 📝 MAIL_USERNAME: {}", 
                mailUsername != null && !mailUsername.isBlank() ? "***@***" : "(vacío)");

        if (isEmailConfigured) {
            // Enviar email con código (modo producción)
            log.info("[PasswordReset] 📤 Intentando enviar código a: {}", email);
            try {
                String nombreUsuario = usuario.getNombre() != null ? usuario.getNombre() : "";
                emailService.enviarCodigoRecuperacionPassword(email, nombreUsuario, codigo, CODE_EXPIRATION_MINUTES);
                log.info("[PasswordReset] ✅ Código generado y email ENVIADO a: {}", email);
            } catch (Exception e) {
                log.error("[PasswordReset] ❌ ERROR enviando email a {}: {}", email, e.getMessage(), e);
                log.error("[PasswordReset] 🐛 Stacktrace completo:", e);
                // No lanzar error - el código ya fue creado, el usuario puede intentar solicitar otro
            }
            return null; // No devolver código en producción
        } else {
            // Modo desarrollo: NO enviar email, devolver código
            log.warn("[PasswordReset] ⚠️ Email NO configurado - Devolviendo código directamente (SOLO DEV)");
            log.warn("[PasswordReset] 🔢 Código de recuperación (SOLO DEV): {}", codigo);
            log.warn("[PasswordReset] 💡 Para habilitar emails: configurar MAIL_USERNAME y MAIL_PASSWORD");
            return codigo; // Devolver código para modo desarrollo
        }
    }

    /**
     * Validar código de recuperación para un email específico
     */
    @Transactional(readOnly = true)
    public boolean validarCodigo(String email, String codigo) {
        log.info("[PasswordReset] Validando código para email: {}", email);
        
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) {
            log.warn("[PasswordReset] Usuario no encontrado para validación: {}", email);
            return false;
        }
        
        return passwordResetTokenRepository.findByToken(codigo)
                .filter(token -> token.getUsuario().equals(usuario))
                .map(PasswordResetToken::esValido)
                .orElse(false);
    }
    
    /**
     * Verificar si un token es válido (mantener para compatibilidad)
     * @deprecated Usar validarCodigo(email, codigo) en su lugar
     */
    @Deprecated
    @Transactional(readOnly = true)
    public boolean validarToken(String token) {
        return passwordResetTokenRepository.findByToken(token)
                .map(PasswordResetToken::esValido)
                .orElse(false);
    }

    /**
     * Restablecer contraseña usando email y código
     */
    @Transactional
    public void restablecerPasswordConCodigo(String email, String codigo, String nuevaPassword) {
        log.info("[PasswordReset] Restableciendo contraseña para: {}", email);
        
        // Buscar usuario
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        // Buscar token/código
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(codigo)
                .filter(token -> token.getUsuario().equals(usuario))
                .orElseThrow(() -> new IllegalArgumentException("Código inválido"));

        if (!resetToken.esValido()) {
            throw new IllegalStateException("El código ha expirado o ya fue utilizado");
        }

        // Validar contraseña
        if (nuevaPassword == null || nuevaPassword.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
        
        // Actualizar contraseña
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);

        // Marcar código como usado
        resetToken.setUsado(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("[PasswordReset] ✅ Contraseña restablecida para usuario: {}", usuario.getEmail());
    }
    
    /**
     * Restablecer contraseña usando token (mantener para compatibilidad)
     * @deprecated Usar restablecerPasswordConCodigo en su lugar
     */
    @Deprecated
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
     * Generar código de verificación de 6 dígitos (igual que verificación de email)
     */
    private String generarCodigoVerificacion() {
        int codigo = 100000 + secureRandom.nextInt(900000); // Entre 100000 y 999999
        return String.valueOf(codigo);
    }
}
