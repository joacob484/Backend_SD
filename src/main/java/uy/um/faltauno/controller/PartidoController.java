package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.PartidoDto;
import uy.um.faltauno.service.PartidoService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/partidos")
@RequiredArgsConstructor
public class PartidoController {

    private final PartidoService partidoService;

    @GetMapping
    public ResponseEntity<List<PartidoDto>> listar() {
        return ResponseEntity.ok(partidoService.listarPartidos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartidoDto> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(partidoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<PartidoDto> crear(@RequestBody PartidoDto dto) {
        return ResponseEntity.ok(partidoService.crearPartido(dto));
    }
}
