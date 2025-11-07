package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.service.PasswordResetService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Controller para recuperación de contraseña
 */
@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * POST /api/auth/password/forgot
     * Solicitar recuperación de contraseña
     */
    @PostMapping("/forgot")
    public ResponseEntity<ApiResponse<Object>> solicitarRecuperacion(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        try {
            log.info("[PasswordReset] Solicitud de recuperación para: {}", request.email());
            
            String resetLink = passwordResetService.solicitarRecuperacion(request.email());
            
            // ⚡ NUEVO: Si resetLink no es null, estamos en modo desarrollo (sin email configurado)
            // Devolver el link directamente en la respuesta
            if (resetLink != null) {
                log.warn("[PasswordReset] ⚠️ Modo desarrollo - Devolviendo link en respuesta");
                return ResponseEntity.ok(new ApiResponse<>(
                        Map.of("resetLink", resetLink),
                        "Link de recuperación generado (solo modo desarrollo)",
                        true
                ));
            }
            
            // Modo producción - Email enviado, no devolver link
            return ResponseEntity.ok(new ApiResponse<>(
                    null,
                    "Si el email existe, recibirás instrucciones para restablecer tu contraseña",
                    true
            ));
            
        } catch (IllegalStateException e) {
            // Error de rate limiting
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    null,
                    e.getMessage(),
                    false
            ));
        } catch (Exception e) {
            log.error("[PasswordReset] Error en solicitud: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new ApiResponse<>(
                    null,
                    "Error procesando la solicitud",
                    false
            ));
        }
    }

    /**
     * GET /api/auth/password/validate-token
     * Validar si un token es válido
     */
    @GetMapping("/validate-token")
    public ResponseEntity<ApiResponse<Boolean>> validarToken(
            @RequestParam("token") String token
    ) {
        try {
            boolean valido = passwordResetService.validarToken(token);
            
            return ResponseEntity.ok(new ApiResponse<>(
                    valido,
                    valido ? "Token válido" : "Token inválido o expirado",
                    true
            ));
            
        } catch (Exception e) {
            log.error("[PasswordReset] Error validando token: {}", e.getMessage(), e);
            return ResponseEntity.ok(new ApiResponse<>(
                    false,
                    "Token inválido",
                    true
            ));
        }
    }

    /**
     * POST /api/auth/password/reset
     * Restablecer contraseña con token
     */
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Void>> restablecerPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        try {
            log.info("[PasswordReset] Intentando restablecer contraseña con token");
            
            passwordResetService.restablecerPassword(request.token(), request.newPassword());
            
            return ResponseEntity.ok(new ApiResponse<>(
                    null,
                    "Contraseña restablecida exitosamente",
                    true
            ));
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    null,
                    e.getMessage(),
                    false
            ));
        } catch (Exception e) {
            log.error("[PasswordReset] Error restableciendo contraseña: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new ApiResponse<>(
                    null,
                    "Error restableciendo la contraseña",
                    false
            ));
        }
    }

    /**
     * DTOs
     */
    public record ForgotPasswordRequest(
            @NotBlank(message = "El email es obligatorio")
            @Email(message = "Formato de email inválido")
            String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "El token es obligatorio")
            String token,
            
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
            String newPassword
    ) {}
}
