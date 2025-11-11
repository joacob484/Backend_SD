package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.entity.PendingRegistration;
import uy.um.faltauno.service.VerificationService;

import java.util.Map;

/**
 * Controller para gestionar verificación de email mediante pre-registro
 */
@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class VerificationController {

    private final VerificationService verificationService;
    private final uy.um.faltauno.service.UsuarioService usuarioService;
    private final uy.um.faltauno.config.JwtUtil jwtUtil;

    /**
     * Crear pre-registro y enviar código
     * POST /api/verification/send-code
     */
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Map<String, String>>> enviarCodigo(
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

            // ✅ Usar nuevo método sin deprecación
            PendingRegistration preRegistro = verificationService.crearPreRegistroSinEmail(email, password);
            verificationService.enviarCodigoVerificacionPreRegistro(preRegistro);

            return ResponseEntity.ok(new ApiResponse<>(
                    Map.of("email", preRegistro.getEmail()),
                    "Código de verificación enviado exitosamente",
                    true
            ));

        } catch (IllegalStateException e) {
            log.error("[VerificationController] Estado inválido", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[VerificationController] Error enviando código", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al enviar el código", false));
        }
    }

    /**
     * Verificar código ingresado
     * POST /api/verification/verify-code
     * Retorna el passwordHash para crear el usuario
     */
    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verificarCodigo(
            @RequestBody Map<String, String> request
    ) {
        try {
            String email = request.get("email");
            String codigo = request.get("code");

            log.info("[VerificationController] 📧 Iniciando verificación para email: {}", email);

            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Email requerido", false));
            }

            if (codigo == null || codigo.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Código requerido", false));
            }

            log.info("[VerificationController] 🔍 Verificando código...");
            PendingRegistration preRegistro = verificationService.verificarCodigo(email, codigo.trim());
            log.info("[VerificationController] ✅ Código verificado correctamente");

        // ===== NEW: Crear o actualizar usuario automáticamente para evitar que el
        // frontend deba llamar a otro endpoint si saltó el paso de "complete-register".
        // Si el frontend envía datos de perfil en el request (nombre, apellido, fechaNacimiento, celular)
        // los usaremos para poblar el usuario; si no, creamos un usuario mínimo y el frontend debe
        // redirigir al usuario a completar su perfil.

        uy.um.faltauno.dto.UsuarioDTO profileDto = new uy.um.faltauno.dto.UsuarioDTO();
        profileDto.setNombre(request.get("nombre"));
        profileDto.setApellido(request.get("apellido"));
        profileDto.setFechaNacimiento(request.get("fechaNacimiento"));
        profileDto.setCelular(request.get("celular"));

        log.info("[VerificationController] 👤 Creando/actualizando usuario...");
        // Crear o actualizar usuario
        uy.um.faltauno.entity.Usuario usuario = 
            usuarioService.createOrUpdateUserAfterVerification(preRegistro.getEmail(), preRegistro.getPasswordHash(), profileDto);
        
        // ⚡ CRÍTICO: Validar que el usuario fue creado correctamente
        if (usuario == null) {
            log.error("[VerificationController] ❌ Error: usuario retornado es null");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(null, "Error al crear usuario después de verificación", false));
        }
        
        log.info("[VerificationController] ✅ Usuario creado/actualizado: ID={}", usuario.getId());

        log.info("[VerificationController] 🔑 Generando token JWT...");
        // ✅ FIX: Usar directamente el usuario retornado en lugar de hacer segunda consulta
        // Validar campos requeridos antes de generar token
        if (usuario.getTokenVersion() == null) {
            usuario.setTokenVersion(1); // Default si no existe
        }
        if (usuario.getRol() == null || usuario.getRol().isBlank()) {
            usuario.setRol("USER"); // Default si no existe
        }
        
        String token = jwtUtil.generateToken(usuario.getId(), usuario.getEmail(), usuario.getTokenVersion(), usuario.getRol());
        log.info("[VerificationController] ✅ Token generado");

        log.info("[VerificationController] 🧹 Limpiando pre-registro...");
        // Limpiar pre-registro
        verificationService.limpiarPreRegistro(email);
        log.info("[VerificationController] ✅ Pre-registro limpiado");

        log.info("[VerificationController] 📦 Preparando respuesta...");
        // Preparar DTO para respuesta
        uy.um.faltauno.dto.UsuarioDTO usuarioDto = usuarioService.getUsuario(usuario.getId());

        Map<String, Object> responseData = Map.of(
            "verified", true,
            "usuario", usuarioDto,
            "token", token
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(responseData, "Email verificado y usuario creado/actualizado", true));

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("[VerificationController] Error de validación: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(
                            Map.of("verified", false),
                            e.getMessage(),
                            false
                    ));
                    
        } catch (Exception e) {
            log.error("[VerificationController] ❌ Error inesperado verificando código - Tipo: {} | Mensaje: {}", 
                    e.getClass().getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al verificar el código: " + e.getMessage(), false));
        }
    }

    /**
     * Reenviar código de verificación
     * POST /api/verification/resend-code
     */
    @PostMapping("/resend-code")
    public ResponseEntity<ApiResponse<Void>> reenviarCodigo(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Email requerido", false));
            }

            verificationService.reenviarCodigo(email);

            return ResponseEntity.ok(new ApiResponse<>(
                    null,
                    "Código reenviado exitosamente",
                    true
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[VerificationController] Error reenviando código", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al reenviar el código", false));
        }
    }

    /**
     * ⚡ NUEVO: Solicitar código de verificación para usuario existente (login con emailVerified=false)
     * POST /api/verification/request-login-verification
     * 
     * Este endpoint es para usuarios que hicieron login pero no tienen el email verificado.
     * NO requiere password porque el usuario ya está autenticado.
     */
    @PostMapping("/request-login-verification")
    public ResponseEntity<ApiResponse<Map<String, String>>> solicitarVerificacionLogin(
            @RequestBody Map<String, String> request
    ) {
        try {
            String email = request.get("email");
            
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Email requerido", false));
            }

            // Verificar si ya existe un pre-registro
            verificationService.reenviarCodigo(email);

            log.info("[VerificationController] Código de verificación enviado para login: {}", email);

            return ResponseEntity.ok(new ApiResponse<>(
                    Map.of("email", email, "message", "Código enviado"),
                    "Código de verificación enviado a tu email",
                    true
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("[VerificationController] Error en request-login-verification", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[VerificationController] Error enviando código de verificación para login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al enviar el código", false));
        }
    }

    /**
     * Cancelar un pre-registro (cuando el usuario vuelve atrás en el wizard de registro)
     * DELETE /api/verification/cancel-pre-registration
     * Body: { "email": "user@example.com" }
     */
    @DeleteMapping("/cancel-pre-registration")
    public ResponseEntity<ApiResponse<Void>> cancelarPreRegistro(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(null, "Email requerido", false));
            }

            verificationService.limpiarPreRegistro(email);
            return ResponseEntity.ok(new ApiResponse<>(null, "Pre-registro eliminado", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("[VerificationController] Error eliminando pre-registro", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(null, "Error al eliminar pre-registro", false));
        }
    }
}
