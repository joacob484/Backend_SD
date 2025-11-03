package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.dto.InscripcionDTO;
import uy.um.faltauno.dto.SolicitudDTO;
import uy.um.faltauno.service.InscripcionService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inscripciones")
@CrossOrigin(origins = "${FRONTEND_URL:http://localhost:3000}")
@RequiredArgsConstructor
@Slf4j
public class InscripcionController {

    private final InscripcionService inscripcionService;

    @PostMapping
    public ResponseEntity<ApiResponse<InscripcionDTO>> crear(
            @Valid @RequestBody SolicitudDTO dto,
            Authentication auth) {
        try {
            log.info("[InscripcionController] POST /api/inscripciones - partidoId={}, usuarioId={}", 
                    dto.getPartidoId(), dto.getUsuarioId());
            
            InscripcionDTO inscripcion = inscripcionService.crearInscripcion(
                    dto.getPartidoId(), 
                    dto.getUsuarioId(),
                    auth);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(inscripcion, "Solicitud enviada correctamente", true));
                    
        } catch (IllegalStateException e) {
            log.warn("[InscripcionController] IllegalStateException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (IllegalArgumentException e) {
            log.warn("[InscripcionController] IllegalArgumentException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (SecurityException e) {
            log.warn("[InscripcionController] SecurityException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[InscripcionController] Error inesperado creando inscripción", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al crear inscripción", false));
        }
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<ApiResponse<List<InscripcionDTO>>> listarPorUsuario(
            @PathVariable UUID usuarioId,
            @RequestParam(required = false) String estado) {
        try {
            log.debug("[InscripcionController] GET /api/inscripciones/usuario/{} - estado={}", 
                    usuarioId, estado);
            
            List<InscripcionDTO> inscripciones = inscripcionService.listarPorUsuario(usuarioId, estado);
            return ResponseEntity.ok(new ApiResponse<>(inscripciones, "Inscripciones del usuario", true));
            
        } catch (Exception e) {
            log.error("[InscripcionController] Error listando inscripciones del usuario {}", usuarioId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al listar inscripciones", false));
        }
    }

    @GetMapping("/partido/{partidoId}")
    public ResponseEntity<ApiResponse<List<InscripcionDTO>>> listarPorPartido(
            @PathVariable UUID partidoId,
            @RequestParam(required = false) String estado) {
        try {
            log.debug("[InscripcionController] GET /api/inscripciones/partido/{} - estado={}", 
                    partidoId, estado);
            
            List<InscripcionDTO> inscripciones = inscripcionService.listarPorPartido(partidoId, estado);
            return ResponseEntity.ok(new ApiResponse<>(inscripciones, "Inscripciones del partido", true));
            
        } catch (Exception e) {
            log.error("[InscripcionController] Error listando inscripciones del partido {}", partidoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al listar inscripciones", false));
        }
    }

    @PostMapping("/{inscripcionId}/aceptar")
    public ResponseEntity<ApiResponse<InscripcionDTO>> aceptar(
            @PathVariable UUID inscripcionId,
            Authentication auth) {
        try {
            log.info("[InscripcionController] POST /api/inscripciones/{}/aceptar", inscripcionId);
            
            InscripcionDTO aceptada = inscripcionService.aceptarInscripcion(inscripcionId, auth);
            return ResponseEntity.ok(new ApiResponse<>(aceptada, "Solicitud aceptada", true));
            
        } catch (IllegalArgumentException e) {
            log.warn("[InscripcionController] Recurso no encontrado al aceptar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (SecurityException e) {
            log.warn("[InscripcionController] Acceso denegado al aceptar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (IllegalStateException e) {
            log.warn("[InscripcionController] Error de estado al aceptar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[InscripcionController] Error inesperado aceptando inscripción", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al aceptar inscripción", false));
        }
    }

    @PostMapping("/{inscripcionId}/rechazar")
    public ResponseEntity<ApiResponse<Void>> rechazar(
            @PathVariable UUID inscripcionId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        try {
            String motivo = body != null ? body.get("motivo") : null;
            log.info("[InscripcionController] POST /api/inscripciones/{}/rechazar - motivo={}", 
                    inscripcionId, motivo);
            
            inscripcionService.rechazarInscripcion(inscripcionId, motivo, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Solicitud rechazada", true));
            
        } catch (IllegalArgumentException e) {
            log.warn("[InscripcionController] Recurso no encontrado al rechazar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (SecurityException e) {
            log.warn("[InscripcionController] Acceso denegado al rechazar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (IllegalStateException e) {
            log.warn("[InscripcionController] Error de estado al rechazar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[InscripcionController] Error inesperado rechazando inscripción", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al rechazar inscripción", false));
        }
    }

    @DeleteMapping("/{inscripcionId}")
    public ResponseEntity<ApiResponse<Void>> cancelar(
            @PathVariable UUID inscripcionId,
            Authentication auth) {
        try {
            log.info("[InscripcionController] DELETE /api/inscripciones/{}", inscripcionId);
            
            inscripcionService.cancelarInscripcion(inscripcionId, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Te has retirado del partido", true));
            
        } catch (IllegalArgumentException e) {
            log.warn("[InscripcionController] Recurso no encontrado al cancelar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (SecurityException e) {
            log.warn("[InscripcionController] Acceso denegado al cancelar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (IllegalStateException e) {
            log.warn("[InscripcionController] Error de estado al cancelar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[InscripcionController] Error inesperado cancelando inscripción", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al cancelar inscripción", false));
        }
    }

    @GetMapping("/estado")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerEstado(
            @RequestParam UUID partidoId,
            @RequestParam UUID usuarioId) {
        try {
            log.debug("[InscripcionController] GET /api/inscripciones/estado - partidoId={}, usuarioId={}", 
                    partidoId, usuarioId);
            
            Map<String, Object> estado = inscripcionService.obtenerEstadoInscripcion(partidoId, usuarioId);
            return ResponseEntity.ok(new ApiResponse<>(estado, "Estado de inscripción", true));
            
        } catch (Exception e) {
            log.error("[InscripcionController] Error obteniendo estado de inscripción", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener estado", false));
        }
    }
}