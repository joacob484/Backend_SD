package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.dto.MensajeDTO;
import uy.um.faltauno.service.MensajeService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/partidos/{partidoId}/mensajes")
@CrossOrigin(origins = "${FRONTEND_URL:http://localhost:3000}")
@RequiredArgsConstructor
@Slf4j
public class MensajeController {

    private final MensajeService mensajeService;

    /**
     * Obtener mensajes del chat de un partido
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MensajeDTO>>> obtenerMensajes(
            @PathVariable UUID partidoId,
            @RequestParam(required = false, defaultValue = "50") int limit,
            Authentication auth) {
        try {
            List<MensajeDTO> mensajes = mensajeService.obtenerMensajesPartido(partidoId, limit, auth);
            return ResponseEntity.ok(new ApiResponse<>(mensajes, "Mensajes del partido", true));
        } catch (IllegalArgumentException e) {
            log.warn("[MensajeController] Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("Error obteniendo mensajes del partido {}", partidoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener mensajes", false));
        }
    }

    /**
     * Enviar un mensaje al chat del partido
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MensajeDTO>> enviarMensaje(
            @PathVariable UUID partidoId,
            @Valid @RequestBody MensajeDTO mensajeDTO,
            Authentication auth) {
        try {
            MensajeDTO mensaje = mensajeService.enviarMensaje(partidoId, mensajeDTO, auth);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(mensaje, "Mensaje enviado", true));
        } catch (IllegalArgumentException e) {
            // Distinguir 404 (no encontrado) vs 400 (validación)
            if (e.getMessage().contains("no encontrado") || e.getMessage().contains("no encontrada")) {
                log.warn("[MensajeController] Recurso no encontrado: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(null, e.getMessage(), false));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(null, e.getMessage(), false));
            }
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("Error enviando mensaje", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al enviar mensaje", false));
        }
    }

    /**
     * Marcar mensajes como leídos
     */
    @PutMapping("/leer")
    public ResponseEntity<ApiResponse<Void>> marcarComoLeidos(
            @PathVariable UUID partidoId,
            Authentication auth) {
        try {
            mensajeService.marcarMensajesComoLeidos(partidoId, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Mensajes marcados como leídos", true));
        } catch (Exception e) {
            log.error("Error marcando mensajes como leídos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al marcar mensajes", false));
        }
    }

    /**
     * Eliminar un mensaje (solo el autor o el organizador)
     */
    @DeleteMapping("/{mensajeId}")
    public ResponseEntity<ApiResponse<Void>> eliminarMensaje(
            @PathVariable UUID partidoId,
            @PathVariable UUID mensajeId,
            Authentication auth) {
        try {
            mensajeService.eliminarMensaje(mensajeId, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Mensaje eliminado", true));
        } catch (IllegalArgumentException e) {
            log.warn("[MensajeController] Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("Error eliminando mensaje", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al eliminar mensaje", false));
        }
    }
}