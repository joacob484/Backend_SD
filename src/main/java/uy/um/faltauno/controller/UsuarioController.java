package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import uy.um.faltauno.config.CustomUserDetailsService;
import uy.um.faltauno.config.JwtUtil;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.dto.PerfilDTO;
import uy.um.faltauno.dto.UsuarioDTO;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Slf4j
public class UsuarioController {
    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    // Registro con auto-login
    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApiResponse<UsuarioDTO>> createUsuario(@RequestBody UsuarioDTO dto,
                                                                 HttpServletRequest request,
                                                                 HttpServletResponse response) {
        try {
            UsuarioDTO u = usuarioService.createUsuario(dto);

            // intentar auto-login (silencioso)
            try {
                UsernamePasswordAuthenticationToken token =
                        new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword());
                Authentication auth = authenticationManager.authenticate(token);

                SecurityContextHolder.getContext().setAuthentication(auth);
                request.getSession(true);
                log.info("Usuario {} autenticado automáticamente tras registro", dto.getEmail());
            } catch (Exception authEx) {
                log.warn("Auto-login falló para {}: {}", dto.getEmail(), authEx.getMessage());
            }

            u.setPassword(null);
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

    // Obtener el usuario actual autenticado
    @GetMapping(path = "/me", produces = "application/json")
    public ResponseEntity<ApiResponse<UsuarioDTO>> getMe() {
        try {
            UUID currentUserId = resolveCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>(null, "No autenticado", false));
            }
            UsuarioDTO dto = usuarioService.getUsuario(currentUserId);
            return ResponseEntity.ok(new ApiResponse<>(dto, "Usuario actual", true));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    // actualizar perfil: usa X-USER-ID si está, si no usa el usuario autenticado
    @PutMapping(path = "/me", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> actualizarPerfil(@RequestBody PerfilDTO perfilDTO,
                                              @RequestHeader(value = "X-USER-ID", required = false) String usuarioIdHeader) {
        try {
            UUID usuarioId = null;
            if (usuarioIdHeader != null && !usuarioIdHeader.isBlank()) {
                try {
                    usuarioId = UUID.fromString(usuarioIdHeader);
                } catch (IllegalArgumentException iae) {
                    return ResponseEntity.badRequest().body(new ApiResponse<>(null, "X-USER-ID invalid UUID format", false));
                }
            } else {
                usuarioId = resolveCurrentUserId();
            }

            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>(null, "No autenticado", false));
            }

            Usuario updated = usuarioService.actualizarPerfil(usuarioId, perfilDTO);
            UsuarioDTO dto = usuarioService.getUsuario(usuarioId);
            return ResponseEntity.ok(new ApiResponse<>(dto, "Perfil actualizado", true));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json")
    public ResponseEntity<?> subirFotoPorId(@PathVariable("id") UUID id,
                                           @RequestParam("file") MultipartFile file,
                                           @RequestHeader(value = "X-USER-ID", required = false) String headerUserId) {
        try {
            UUID effectiveUserId = null;
            if (headerUserId != null && !headerUserId.isBlank()) {
                try {
                    effectiveUserId = UUID.fromString(headerUserId);
                } catch (IllegalArgumentException iae) {
                    return ResponseEntity.badRequest().body(new ApiResponse<>(null, "X-USER-ID invalid UUID format", false));
                }
            } else {
                effectiveUserId = resolveCurrentUserId();
            }

            if (effectiveUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>(null, "No autenticado", false));
            }

            if (!effectiveUserId.equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse<>(null, "User ID mismatch", false));
            }

            usuarioService.subirFoto(id, file);
            return ResponseEntity.ok(new ApiResponse<>(null, "Foto subida", true));
        } catch (IOException ioe) {
            log.error("Error guardando foto para id {}: {}", id, ioe.getMessage(), ioe);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(null, "Error guardando foto", false));
        } catch (RuntimeException re) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(null, re.getMessage(), false));
        }
    }

    @PostMapping("/me/foto")
    public ResponseEntity<?> subirFotoMe(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token faltante");
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido");
        }

        String userId = jwtUtil.extractUserId(token);
        try {
            usuarioService.subirFoto(UUID.fromString(userId), file);
            return ResponseEntity.ok("Foto subida correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al subir la foto");
        }
    }

    // endpoint público para verificar cédula (sin asociar a usuario)
    @PostMapping("/verificar-cedula")
    public ResponseEntity<?> verificarCedula(@RequestBody Map<String, String> body) {
        String cedula = body.get("cedula");
        if (cedula == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "cédula requerida"));
        }

        UUID userId = resolveCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "No autenticado"));
        }

        boolean valida = usuarioService.verificarCedula(cedula);
        if (!valida) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cédula inválida"));
        }

        // Persistir la cédula para el usuario autenticado y devolver DTO actualizado
        UsuarioDTO updated = usuarioService.saveCedulaForUser(userId, cedula);
        return ResponseEntity.ok(new ApiResponse<>(updated, "Cédula verificada y guardada", true));
    }

    // endpoint para que el usuario autenticado verifique su propia cédula y se la guarde
    @PostMapping(path = "/me/verify-cedula", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> verificarCedulaMe(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-USER-ID", required = false) String usuarioIdHeader,
            HttpServletRequest request // necesitamos el request para la sesión
    ) {
        String cedula = body.get("cedula");
        if (cedula == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Cédula requerida"
            ));
        }

        UUID usuarioId = null;
        if (usuarioIdHeader != null && !usuarioIdHeader.isBlank()) {
            try {
                usuarioId = UUID.fromString(usuarioIdHeader);
            } catch (IllegalArgumentException iae) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "X-USER-ID invalid UUID format"
                ));
            }
        } else {
            usuarioId = resolveCurrentUserId();
        }

        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "No autenticado"
            ));
        }

        boolean valida = usuarioService.verificarCedula(cedula);
        if (!valida) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "data", Map.of("verified", false),
                    "message", "Cédula inválida"
            ));
        }

        // Guardar cédula en usuario
        Usuario updated = usuarioService.marcarCedula(usuarioId, cedula);

        // --- Auto-login seguro: crear Authentication desde UserDetails (NO reintentar autenticar con password hasheada)
        try {
            // Creamos un UserPrincipal compatible con tu CustomUserDetailsService
            CustomUserDetailsService.UserPrincipal userPrincipal =
                    new CustomUserDetailsService.UserPrincipal(
                            updated.getId(),
                            updated.getEmail(),
                            updated.getPassword(),
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );

            // Credenciales a null porque no volvemos a chequear contraseña
            Authentication auth = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Aseguramos que exista sesión HTTP para que se emita la cookie de sesión al cliente
            request.getSession(true);

            log.info("Usuario {} autenticado automáticamente tras verificar cédula", updated.getEmail());
        } catch (Exception e) {
            log.warn("Auto-login tras verificar cédula falló: {}", e.getMessage());
        }

        UsuarioDTO dto = usuarioService.getUsuario(usuarioId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cédula verificada y guardada",
                "data", Map.of("verified", true, "user", dto)
        ));
    }


    @GetMapping(path = "/{id}/foto", produces = { MediaType.IMAGE_JPEG_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE })
    public ResponseEntity<byte[]> getFoto(@PathVariable("id") UUID id) {
        Usuario u = usuarioService.findUsuarioEntityById(id);
        if (u == null || u.getFotoPerfil() == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] data = u.getFotoPerfil();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
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

    // helper para resolver ID del principal autenticado (por email)
    private UUID resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();

        try {
            // Si usas el UserPrincipal que expone getId()
            if (principal instanceof uy.um.faltauno.config.CustomUserDetailsService.UserPrincipal) {
                return ((uy.um.faltauno.config.CustomUserDetailsService.UserPrincipal) principal).getId();
            }
            // Si es UserDetails estándar: usamos username (email) para buscar el usuario
            if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                Usuario u = usuarioService.findByEmail(username);
                if (u != null) return u.getId();
            }
            // Si principal es String (username)
            if (principal instanceof String) {
                String username = (String) principal;
                Usuario u = usuarioService.findByEmail(username);
                if (u != null) return u.getId();
            }
        } catch (Exception e) {
            log.warn("No se pudo resolver usuario actual desde principal: {}", e.getMessage());
        }
        return null;
    }
}