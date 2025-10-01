package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.dto.PerfilDTO;
import uy.um.faltauno.dto.UsuarioDTO;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.service.UsuarioService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApiResponse<UsuarioDTO>> createUsuario(@RequestBody UsuarioDTO dto) {
        try {
            UsuarioDTO u = usuarioService.createUsuario(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(u, "Usuario creado", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

     @PutMapping(path = "/me", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> actualizarPerfil(@RequestBody PerfilDTO perfilDTO,
                                              @RequestHeader("X-USER-ID") UUID usuarioId) {
        try {
            Usuario updated = usuarioService.actualizarPerfil(usuarioId, perfilDTO);
            return ResponseEntity.ok(new ApiResponse<>(updated, "Perfil actualizado", true));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json")
    public ResponseEntity<?> subirFotoPorId(@PathVariable("id") UUID id, @RequestParam("file") MultipartFile file,
                                           @RequestHeader(value = "X-USER-ID", required = false) UUID headerUserId) {
        // Si el front envía header X-USER-ID el controller lo usa; si no, comparamos path id con header
        try {
            // validar que el id/path coincida con header si header presente
            if (headerUserId != null && !headerUserId.equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse<>(null, "User ID mismatch", false));
            }
            usuarioService.subirFoto(id, file);
            return ResponseEntity.ok(new ApiResponse<>(null, "Foto subida", true));
        } catch (IOException ioe) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(null, "Error guardando foto", false));
        } catch (RuntimeException re) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(null, re.getMessage(), false));
        }
    }

    // endpoint alternativo que el front puede usar: POST /api/usuarios/me/foto (usa X-USER-ID header)
    @PostMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json")
    public ResponseEntity<?> subirFotoMe(@RequestParam("file") MultipartFile file,
                                        @RequestHeader("X-USER-ID") UUID usuarioId) {
        try {
            usuarioService.subirFoto(usuarioId, file);
            return ResponseEntity.ok(new ApiResponse<>(null, "Foto subida", true));
        } catch (IOException ioe) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(null, "Error guardando foto", false));
        } catch (RuntimeException re) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(null, re.getMessage(), false));
        }
    }

    @GetMapping(path = "/{id}/foto", produces = { MediaType.IMAGE_JPEG_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE })
    public ResponseEntity<byte[]> getFoto(@PathVariable("id") UUID id) {
        Usuario u = usuarioService.findUsuarioEntityById(id); // implementá este helper en service
        if (u == null || u.getFotoPerfil() == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] data = u.getFotoPerfil();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG); // asumimos jpeg; podés almacenar mime en la DB si querés
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    @GetMapping(produces = "application/json")
    public ResponseEntity<ApiResponse<List<UsuarioDTO>>> getAllUsuarios() {
        List<UsuarioDTO> all = usuarioService.getAllUsuarios();
        return ResponseEntity.ok(new ApiResponse<>(all, "Lista de usuarios", true));
    }

    @GetMapping(path = "/{id}", produces = "application/json")
    public ResponseEntity<ApiResponse<UsuarioDTO>> getUsuario(@PathVariable UUID id) {
        try {
            UsuarioDTO dto = usuarioService.getUsuario(id);
            return ResponseEntity.ok(new ApiResponse<>(dto, "Usuario encontrado", true));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    @DeleteMapping(path = "/{id}", produces = "application/json")
    public ResponseEntity<ApiResponse<Void>> deleteUsuario(@PathVariable UUID id) {
        usuarioService.deleteUsuario(id);
        return ResponseEntity.ok(new ApiResponse<>(null, "Usuario eliminado", true));
    }
}