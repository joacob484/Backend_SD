package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.service.StatsService;

import java.util.Map;

/**
 * Controller para estadísticas de la comunidad y aplicación
 */
@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "${FRONTEND_URL:https://faltauno-frontend-169771742214.us-central1.run.app}")
@RequiredArgsConstructor
@Slf4j
public class StatsController {

    private final StatsService statsService;

    /**
     * Obtener estadísticas generales de la comunidad
     * GET /api/stats/community
     */
    @GetMapping("/community")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCommunityStats() {
        try {
            Map<String, Object> stats = statsService.obtenerEstadisticasComunidad();
            return ResponseEntity.ok(new ApiResponse<>(stats, "Estadísticas de la comunidad", true));

        } catch (Exception e) {
            log.error("Error obteniendo estadísticas de la comunidad", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener estadísticas", false));
        }
    }

    /**
     * Obtener estadísticas detalladas del sistema
     * GET /api/stats/system
     */
    @GetMapping("/system")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStats() {
        try {
            Map<String, Object> stats = statsService.obtenerEstadisticasSistema();
            return ResponseEntity.ok(new ApiResponse<>(stats, "Estadísticas del sistema", true));

        } catch (Exception e) {
            log.error("Error obteniendo estadísticas del sistema", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener estadísticas", false));
        }
    }
}
