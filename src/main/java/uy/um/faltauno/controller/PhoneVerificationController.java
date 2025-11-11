package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.service.PhoneVerificationService;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/phone-verification")
@CrossOrigin(origins = "${FRONTEND_URL:https://faltauno-frontend-169771742214.us-central1.run.app}")
@RequiredArgsConstructor
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    /**
     * Enviar código de verificación al celular del usuario
     * POST /api/phone-verification/send
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> enviarCodigo(Authentication auth) {
        try {
            log.info("[PhoneVerificationController] POST /api/phone-verification/send");
            
            UUID usuarioId = UUID.fromString(auth.getName());
            phoneVerificationService.enviarCodigoVerificacion(usuarioId);
            
            return ResponseEntity.ok(new ApiResponse<>(
                null,
                "Código enviado exitosamente. Revisa tu celular.",
                true
            ));
            
        } catch (IllegalStateException e) {
            log.warn("[PhoneVerificationController] Error de estado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (IllegalArgumentException e) {
            log.warn("[PhoneVerificationController] Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[PhoneVerificationController] Error inesperado enviando código", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al enviar código de verificación", false));
        }
    }

    /**
     * Verificar código ingresado por el usuario
     * POST /api/phone-verification/verify
     * Body: { "code": "123456" }
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verificarCodigo(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            log.info("[PhoneVerificationController] POST /api/phone-verification/verify");
            
            String codigo = body.get("code");
            if (codigo == null || codigo.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(null, "Código requerido", false));
            }
            
            UUID usuarioId = UUID.fromString(auth.getName());
            boolean verificado = phoneVerificationService.verificarCodigo(usuarioId, codigo);
            
            return ResponseEntity.ok(new ApiResponse<>(
                Map.of("verified", verificado),
                "Celular verificado exitosamente",
                true
            ));
            
        } catch (IllegalStateException e) {
            log.warn("[PhoneVerificationController] Error de validación: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (IllegalArgumentException e) {
            log.warn("[PhoneVerificationController] Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[PhoneVerificationController] Error inesperado verificando código", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al verificar código", false));
        }
    }

    /**
     * Reenviar código de verificación
     * POST /api/phone-verification/resend
     */
    @PostMapping("/resend")
    public ResponseEntity<ApiResponse<Void>> reenviarCodigo(Authentication auth) {
        try {
            log.info("[PhoneVerificationController] POST /api/phone-verification/resend");
            
            UUID usuarioId = UUID.fromString(auth.getName());
            phoneVerificationService.reenviarCodigo(usuarioId);
            
            return ResponseEntity.ok(new ApiResponse<>(
                null,
                "Nuevo código enviado exitosamente",
                true
            ));
            
        } catch (IllegalStateException e) {
            log.warn("[PhoneVerificationController] Error de estado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (IllegalArgumentException e) {
            log.warn("[PhoneVerificationController] Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[PhoneVerificationController] Error inesperado reenviando código", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al reenviar código", false));
        }
    }

    /**
     * Obtener estado de verificación
     * GET /api/phone-verification/status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> obtenerEstado(Authentication auth) {
        try {
            log.debug("[PhoneVerificationController] GET /api/phone-verification/status");
            
            UUID usuarioId = UUID.fromString(auth.getName());
            boolean verificado = phoneVerificationService.esCelularVerificado(usuarioId);
            
            return ResponseEntity.ok(new ApiResponse<>(
                Map.of("verified", verificado),
                "Estado de verificación",
                true
            ));
            
        } catch (Exception e) {
            log.error("[PhoneVerificationController] Error obteniendo estado", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener estado", false));
        }
    }
}
