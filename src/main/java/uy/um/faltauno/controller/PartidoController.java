package uy.um.faltauno.controller;

import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.service.PartidoService;
import uy.um.faltauno.entity.Partido;
import java.util.List;

@RestController
@RequestMapping("/api/partidos")
public class PartidoController {

    private final PartidoService service;

    public PartidoController(PartidoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Partido> buscar(@RequestParam String zona, @RequestParam String nivel) {
        return service.buscarPartidos(zona, nivel);
    }

    @PostMapping
    public Partido crear(@RequestBody Partido p) {
        return service.crearPartido(p);
    }

    @PostMapping("/{id}/join")
    public void join(@PathVariable Long id) {
        service.joinPartido(id);
    }
}
