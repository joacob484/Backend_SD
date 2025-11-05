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

            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Email requerido", false));
            }

            if (codigo == null || codigo.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Código requerido", false));
            }

            PendingRegistration preRegistro = verificationService.verificarCodigo(email, codigo.trim());

            return ResponseEntity.ok(new ApiResponse<>(
                    Map.of(
                        "verified", true,
                        "email", preRegistro.getEmail(),
                        "passwordHash", preRegistro.getPasswordHash()
                    ),
                    "Email verificado exitosamente",
                    true
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("[VerificationController] Error de validación", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(
                            Map.of("verified", false),
                            e.getMessage(),
                            false
                    ));
                    
        } catch (Exception e) {
            log.error("[VerificationController] Error verificando código", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al verificar el código", false));
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
}
