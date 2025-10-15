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
import java.util.HashMap;
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

    // Registro con auto-login y JWT
    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createUsuario(
            @RequestBody UsuarioDTO dto,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // IMPORTANTE: Eliminar foto_perfil si viene en el DTO
            dto.setFotoPerfil(null);
            
            UsuarioDTO u = usuarioService.createUsuario(dto);

            // Auto-login y generación de token JWT
            String token = null;
            try {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword());
                Authentication auth = authenticationManager.authenticate(authToken);
                SecurityContextHolder.getContext().setAuthentication(auth);
                
                // Generar token JWT
                Usuario usuario = usuarioService.findByEmail(dto.getEmail());
                if (usuario != null) {
                    token = jwtUtil.generateToken(usuario.getId(), usuario.getEmail());
                    log.info("Usuario {} registrado y autenticado con JWT", dto.getEmail());
                }
            } catch (Exception authEx) {
                log.warn("Auto-login falló para {}: {}", dto.getEmail(), authEx.getMessage());
            }

            u.setPassword(null); // No devolver password
            
            // Respuesta con token y usuario
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", u);
            if (token != null) {
                responseData.put("token", token);
            }
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(responseData, "Usuario creado", true));
        } catch (IllegalArgumentException e) {
            log.error("Error creando usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("Error inesperado creando usuario", e);
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(null, "No autenticado", false));
            }
            UsuarioDTO dto = usuarioService.getUsuario(currentUserId);
            dto.setPassword(null);
            return ResponseEntity.ok(new ApiResponse<>(dto, "Usuario actual", true));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    // Actualizar perfil del usuario autenticado
    @PutMapping(path = "/me", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> actualizarPerfil(
            @RequestBody PerfilDTO perfilDTO,
            @RequestHeader(value = "X-USER-ID", required = false) String usuarioIdHeader) {
        try {
            UUID usuarioId = resolveUserIdFromHeaderOrAuth(usuarioIdHeader);
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(null, "No autenticado", false));
            }

            Usuario updated = usuarioService.actualizarPerfil(usuarioId, perfilDTO);
            UsuarioDTO dto = usuarioService.getUsuario(usuarioId);
            dto.setPassword(null);
            return ResponseEntity.ok(new ApiResponse<>(dto, "Perfil actualizado", true));
        } catch (RuntimeException e) {
            log.error("Error actualizando perfil", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    // Subir foto del usuario autenticado
    @PostMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json")
    public ResponseEntity<?> subirFotoMe(@RequestParam("file") MultipartFile file) {
        try {
            UUID userId = resolveCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(null, "No autenticado", false));
            }

            usuarioService.subirFoto(userId, file);
            return ResponseEntity.ok(new ApiResponse<>(null, "Foto subida correctamente", true));
        } catch (IOException ioe) {
            log.error("Error guardando foto: {}", ioe.getMessage(), ioe);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error guardando foto", false));
        } catch (Exception e) {
            log.error("Error inesperado subiendo foto", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    // Verificar cédula del usuario autenticado
    @PostMapping(path = "/me/verify-cedula", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> verificarCedulaMe(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-USER-ID", required = false) String usuarioIdHeader,
            HttpServletRequest request) {
        
        String cedula = body.get("cedula");
        if (cedula == null || cedula.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Cédula requerida"
            ));
        }

        UUID usuarioId = resolveUserIdFromHeaderOrAuth(usuarioIdHeader);
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "No autenticado"
            ));
        }

        // Validar cédula
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
        
        // Generar nuevo token con info actualizada
        String newToken = jwtUtil.generateToken(updated.getId(), updated.getEmail());

        // Actualizar autenticación en contexto
        try {
            CustomUserDetailsService.UserPrincipal userPrincipal =
                    new CustomUserDetailsService.UserPrincipal(
                            updated.getId(),
                            updated.getEmail(),
                            updated.getPassword(),
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userPrincipal, null, userPrincipal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
            log.warn("No se pudo actualizar Authentication tras verificar cédula: {}", e.getMessage());
        }

        UsuarioDTO dto = usuarioService.getUsuario(usuarioId);
        dto.setPassword(null);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cédula verificada y guardada",
                "data", Map.of(
                    "verified", true, 
                    "user", dto,
                    "token", newToken
                )
        ));
    }

    // Obtener foto de un usuario
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

    // Listar todos los usuarios
    @GetMapping(produces = "application/json")
    public ResponseEntity<ApiResponse<List<UsuarioDTO>>> getAllUsuarios() {
        List<UsuarioDTO> all = usuarioService.getAllUsuarios();
        return ResponseEntity.ok(new ApiResponse<>(all, "Lista de usuarios", true));
    }

    // Obtener un usuario por ID
    @GetMapping(path = "/{id}", produces = "application/json")
    public ResponseEntity<ApiResponse<UsuarioDTO>> getUsuario(@PathVariable UUID id) {
        try {
            UsuarioDTO dto = usuarioService.getUsuario(id);
            dto.setPassword(null);
            return ResponseEntity.ok(new ApiResponse<>(dto, "Usuario encontrado", true));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    // Eliminar un usuario
    @DeleteMapping(path = "/{id}", produces = "application/json")
    public ResponseEntity<ApiResponse<Void>> deleteUsuario(@PathVariable UUID id) {
        usuarioService.deleteUsuario(id);
        return ResponseEntity.ok(new ApiResponse<>(null, "Usuario eliminado", true));
    }

    // Helper: resolver ID del usuario desde header o authentication
    private UUID resolveUserIdFromHeaderOrAuth(String headerUserId) {
        if (headerUserId != null && !headerUserId.isBlank()) {
            try {
                return UUID.fromString(headerUserId);
            } catch (IllegalArgumentException e) {
                log.warn("Header X-USER-ID con formato inválido: {}", headerUserId);
            }
        }
        return resolveCurrentUserId();
    }

    // Helper: resolver ID del usuario autenticado
    private UUID resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        
        Object principal = auth.getPrincipal();

        try {
            // Si usas el UserPrincipal personalizado
            if (principal instanceof CustomUserDetailsService.UserPrincipal) {
                return ((CustomUserDetailsService.UserPrincipal) principal).getId();
            }
            
            // Si es UserDetails estándar: usar username (email) para buscar
            if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                UUID userId = usuarioService.findUserIdByEmail(username);
                if (userId != null) return userId;
            }
            
            // Si principal es String (username/email)
            if (principal instanceof String) {
                String username = (String) principal;
                UUID userId = usuarioService.findUserIdByEmail(username);
                if (userId != null) return userId;
            }
        } catch (Exception e) {
            log.warn("No se pudo resolver usuario desde principal: {}", e.getMessage());
        }
        
        return null;
    }
}