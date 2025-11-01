package uy.um.faltauno.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.service.NovedadesService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/novedades")
@CrossOrigin(origins = "*")
public class NovedadesController {

    private final NovedadesService novedadesService;

    public NovedadesController(NovedadesService novedadesService) {
        this.novedadesService = novedadesService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getNovedades(
            @RequestParam(defaultValue = "5") int limit
    ) {
        try {
            List<Map<String, Object>> novedades = novedadesService.getUltimosCommits(limit);
            return ResponseEntity.ok(ApiResponse.success(novedades));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Error obteniendo novedades: " + e.getMessage()));
        }
    }
}
