package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.dto.ReviewDTO;
import uy.um.faltauno.service.ReviewService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "${FRONTEND_URL:https://faltauno.vercel.app}")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Crear una review para un jugador después de un partido
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewDTO>> crear(
            @Valid @RequestBody ReviewDTO reviewDTO,
            Authentication auth) {
        try {
            ReviewDTO review = reviewService.crearReview(reviewDTO, auth);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(review, "Review creada exitosamente", true));
        } catch (IllegalArgumentException e) {
            log.warn("[ReviewController] Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("Error creando review", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al crear review", false));
        }
    }

    /**
     * Obtener reviews recibidas por un usuario
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<ApiResponse<List<ReviewDTO>>> obtenerPorUsuario(
            @PathVariable UUID usuarioId) {
        try {
            List<ReviewDTO> reviews = reviewService.obtenerReviewsDeUsuario(usuarioId);
            return ResponseEntity.ok(new ApiResponse<>(reviews, "Reviews del usuario", true));
        } catch (Exception e) {
            log.error("Error obteniendo reviews", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener reviews", false));
        }
    }

    /**
     * Obtener reviews de un partido
     */
    @GetMapping("/partido/{partidoId}")
    public ResponseEntity<ApiResponse<List<ReviewDTO>>> obtenerPorPartido(
            @PathVariable UUID partidoId) {
        try {
            List<ReviewDTO> reviews = reviewService.obtenerReviewsDePartido(partidoId);
            return ResponseEntity.ok(new ApiResponse<>(reviews, "Reviews del partido", true));
        } catch (Exception e) {
            log.error("Error obteniendo reviews del partido", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener reviews", false));
        }
    }

    /**
     * Obtener estadísticas de un usuario (promedios)
     */
    @GetMapping("/usuario/{usuarioId}/estadisticas")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerEstadisticas(
            @PathVariable UUID usuarioId) {
        try {
            Map<String, Object> estadisticas = reviewService.obtenerEstadisticas(usuarioId);
            return ResponseEntity.ok(new ApiResponse<>(estadisticas, "Estadísticas del usuario", true));
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener estadísticas", false));
        }
    }

    /**
     * Obtener reviews pendientes de un usuario (jugadores que debe calificar)
     */
    @GetMapping("/pendientes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerPendientes(
            Authentication auth) {
        try {
            List<Map<String, Object>> pendientes = reviewService.obtenerReviewsPendientes(auth);
            return ResponseEntity.ok(new ApiResponse<>(pendientes, "Reviews pendientes", true));
        } catch (Exception e) {
            log.error("Error obteniendo reviews pendientes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener reviews pendientes", false));
        }
    }

    /**
     * Verificar si un usuario ya calificó a otro en un partido
     */
    @GetMapping("/verificar")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verificarReview(
            @RequestParam UUID partidoId,
            @RequestParam UUID usuarioCalificadoId,
            Authentication auth) {
        try {
            boolean existe = reviewService.verificarReviewExistente(partidoId, usuarioCalificadoId, auth);
            return ResponseEntity.ok(new ApiResponse<>(
                Map.of("existe", existe),
                "Estado de review verificado",
                true
            ));
        } catch (Exception e) {
            log.error("Error verificando review", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al verificar review", false));
        }
    }

    /**
     * Eliminar una review (solo el autor)
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @PathVariable UUID reviewId,
            Authentication auth) {
        try {
            reviewService.eliminarReview(reviewId, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Review eliminada", true));
        } catch (IllegalArgumentException e) {
            log.warn("[ReviewController] Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("Error eliminando review", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al eliminar review", false));
        }
    }
}