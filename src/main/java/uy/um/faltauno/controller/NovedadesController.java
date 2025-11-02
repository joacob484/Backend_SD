package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.service.NovedadesService;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NovedadesController {

    private final NovedadesService novedadesService;

    @GetMapping("/novedades")
    @Cacheable(value = "novedades-github", unless = "#result == null")
    public ResponseEntity<Map<String, Object>> getNovedades(@RequestParam(defaultValue = "5") int limit) {
        // Obtener commits reales de GitHub con deploys exitosos
        List<Map<String, Object>> novedades = novedadesService.getUltimosCommits(limit);
        
        // Respuesta en el formato esperado por el frontend
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", novedades);
        
        return ResponseEntity.ok(response);
    }
}
