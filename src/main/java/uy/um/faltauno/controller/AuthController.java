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

            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Email requerido", false));
            }

            if (password == null || password.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Contraseña requerida", false));
            }

            // Validar longitud mínima de contraseña
            if (password.length() < 8) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "La contraseña debe tener al menos 8 caracteres", false));
            }

            // ✅ NUEVO: Verificar si existe usuario eliminado recuperable
            if (usuarioService.hasRecoverableDeletedUser(email)) {
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

            PendingRegistration preRegistro = verificationService.crearPreRegistro(email, password);

            log.info("[AuthController] Pre-registro creado para email: {}", email);

            return ResponseEntity.ok(new ApiResponse<>(
                    Map.of(
                        "email", preRegistro.getEmail(),
                        "message", "Código de verificación enviado a tu email"
                    ),
                    "Pre-registro exitoso. Revisa tu email para el código de verificación.",
                    true
            ));

        } catch (IllegalStateException e) {
            log.error("[AuthController] Error de estado en pre-registro", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[AuthController] Error en pre-registro", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al procesar el registro", false));
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

            // 2. Crear usuario con password ya encriptado
            dto.setPassword(preRegistro.getPasswordHash()); // Usar password hash del pre-registro
            dto.setEmailVerified(true); // Marcar como verificado
            
            UsuarioDTO nuevoUsuario = usuarioService.createUsuario(dto);
            
            // 3. Obtener la entidad Usuario para generar token con tokenVersion
            Usuario usuarioEntity = usuarioService.findUsuarioEntityById(nuevoUsuario.getId());
            
            // 4. Generar token JWT
            String token = jwtUtil.generateToken(
                usuarioEntity.getId(), 
                usuarioEntity.getEmail(), 
                usuarioEntity.getTokenVersion()
            );
            
            // 5. Agregar token al DTO de respuesta
            nuevoUsuario.setPassword(null); // No retornar password en response
            
            // 6. Limpiar pre-registro
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
