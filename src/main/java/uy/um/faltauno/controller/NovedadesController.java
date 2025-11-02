package uy.um.faltauno.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class NovedadesController {

    @GetMapping("/novedades")
    public ResponseEntity<Map<String, Object>> getNovedades() {
        List<Map<String, Object>> novedades = new ArrayList<>();
        
        // Novedad 1
        Map<String, Object> nov1 = new HashMap<>();
        nov1.put("id", "1");
        nov1.put("type", "feature");
        nov1.put("title", "¡Sistema actualizado!");
        nov1.put("description", "Hemos implementado mejoras significativas en el rendimiento y la experiencia de usuario.");
        nov1.put("date", "Hace 2 horas");
        nov1.put("author", "Equipo Falta Uno");
        nov1.put("tags", Arrays.asList("Actualización", "Mejoras"));
        novedades.add(nov1);
        
        // Novedad 2
        Map<String, Object> nov2 = new HashMap<>();
        nov2.put("id", "2");
        nov2.put("type", "update");
        nov2.put("title", "Nuevo sistema de novedades");
        nov2.put("description", "Ahora puedes mantenerte al día con todas las actualizaciones del sistema en tiempo real.");
        nov2.put("date", "Hoy");
        nov2.put("author", "Equipo de desarrollo");
        nov2.put("tags", Arrays.asList("Novedades", "Comunicación"));
        novedades.add(nov2);
        
        // Respuesta en el formato esperado por el frontend
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", novedades);
        
        return ResponseEntity.ok(response);
    }
}
