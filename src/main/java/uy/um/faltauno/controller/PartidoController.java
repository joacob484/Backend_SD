package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.PartidoDTO;
import uy.um.faltauno.service.PartidoService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/partidos")
@RequiredArgsConstructor
public class PartidoController {

    private final PartidoService partidoService;

    @GetMapping
    public ResponseEntity<List<PartidoDTO>> listar() {
        return ResponseEntity.ok(partidoService.listarPartidos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartidoDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(partidoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<PartidoDTO> crear(@RequestBody PartidoDTO dto) {
        return ResponseEntity.ok(partidoService.crearPartido(dto));
    }
}
