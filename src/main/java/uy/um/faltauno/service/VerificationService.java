package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.um.faltauno.entity.PendingRegistration;
import uy.um.faltauno.repository.PendingRegistrationRepository;
import uy.um.faltauno.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Servicio para gestionar verificación de email mediante pre-registro.
 * Los usuarios LOCAL deben verificar email ANTES de ser creados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    
    private static final int CODE_EXPIRATION_MINUTES = 15;
    private static final Random RANDOM = new Random();

    /**
     * Crear pre-registro SIN enviar email (permite validaciones previas en controller)
     */
    @Transactional
    public PendingRegistration crearPreRegistroSinEmail(String email, String password) {
        log.info("[VerificationService] Creando pre-registro para: {}", email);
        
        // Validar que el email no esté ya registrado
        if (usuarioRepository.existsByEmail(email)) {
            log.warn("[VerificationService] Email ya registrado: {}", email);
            throw new IllegalStateException("Este email ya está registrado. Si olvidaste tu contraseña, usa la opción 'Recuperar contraseña' en el login.");
        }

        // Eliminar pre-registro anterior si existe
        pendingRegistrationRepository.findByEmail(email).ifPresent(existing -> {
            log.info("[VerificationService] Eliminando pre-registro anterior: {}", email);
            pendingRegistrationRepository.delete(existing);
        });

        // Generar código de 6 dígitos
        String code = generateVerificationCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES);

        // Encriptar contraseña
        String passwordHash = passwordEncoder.encode(password);
        
        // 🔍 DEBUG: Log del password hash generado
        log.info("[VerificationService] 🔍 Password hash generado (primeros 20 chars): {}", 
            passwordHash.substring(0, Math.min(20, passwordHash.length())));

        // Crear pre-registro
        PendingRegistration preRegistro = PendingRegistration.builder()
                .email(email)
                .passwordHash(passwordHash)
                .verificationCode(code)
                .verificationCodeExpiresAt(expiresAt)
                .build();

        PendingRegistration saved = pendingRegistrationRepository.save(preRegistro);
        log.info("[VerificationService] Pre-registro creado, código expira: {}", expiresAt);

        return saved;
    }

    /**
     * Enviar código de verificación para un pre-registro existente
     */
    public void enviarCodigoVerificacionPreRegistro(PendingRegistration preRegistro) {
        try {
            emailService.enviarCodigoVerificacion(
                preRegistro.getEmail(), 
                preRegistro.getEmail(), 
                preRegistro.getVerificationCode(), 
                CODE_EXPIRATION_MINUTES
            );
            log.info("[VerificationService] ✅ Email enviado a: {}", preRegistro.getEmail());
        } catch (Exception e) {
            log.error("[VerificationService] ❌ Error enviando email", e);
            
            // ⚡ IMPORTANTE: En desarrollo, NO eliminar pre-registro si falla el email
            // En producción, el email DEBE funcionar, pero en dev puede estar deshabilitado
            String mailUsername = System.getenv("MAIL_USERNAME");
            boolean isEmailConfigured = mailUsername != null && !mailUsername.isBlank();
            
            if (!isEmailConfigured) {
                log.warn("[VerificationService] ⚠️ Email NO configurado. Pre-registro creado sin enviar email.");
                log.warn("[VerificationService] 🔍 Código de verificación (SOLO DEV): {}", preRegistro.getVerificationCode());
                return; // ✅ NO lanzar excepción si el email no está configurado
            }
            
            // Si el email está configurado pero falló, entonces SÍ es un error crítico
            pendingRegistrationRepository.delete(preRegistro);
            throw new IllegalStateException("Error al enviar el código de verificación");
        }
    }

    /**
     * Crear pre-registro Y enviar email (método legacy - mantener para compatibilidad)
     * @deprecated Use crearPreRegistroSinEmail + enviarCodigoVerificacionPreRegistro para mejor control
     */
    @Deprecated
    public PendingRegistration crearPreRegistro(String email, String password) {
        PendingRegistration preRegistro = crearPreRegistroSinEmail(email, password);
        enviarCodigoVerificacionPreRegistro(preRegistro);
        return preRegistro;
    }

    /**
     * Verificar código y devolver datos del pre-registro
     * Si el código es válido, se puede proceder a crear el usuario
     */
    @Transactional(readOnly = true)
    public PendingRegistration verificarCodigo(String email, String codigo) {
        log.info("[VerificationService] Verificando código para: {}", email);
        
        PendingRegistration preRegistro = pendingRegistrationRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[VerificationService] Pre-registro no encontrado: {}", email);
                    return new IllegalArgumentException("No hay un registro pendiente para este email");
                });

        // Validar código
        if (!preRegistro.isValidCode(codigo)) {
            if (preRegistro.isCodeExpired()) {
                log.warn("[VerificationService] Código expirado para: {}", email);
                throw new IllegalStateException("El código ha expirado. Solicita uno nuevo.");
            } else {
                log.warn("[VerificationService] Código incorrecto para: {}", email);
                throw new IllegalStateException("Código incorrecto");
            }
        }

        log.info("[VerificationService] ✅ Código válido para: {}", email);
        return preRegistro;
    }

    /**
     * Reenviar código de verificación
     */
    @Transactional
    public void reenviarCodigo(String email) {
        log.info("[VerificationService] Reenviando código para: {}", email);
        
        PendingRegistration preRegistro = pendingRegistrationRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No hay un registro pendiente para este email"));

        // Generar nuevo código
        String newCode = generateVerificationCode();
        LocalDateTime newExpiresAt = LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES);

        preRegistro.setVerificationCode(newCode);
        preRegistro.setVerificationCodeExpiresAt(newExpiresAt);
        pendingRegistrationRepository.save(preRegistro);

        log.info("[VerificationService] Nuevo código generado, expira: {}", newExpiresAt);

        // Enviar email
        try {
            emailService.enviarCodigoVerificacion(email, email, newCode, CODE_EXPIRATION_MINUTES);
            log.info("[VerificationService] ✅ Código reenviado a: {}", email);
        } catch (Exception e) {
            log.error("[VerificationService] ❌ Error reenviando email", e);
            throw new IllegalStateException("Error al reenviar el código");
        }
    }

    /**
     * Limpiar pre-registro después de crear el usuario
     */
    @Transactional
    public void limpiarPreRegistro(String email) {
        log.info("[VerificationService] Limpiando pre-registro: {}", email);
        pendingRegistrationRepository.deleteByEmail(email);
    }

    /**
     * Limpiar registros expirados (ejecutar periódicamente)
     */
    @Transactional
    public int limpiarRegistrosExpirados() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        int deleted = pendingRegistrationRepository.deleteExpiredRegistrations(cutoff);
        log.info("[VerificationService] 🧹 Limpiados {} registros expirados", deleted);
        return deleted;
    }

    /**
     * Generar código numérico de 6 dígitos
     */
    private String generateVerificationCode() {
        int code = 100000 + RANDOM.nextInt(900000); // Entre 100000 y 999999
        return String.valueOf(code);
    }

    /**
     * Verificar si un email tiene pre-registro pendiente
     */
    public boolean tienePreRegistroPendiente(String email) {
        return pendingRegistrationRepository.existsByEmail(email);
    }

    /**
     * Verificar si un usuario tiene email verificado
     */
    public boolean isEmailVerified(java.util.UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(u -> Boolean.TRUE.equals(u.getEmailVerified()))
                .orElse(false);
    }
}
