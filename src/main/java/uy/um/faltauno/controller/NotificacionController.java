package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.dto.NotificacionDTO;
import uy.um.faltauno.service.NotificacionService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/notificaciones")
@CrossOrigin(origins = "${FRONTEND_URL:https://faltauno-frontend-169771742214.us-central1.run.app}")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    /**
     * Obtener todas las notificaciones del usuario autenticado
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificacionDTO>>> obtenerNotificaciones(Authentication auth) {
        try {
            log.debug("[NotificacionController] GET /api/notificaciones");
            List<NotificacionDTO> notificaciones = notificacionService.obtenerNotificaciones(auth);
            return ResponseEntity.ok(new ApiResponse<>(notificaciones, "Notificaciones obtenidas", true));
        } catch (Exception e) {
            log.error("[NotificacionController] Error obteniendo notificaciones", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    /**
     * Obtener notificaciones no leídas
     */
    @GetMapping("/no-leidas")
    public ResponseEntity<ApiResponse<List<NotificacionDTO>>> obtenerNoLeidas(Authentication auth) {
        try {
            log.debug("[NotificacionController] GET /api/notificaciones/no-leidas");
            List<NotificacionDTO> notificaciones = notificacionService.obtenerNoLeidas(auth);
            return ResponseEntity.ok(new ApiResponse<>(notificaciones, "Notificaciones no leídas", true));
        } catch (Exception e) {
            log.error("[NotificacionController] Error obteniendo notificaciones no leídas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    /**
     * Contar notificaciones no leídas
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> contarNoLeidas(Authentication auth) {
        try {
            log.debug("[NotificacionController] GET /api/notificaciones/count");
            long count = notificacionService.contarNoLeidas(auth);
            return ResponseEntity.ok(new ApiResponse<>(
                    Map.of("count", count), 
                    "Cantidad de notificaciones no leídas", 
                    true
            ));
        } catch (Exception e) {
            log.error("[NotificacionController] Error contando notificaciones", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    /**
     * Marcar notificación como leída
     */
    @PutMapping("/{id}/leer")
    public ResponseEntity<ApiResponse<Void>> marcarComoLeida(
            @PathVariable("id") UUID id,
            Authentication auth) {
        try {
            log.debug("[NotificacionController] PUT /api/notificaciones/{}/leer", id);
            notificacionService.marcarComoLeida(id, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Notificación marcada como leída", true));
        } catch (IllegalArgumentException e) {
            log.warn("[NotificacionController] Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("[NotificacionController] Error marcando notificación como leída", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    /**
     * Marcar todas las notificaciones como leídas
     */
    @PutMapping("/leer-todas")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> marcarTodasComoLeidas(Authentication auth) {
        try {
            log.debug("[NotificacionController] PUT /api/notificaciones/leer-todas");
            int count = notificacionService.marcarTodasComoLeidas(auth);
            return ResponseEntity.ok(new ApiResponse<>(
                    Map.of("count", count),
                    count + " notificaciones marcadas como leídas",
                    true
            ));
        } catch (Exception e) {
            log.error("[NotificacionController] Error marcando todas como leídas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    /**
     * Eliminar una notificación
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarNotificacion(
            @PathVariable("id") UUID id,
            Authentication auth) {
        try {
            log.debug("[NotificacionController] DELETE /api/notificaciones/{}", id);
            notificacionService.eliminarNotificacion(id, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Notificación eliminada", true));
        } catch (IllegalArgumentException e) {
            log.warn("[NotificacionController] Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("[NotificacionController] Error eliminando notificación", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }
}
