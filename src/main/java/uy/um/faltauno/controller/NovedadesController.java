package uy.um.faltauno.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class NovedadesController {

    @GetMapping("/novedades")
    public ResponseEntity<List<Map<String, Object>>> getNovedades() {
        List<Map<String, Object>> novedades = new ArrayList<>();
        
        Map<String, Object> nov = new HashMap<>();
        nov.put("titulo", "Sistema actualizado");
        nov.put("descripcion", "Mejoras en el sistema");
        nov.put("fecha", LocalDateTime.now());
        novedades.add(nov);
        
        return ResponseEntity.ok(novedades);
    }
}
