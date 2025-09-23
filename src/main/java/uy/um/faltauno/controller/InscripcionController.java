package uy.um.faltauno.controller;

import uy.um.faltauno.dto.InscripcionDTO;
import uy.um.faltauno.service.InscripcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inscripciones")
@RequiredArgsConstructor
public class InscripcionController {

    private final InscripcionService inscripcionService;

    @PostMapping
    public ResponseEntity<InscripcionDTO> crear(@RequestParam UUID partidoId, @RequestParam UUID usuarioId) {
        return ResponseEntity.ok(inscripcionService.crearInscripcion(partidoId, usuarioId));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<InscripcionDTO>> listarPorUsuario(@PathVariable UUID usuarioId) {
        return ResponseEntity.ok(inscripcionService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/partido/{partidoId}")
    public ResponseEntity<List<InscripcionDTO>> listarPorPartido(@PathVariable UUID partidoId) {
        return ResponseEntity.ok(inscripcionService.listarPorPartido(partidoId));
    }
}
