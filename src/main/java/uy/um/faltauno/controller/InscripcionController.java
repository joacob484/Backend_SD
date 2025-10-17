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

    /**
     * Crear solicitud de inscripción (el usuario se postula al partido)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<InscripcionDTO>> crear(
            @Valid @RequestBody SolicitudDTO dto,
            Authentication auth) {
        try {
            log.info("Creando inscripción: partidoId={}, usuarioId={}", 
                    dto.getPartidoId(), dto.getUsuarioId());
            
            InscripcionDTO inscripcion = inscripcionService.crearInscripcion(
                    dto.getPartidoId(), 
                    dto.getUsuarioId(),
                    auth);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(inscripcion, "Solicitud enviada correctamente", true));
        } catch (IllegalStateException e) {
            log.warn("Error de estado creando inscripción: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("Error creando inscripción", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al crear inscripción", false));
        }
    }

    /**
     * Listar inscripciones de un usuario
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<ApiResponse<List<InscripcionDTO>>> listarPorUsuario(
            @PathVariable UUID usuarioId,
            @RequestParam(required = false) String estado) {
        try {
            List<InscripcionDTO> inscripciones = inscripcionService.listarPorUsuario(usuarioId, estado);
            return ResponseEntity.ok(new ApiResponse<>(inscripciones, "Inscripciones del usuario", true));
        } catch (Exception e) {
            log.error("Error listando inscripciones del usuario {}", usuarioId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al listar inscripciones", false));
        }
    }

    /**
     * Listar inscripciones de un partido
     */
    @GetMapping("/partido/{partidoId}")
    public ResponseEntity<ApiResponse<List<InscripcionDTO>>> listarPorPartido(
            @PathVariable UUID partidoId,
            @RequestParam(required = false) String estado) {
        try {
            List<InscripcionDTO> inscripciones = inscripcionService.listarPorPartido(partidoId, estado);
            return ResponseEntity.ok(new ApiResponse<>(inscripciones, "Inscripciones del partido", true));
        } catch (Exception e) {
            log.error("Error listando inscripciones del partido {}", partidoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al listar inscripciones", false));
        }
    }

    /**
     * Obtener solicitudes pendientes de un partido (solo organizador)
     */
    @GetMapping("/partido/{partidoId}/pendientes")
    public ResponseEntity<ApiResponse<List<InscripcionDTO>>> obtenerSolicitudesPendientes(
            @PathVariable UUID partidoId,
            Authentication auth) {
        try {
            List<InscripcionDTO> solicitudes = inscripcionService.obtenerSolicitudesPendientes(partidoId, auth);
            return ResponseEntity.ok(new ApiResponse<>(solicitudes, "Solicitudes pendientes", true));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("Error obteniendo solicitudes pendientes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener solicitudes", false));
        }
    }

    /**
     * Aceptar una solicitud de inscripción (solo organizador)
     */
    @PostMapping("/{inscripcionId}/aceptar")
    public ResponseEntity<ApiResponse<InscripcionDTO>> aceptar(
            @PathVariable UUID inscripcionId,
            Authentication auth) {
        try {
            log.info("Aceptando inscripción {}", inscripcionId);
            InscripcionDTO aceptada = inscripcionService.aceptarInscripcion(inscripcionId, auth);
            return ResponseEntity.ok(new ApiResponse<>(aceptada, "Solicitud aceptada", true));
        } catch (SecurityException e) {
            log.warn("Acceso denegado al aceptar inscripción: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (IllegalStateException e) {
            log.warn("Error de estado al aceptar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (RuntimeException e) {
            log.error("Error aceptando inscripción", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    /**
     * Rechazar una solicitud de inscripción (solo organizador)
     */
    @PostMapping("/{inscripcionId}/rechazar")
    public ResponseEntity<ApiResponse<Void>> rechazar(
            @PathVariable UUID inscripcionId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        try {
            String motivo = body != null ? body.get("motivo") : null;
            log.info("Rechazando inscripción {}", inscripcionId);
            inscripcionService.rechazarInscripcion(inscripcionId, motivo, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Solicitud rechazada", true));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    /**
     * Cancelar inscripción (el usuario se retira del partido)
     */
    @DeleteMapping("/{inscripcionId}")
    public ResponseEntity<ApiResponse<Void>> cancelar(
            @PathVariable UUID inscripcionId,
            Authentication auth) {
        try {
            log.info("Cancelando inscripción {}", inscripcionId);
            inscripcionService.cancelarInscripcion(inscripcionId, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Te has retirado del partido", true));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    /**
     * Obtener estado de inscripción de un usuario en un partido
     */
    @GetMapping("/estado")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerEstado(
            @RequestParam UUID partidoId,
            @RequestParam UUID usuarioId) {
        try {
            Map<String, Object> estado = inscripcionService.obtenerEstadoInscripcion(partidoId, usuarioId);
            return ResponseEntity.ok(new ApiResponse<>(estado, "Estado de inscripción", true));
        } catch (Exception e) {
            log.error("Error obteniendo estado de inscripción", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener estado", false));
        }
    }
}