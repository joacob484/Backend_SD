package uy.um.faltauno.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.entity.Cancha;
import uy.um.faltauno.repository.CanchaRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/canchas")
public class CanchaController {

    private final CanchaRepository canchaRepository;

    public CanchaController(CanchaRepository canchaRepository) {
        this.canchaRepository = canchaRepository;
    }

    // Listar todas
    @GetMapping
    public List<Cancha> getAllCanchas() {
        return canchaRepository.findAll();
    }

    // Buscar por id
    @GetMapping("/{id}")
    public ResponseEntity<Cancha> getCancha(@PathVariable Long id) {
        return canchaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear nueva cancha
    @PostMapping
    public Cancha createCancha(@RequestBody Cancha cancha) {
        return canchaRepository.save(cancha);
    }

    // Borrar cancha
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCancha(@PathVariable Long id) {
        return canchaRepository.findById(id).map(cancha -> {
            canchaRepository.delete(cancha);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
