package uy.um.faltauno.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.PartidoDTO;
import uy.um.faltauno.dto.PartidoDTO;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.service.PartidoService;

import java.util.UUID;

@RestController
@RequestMapping("/api/partidos")
@CrossOrigin(origins = "${FRONTEND_URL:http://localhost:3000}")
public class PartidoController {

    private final PartidoService service;

    public PartidoController(PartidoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Partido crear(@RequestBody PartidoDTO req) {
        return service.crear(req);
    }

    @GetMapping("/{id}")
    public Partido get(@PathVariable("id") UUID id) {
        return service.obtenerPorId(id);
    }
}
