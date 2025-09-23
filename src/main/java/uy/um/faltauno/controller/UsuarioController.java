package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.UsuarioDto;
import uy.um.faltauno.service.UsuarioService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioDto> createUsuario(@RequestBody UsuarioDto dto) {
        return ResponseEntity.ok(usuarioService.createUsuario(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDto> getUsuario(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.getUsuario(id));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDto>> getAllUsuarios() {
        return ResponseEntity.ok(usuarioService.getAllUsuarios());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUsuario(@PathVariable UUID id) {
        usuarioService.deleteUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
