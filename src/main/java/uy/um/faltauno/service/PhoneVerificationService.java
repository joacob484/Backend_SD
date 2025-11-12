package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * Servicio para verificación de números de celular vía SMS
 * Utiliza códigos de 6 dígitos con expiración de 15 minutos
 * Máximo 3 intentos de verificación por código
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneVerificationService {

    private final UsuarioRepository usuarioRepository;
    private final SmsService smsService; // Servicio para enviar SMS (Twilio, AWS SNS, etc.)
    
    @Value("${app.phone.verification.enabled:true}")
    private boolean verificationEnabled;
    
    private static final int CODE_EXPIRY_MINUTES = 15;
    private static final int MAX_ATTEMPTS = 3;

    /**
     * Enviar código de verificación al celular del usuario
     */
    @Transactional
    public void enviarCodigoVerificacion(UUID usuarioId) {
        log.info("[PhoneVerification] Enviando código a usuario: {}", usuarioId);
        
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        if (usuario.getCelular() == null || usuario.getCelular().isBlank()) {
            throw new IllegalStateException("El usuario no tiene número de celular registrado");
        }
        
        if (Boolean.TRUE.equals(usuario.getCelularVerificado())) {
            log.warn("[PhoneVerification] Celular ya verificado para usuario: {}", usuarioId);
            throw new IllegalStateException("El celular ya está verificado");
        }
        
        // Generar código aleatorio de 6 dígitos
        String codigo = generarCodigoAleatorio();
        
        // Guardar código y expiración
        usuario.setCodigoVerificacion(codigo);
        usuario.setCodigoVerificacionExpira(LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES));
        usuario.setCodigoVerificacionIntentos(0); // Reset intentos
        
        usuarioRepository.save(usuario);
        
        // Enviar SMS
        if (verificationEnabled) {
            try {
                String mensaje = String.format(
                    "Tu código de verificación de Falta Uno es: %s. Válido por %d minutos.",
                    codigo,
                    CODE_EXPIRY_MINUTES
                );
                smsService.enviarSms(usuario.getCelular(), mensaje);
                log.info("[PhoneVerification] ✅ SMS enviado a: {}", usuario.getCelular());
            } catch (Exception e) {
                log.error("[PhoneVerification] ❌ Error enviando SMS", e);
                throw new RuntimeException("No se pudo enviar el código de verificación. Intenta nuevamente.");
            }
        } else {
            log.warn("[PhoneVerification] ⚠️ Verificación deshabilitada - Código: {}", codigo);
        }
    }

    /**
     * Verificar código ingresado por el usuario
     */
    @Transactional
    public boolean verificarCodigo(UUID usuarioId, String codigo) {
        log.info("[PhoneVerification] Verificando código para usuario: {}", usuarioId);
        
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        if (Boolean.TRUE.equals(usuario.getCelularVerificado())) {
            return true; // Ya estaba verificado
        }
        
        if (usuario.getCodigoVerificacion() == null) {
            throw new IllegalStateException("No hay código de verificación activo. Solicita uno nuevo.");
        }
        
        // Verificar expiración
        if (usuario.getCodigoVerificacionExpira() == null || 
            LocalDateTime.now().isAfter(usuario.getCodigoVerificacionExpira())) {
            log.warn("[PhoneVerification] Código expirado para usuario: {}", usuarioId);
            throw new IllegalStateException("El código ha expirado. Solicita uno nuevo.");
        }
        
        // Verificar intentos
        if (usuario.getCodigoVerificacionIntentos() >= MAX_ATTEMPTS) {
            log.warn("[PhoneVerification] Máximo de intentos alcanzado para usuario: {}", usuarioId);
            throw new IllegalStateException("Máximo de intentos alcanzado. Solicita un nuevo código.");
        }
        
        // Verificar código
        if (!codigo.equals(usuario.getCodigoVerificacion())) {
            // Incrementar intentos fallidos
            usuario.setCodigoVerificacionIntentos(usuario.getCodigoVerificacionIntentos() + 1);
            usuarioRepository.save(usuario);
            
            int intentosRestantes = MAX_ATTEMPTS - usuario.getCodigoVerificacionIntentos();
            log.warn("[PhoneVerification] Código incorrecto. Intentos restantes: {}", intentosRestantes);
            
            throw new IllegalStateException(
                String.format("Código incorrecto. Te quedan %d intento(s).", intentosRestantes)
            );
        }
        
        // ✅ Código correcto - Marcar celular como verificado
        usuario.setCelularVerificado(true);
        usuario.setCodigoVerificacion(null); // Limpiar código usado
        usuario.setCodigoVerificacionExpira(null);
        usuario.setCodigoVerificacionIntentos(0);
        
        usuarioRepository.save(usuario);
        
        log.info("[PhoneVerification] ✅ Celular verificado exitosamente para usuario: {}", usuarioId);
        return true;
    }

    /**
     * Obtener estado de verificación del celular
     */
    @Transactional(readOnly = true)
    public boolean esCelularVerificado(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(u -> Boolean.TRUE.equals(u.getCelularVerificado()))
                .orElse(false);
    }

    /**
     * Reenviar código de verificación
     */
    @Transactional
    public void reenviarCodigo(UUID usuarioId) {
        log.info("[PhoneVerification] Reenviando código a usuario: {}", usuarioId);
        
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        // Limpiar código anterior
        usuario.setCodigoVerificacion(null);
        usuario.setCodigoVerificacionExpira(null);
        usuario.setCodigoVerificacionIntentos(0);
        usuarioRepository.save(usuario);
        
        // Enviar nuevo código
        enviarCodigoVerificacion(usuarioId);
    }

    /**
     * Generar código aleatorio de 6 dígitos
     */
    private String generarCodigoAleatorio() {
        Random random = new Random();
        int codigo = 100000 + random.nextInt(900000); // 100000-999999
        return String.valueOf(codigo);
    }
}
