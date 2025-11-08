package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.dto.UsuarioDTO;
import uy.um.faltauno.entity.PendingRegistration;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.service.VerificationService;
import uy.um.faltauno.service.UsuarioService;
import uy.um.faltauno.config.JwtUtil;

import java.util.Map;

/**
 * Controller para autenticación y registro
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final VerificationService verificationService;
    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;

    /**
     * Verificar si un email ya está registrado
     * GET /api/auth/check-email?email=user@example.com
     */
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkEmail(
            @RequestParam String email
    ) {
        try {
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Email requerido", false));
            }

            boolean exists = usuarioService.existsByEmail(email);
            boolean hasDeletedRecoverable = usuarioService.hasRecoverableDeletedUser(email);

            Map<String, Object> result = Map.of(
                "exists", exists,
                "hasDeletedRecoverable", hasDeletedRecoverable,
                "available", !exists && !hasDeletedRecoverable
            );

            return ResponseEntity.ok(new ApiResponse<>(result, "Verificación completada", true));

        } catch (Exception e) {
            log.error("[AuthController] Error verificando email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al verificar email", false));
        }
    }

    /**
     * Pre-registro: validar datos y enviar código de verificación
     * POST /api/auth/pre-register
     * 
     * Body: { "email": "user@example.com", "password": "password123" }
     */
    @PostMapping("/pre-register")
    public ResponseEntity<ApiResponse<Map<String, String>>> preRegister(
            @RequestBody Map<String, String> request
    ) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            log.info("[AuthController] 📝 Pre-registro iniciado para: {}", email);

            if (email == null || email.isBlank()) {
                log.warn("[AuthController] ❌ Email requerido");
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Email requerido", false));
            }

            if (password == null || password.isBlank()) {
                log.warn("[AuthController] ❌ Contraseña requerida");
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Contraseña requerida", false));
            }

            // Validar longitud mínima de contraseña
            if (password.length() < 8) {
                log.warn("[AuthController] ❌ Contraseña muy corta");
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "La contraseña debe tener al menos 8 caracteres", false));
            }

            // ✅ NUEVO: Verificar si existe usuario eliminado recuperable
            log.info("[AuthController] Verificando usuario eliminado recuperable...");
            boolean hasDeletedUser = usuarioService.hasRecoverableDeletedUser(email);
            log.info("[AuthController] Usuario eliminado recuperable: {}", hasDeletedUser);
            
            if (hasDeletedUser) {
                Map<String, String> deletedUserData = new java.util.HashMap<>();
                deletedUserData.put("deletedUser", "true");
                deletedUserData.put("canRecover", "true");
                deletedUserData.put("email", email);
                
                return ResponseEntity.status(HttpStatus.CONFLICT)  // 409 Conflict
                        .body(new ApiResponse<>(
                                deletedUserData,
                                "Ya existe una cuenta eliminada con este email. ¿Deseas recuperarla o esperar a que se elimine definitivamente?",
                                false
                        ));
            }

            // ✅ Crear pre-registro sin enviar email (permite validaciones previas en controller)
            log.info("[AuthController] Creando pre-registro...");
            PendingRegistration preRegistro = verificationService.crearPreRegistroSinEmail(email, password);

            // ✅ Solo enviar email si todo salió bien
            log.info("[AuthController] Enviando código de verificación...");
            verificationService.enviarCodigoVerificacionPreRegistro(preRegistro);

            log.info("[AuthController] ✅ Pre-registro creado y email enviado: {}", email);

            // ⚡ Para desarrollo: incluir código si email no está configurado
            String mailUsername = System.getenv("MAIL_USERNAME");
            boolean isEmailConfigured = mailUsername != null && !mailUsername.isBlank();
            
            Map<String, String> responseData = new java.util.HashMap<>();
            responseData.put("email", preRegistro.getEmail());
            responseData.put("message", "Código de verificación enviado a tu email");
            
            if (!isEmailConfigured) {
                log.warn("[AuthController] 🔍 Email NO configurado - Incluyendo código en respuesta (SOLO DEV)");
                responseData.put("verificationCode", preRegistro.getVerificationCode());
                responseData.put("debugMode", "true");
            }

            return ResponseEntity.ok(new ApiResponse<>(
                    responseData,
                    "Pre-registro exitoso. Revisa tu email para el código de verificación.",
                    true
            ));

        } catch (IllegalStateException e) {
            log.error("[AuthController] ❌ Error de estado en pre-registro: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[AuthController] ❌ Error INESPERADO en pre-registro", e);
            log.error("[AuthController] ❌ Tipo de excepción: {}", e.getClass().getName());
            log.error("[AuthController] ❌ Mensaje: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("[AuthController] ❌ Causa: {}", e.getCause().getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al procesar el registro: " + e.getMessage(), false));
        }
    }

    /**
     * Completar registro: verificar código y crear usuario
     * POST /api/auth/complete-register
     * 
     * Body: {
     *   "email": "user@example.com",
     *   "verificationCode": "123456",
     *   "nombre": "Juan",
     *   "apellido": "Pérez",
     *   "fechaNacimiento": "1990-01-01",
     *   "celular": "+598..."
     * }
     */
    @PostMapping("/complete-register")
    public ResponseEntity<ApiResponse<Object>> completeRegister(
            @RequestBody UsuarioDTO dto
    ) {
        try {
            String email = dto.getEmail();
            String codigo = dto.getVerificationCode();

            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Email requerido", false));
            }

            if (codigo == null || codigo.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Código de verificación requerido", false));
            }

            // 1. Verificar código
            PendingRegistration preRegistro = verificationService.verificarCodigo(email, codigo.trim());

            // 🔍 DEBUG: Log del password hash que se va a guardar
            log.info("[AuthController] 🔍 Password hash del pre-registro (primeros 20 chars): {}", 
                preRegistro.getPasswordHash() != null ? preRegistro.getPasswordHash().substring(0, Math.min(20, preRegistro.getPasswordHash().length())) : "null");

            // 2. Crear o actualizar usuario con password ya encriptado
            dto.setPassword(preRegistro.getPasswordHash()); // Usar password hash del pre-registro
            dto.setEmailVerified(true); // Marcar como verificado

            Usuario usuarioEntity;
            UsuarioDTO nuevoUsuario;

            if (usuarioService.existsByEmail(email)) {
                // Si el usuario ya existe (p. ej. creado por otro flujo), actualizamos perfil
                usuarioEntity = usuarioService.createOrUpdateUserAfterVerification(email, preRegistro.getPasswordHash(), dto);
                nuevoUsuario = usuarioService.getUsuario(usuarioEntity.getId());
            } else {
                // Crear nuevo usuario (caso normal)
                nuevoUsuario = usuarioService.createUsuario(dto);
                usuarioEntity = usuarioService.findUsuarioEntityById(nuevoUsuario.getId());
            }

            // 3. Generar token JWT
            String token = jwtUtil.generateToken(
                usuarioEntity.getId(), 
                usuarioEntity.getEmail(), 
                usuarioEntity.getTokenVersion()
            );

            // 4. Agregar token al DTO de respuesta
            nuevoUsuario.setPassword(null); // No retornar password en response

            // 5. Limpiar pre-registro
            verificationService.limpiarPreRegistro(email);

            log.info("[AuthController] Usuario creado exitosamente con token: {}", nuevoUsuario.getEmail());

            // Crear respuesta con usuario y token
            Map<String, Object> responseData = Map.of(
                "usuario", nuevoUsuario,
                "token", token
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(
                            responseData,
                            "Registro completado exitosamente",
                            true
                    ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("[AuthController] Error de validación en registro", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[AuthController] Error completando registro", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al completar el registro", false));
        }
    }
}
