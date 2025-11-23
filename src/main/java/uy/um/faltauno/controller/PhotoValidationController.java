package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uy.um.faltauno.dto.PhotoValidationResult;
import uy.um.faltauno.service.PhotoValidationService;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller para validación de fotos de perfil
 */
@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Configurar según necesidades
public class PhotoValidationController {

    private final PhotoValidationService photoValidationService;

    /**
     * Valida una foto de perfil
     * POST /api/photos/validate
     * 
     * Valida:
     * - Exactamente 1 rostro visible
     * - Contenido apropiado (no adulto, violento, etc.)
     * 
     * @param file Archivo de imagen a validar
     * @return Resultado de validación
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validatePhoto(
            @RequestParam("file") MultipartFile file) {
        
        try {
            log.info("[PhotoValidationController] Validating photo: {}", file.getOriginalFilename());

            // Validaciones básicas
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse(
                    "El archivo está vacío"
                ));
            }

            // Validar tamaño (máx 30MB)
            long maxSize = 30 * 1024 * 1024; // 30MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest().body(createErrorResponse(
                    "La imagen no puede superar 30MB"
                ));
            }

            // Validar tipo de archivo
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(createErrorResponse(
                    "El archivo debe ser una imagen"
                ));
            }

            // Validar con Google Cloud Vision API
            PhotoValidationResult result = photoValidationService.validatePhoto(file);

            // Crear respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isValid());
            response.put("valid", result.isValid());
            response.put("hasFace", result.isHasFace());
            response.put("faceCount", result.getFaceCount());
            response.put("isAppropriate", result.isAppropriate());
            response.put("confidence", result.getConfidence());
            response.put("message", result.getMessage());
            
            if (result.getReason() != null) {
                response.put("reason", result.getReason());
            }

            if (result.isValid()) {
                log.info("[PhotoValidationController] Photo validated successfully");
                return ResponseEntity.ok(response);
            } else {
                log.warn("[PhotoValidationController] Photo validation failed: {}", result.getMessage());
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            }

        } catch (Exception e) {
            log.error("[PhotoValidationController] Error validating photo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error al validar la foto: " + e.getMessage()));
        }
    }

    /**
     * Helper para crear respuesta de error
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("valid", false);
        response.put("message", message);
        return response;
    }
}
