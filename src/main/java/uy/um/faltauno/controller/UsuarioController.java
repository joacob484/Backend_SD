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
import uy.um.faltauno.repository.UsuarioRepository;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.service.UsuarioService;
import uy.um.faltauno.service.ReviewService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Slf4j
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final ReviewService reviewService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PartidoRepository partidoRepository;

    // ================================
    // Registro con auto-login y JWT
    // ================================
    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createUsuario(
            @RequestBody UsuarioDTO dto,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            dto.setFotoPerfil(null); // eliminar foto si viene en DTO
            UsuarioDTO u = usuarioService.createUsuario(dto);

            String token = null;
            try {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword());
                Authentication auth = authenticationManager.authenticate(authToken);
                SecurityContextHolder.getContext().setAuthentication(auth);

                Usuario usuario = usuarioService.findByEmail(dto.getEmail());
                if (usuario != null) {
                    token = jwtUtil.generateToken(usuario.getId(), usuario.getEmail(), usuario.getTokenVersion());
                    log.info("Usuario {} registrado y autenticado con JWT (tokenVersion: {})", dto.getEmail(), usuario.getTokenVersion());
                }
            } catch (Exception authEx) {
                log.warn("Auto-login falló para {}: {}", dto.getEmail(), authEx.getMessage());
            }

            u.setPassword(null);
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", u);
            if (token != null) responseData.put("token", token);

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

    // ================================
    // Endpoints de usuario actual
    // ================================
    @GetMapping(path = "/me", produces = "application/json")
    public ResponseEntity<ApiResponse<UsuarioDTO>> getMe() {
        try {
            UUID currentUserId = resolveCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(null, "No autenticado", false));
            }
            
            // 🔍 DEBUG: Obtener entidad directamente antes del mapper
            Usuario usuarioEntity = usuarioService.findUsuarioEntityById(currentUserId);
            log.info("[UsuarioController] 🔍 /me - Entidad ANTES de mapper: fotoPerfil en DB: {}", 
                usuarioEntity.getFotoPerfil() != null ? "SÍ (" + usuarioEntity.getFotoPerfil().length + " bytes)" : "NO");
            
            UsuarioDTO dto = usuarioService.getUsuario(currentUserId);
            
            // 🔍 DEBUG: Log del usuario retornado
            log.info("[UsuarioController] 🔍 /me - DTO DESPUÉS de mapper: {} | perfilCompleto: {} | hasFotoPerfil: {} | fotoPerfil: {}", 
                dto.getEmail(),
                dto.getPerfilCompleto(),
                dto.getHasFotoPerfil(),
                dto.getFotoPerfil() != null ? "SÍ (" + dto.getFotoPerfil().length() + " chars)" : "NO");
            
            dto.setPassword(null);
            return ResponseEntity.ok(new ApiResponse<>(dto, "Usuario actual", true));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    @GetMapping(path = "/me/amigos", produces = "application/json")
    public ResponseEntity<ApiResponse<List<UsuarioDTO>>> getAmigos() {
        try {
            UUID currentUserId = resolveCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(null, "No autenticado", false));
            }
            
            // TODO: Implementar relación de amistad en el futuro
            // Por ahora, retornar lista vacía o usuarios sugeridos
            List<UsuarioDTO> amigos = usuarioService.obtenerAmigosSugeridos(currentUserId);
            
            return ResponseEntity.ok(new ApiResponse<>(amigos, "Lista de amigos", true));
        } catch (RuntimeException e) {
            log.error("Error obteniendo amigos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

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

            // Actualizar perfil
            usuarioService.actualizarPerfil(usuarioId, perfilDTO);
            
            // Obtener datos actualizados
            UsuarioDTO dto = usuarioService.getUsuario(usuarioId);
            dto.setPassword(null);
            
            return ResponseEntity.ok(new ApiResponse<>(dto, "Perfil actualizado", true));
        } catch (IllegalArgumentException e) {
            log.warn("Recurso no encontrado al actualizar perfil: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (RuntimeException e) {
            log.error("Error actualizando perfil", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error interno", false));
        }
    }

    // ================================
    // Foto del usuario
    // ================================
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

    @GetMapping(path = "/{id}/foto", produces = { MediaType.IMAGE_JPEG_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE })
    public ResponseEntity<byte[]> getFoto(@PathVariable("id") UUID id) {
        Usuario u = usuarioService.findUsuarioEntityById(id);
        if (u == null || u.getFotoPerfil() == null) return ResponseEntity.notFound().build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        return new ResponseEntity<>(u.getFotoPerfil(), headers, HttpStatus.OK);
    }

    // ================================
    // Verificación cédula
    // ================================
    @PostMapping(path = "/me/verify-cedula", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> verificarCedulaMe(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-USER-ID", required = false) String usuarioIdHeader) {

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

        boolean valida = usuarioService.verificarCedula(cedula);
        if (!valida) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "data", Map.of("verified", false),
                    "message", "Cédula inválida"
            ));
        }

        Usuario updated = usuarioService.marcarCedula(usuarioId, cedula);
        String newToken = jwtUtil.generateToken(updated.getId(), updated.getEmail(), updated.getTokenVersion());

        // Actualizar Authentication
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

    // ================================
    // Usuarios CRUD
    // ================================
    @GetMapping(produces = "application/json")
    public ResponseEntity<ApiResponse<List<UsuarioDTO>>> getAllUsuarios() {
        List<UsuarioDTO> all = usuarioService.getAllUsuarios();
        return ResponseEntity.ok(new ApiResponse<>(all, "Lista de usuarios", true));
    }

    /**
     * Buscar usuarios por números de teléfono (para sincronización de contactos)
     * POST /api/usuarios/buscar-por-telefonos
     * Body: { "telefonos": ["+598...", "+54..."] }
     */
    @PostMapping(path = "/buscar-por-telefonos", produces = "application/json", consumes = "application/json")
    public ResponseEntity<ApiResponse<List<UsuarioDTO>>> buscarPorTelefonos(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> telefonos = request.get("telefonos");
            if (telefonos == null || telefonos.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Lista de teléfonos requerida", false));
            }

            log.info("Buscando usuarios para {} números de teléfono", telefonos.size());
            
            List<UsuarioDTO> usuarios = usuarioService.buscarPorTelefonos(telefonos);
            
            // Limpiar passwords
            usuarios.forEach(u -> u.setPassword(null));
            
            log.info("Encontrados {} usuarios de {} teléfonos", usuarios.size(), telefonos.size());
            
            return ResponseEntity.ok(new ApiResponse<>(usuarios, 
                    "Usuarios encontrados: " + usuarios.size(), true));
        } catch (Exception e) {
            log.error("Error buscando usuarios por teléfono", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al buscar usuarios", false));
        }
    }

    @GetMapping(path = "/{id}", produces = "application/json")
    public ResponseEntity<ApiResponse<UsuarioDTO>> getUsuario(@PathVariable UUID id) {
        try {
            UsuarioDTO dto = usuarioService.getUsuario(id);
            dto.setPassword(null);
            return ResponseEntity.ok(new ApiResponse<>(dto, "Usuario encontrado", true));
        } catch (IllegalArgumentException e) {
            // Distinguir entre "eliminado" y "no encontrado"
            if (e.getMessage().equals("Usuario eliminado")) {
                return ResponseEntity.status(HttpStatus.GONE) // 410 Gone
                        .body(new ApiResponse<>(null, "Usuario eliminado", false));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (RuntimeException e) {
            log.error("Error obteniendo usuario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error interno", false));
        }
    }

    @DeleteMapping(path = "/{id}", produces = "application/json")
    public ResponseEntity<ApiResponse<Void>> deleteUsuario(@PathVariable UUID id) {
        try {
            usuarioService.deleteUsuario(id);
            return ResponseEntity.ok(new ApiResponse<>(null, "Usuario eliminado correctamente", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("Error eliminando usuario: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al eliminar usuario", false));
        }
    }

    /**
     * Eliminar cuenta del usuario actual
     * DELETE /api/usuarios/me
     */
    @DeleteMapping(path = "/me", produces = "application/json")
    public ResponseEntity<ApiResponse<Void>> deleteMe() {
        try {
            UUID currentUserId = resolveCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(null, "No autenticado", false));
            }
            
            usuarioService.deleteUsuario(currentUserId);
            return ResponseEntity.ok(new ApiResponse<>(null, "Tu cuenta ha sido eliminada correctamente", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("Error eliminando cuenta: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al eliminar cuenta", false));
        }
    }

    /**
     * Verificar si existe un usuario eliminado recuperable (dentro de 30 días)
     * GET /api/usuarios/check-deleted?email=user@example.com
     * 
     * Response: { "success": true, "data": { "canRecover": true }, "message": "..." }
     */
    @GetMapping(path = "/check-deleted", produces = "application/json")
    public ResponseEntity<ApiResponse<java.util.Map<String, Boolean>>> checkDeletedUser(
            @RequestParam String email) {
        
        boolean canRecover = usuarioService.hasRecoverableDeletedUser(email);
        
        java.util.Map<String, Boolean> response = new java.util.HashMap<>();
        response.put("canRecover", canRecover);
        
        String message = canRecover 
            ? "Cuenta eliminada encontrada. Puede recuperarla dentro de los 30 días."
            : "No existe cuenta eliminada recuperable con ese email.";
        
        return ResponseEntity.ok(new ApiResponse<>(response, message, true));
    }

    /**
     * Recuperar usuario eliminado (dentro del plazo de 30 días)
     * POST /api/usuarios/recover
     * Body: { "email": "user@example.com" }
     * 
     * Response: { "success": true, "data": UsuarioDTO, "message": "Cuenta recuperada" }
     */
    @PostMapping(path = "/recover", produces = "application/json", consumes = "application/json")
    public ResponseEntity<ApiResponse<UsuarioDTO>> recoverDeletedUser(
            @RequestBody java.util.Map<String, String> body) {
        
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(null, "Email es requerido", false));
        }
        
        try {
            Usuario usuario = usuarioService.recoverDeletedUser(email);
            UsuarioDTO dto = usuarioService.getUsuario(usuario.getId());
            
            return ResponseEntity.ok(new ApiResponse<>(dto, 
                "Cuenta recuperada exitosamente. Puede iniciar sesión ahora.", true));
                
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.GONE)  // 410 Gone
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("Error recuperando usuario: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al recuperar usuario", false));
        }
    }

    // ================================
    // Helpers
    // ================================
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

    private UUID resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();

        try {
            if (principal instanceof CustomUserDetailsService.UserPrincipal) {
                return ((CustomUserDetailsService.UserPrincipal) principal).getId();
            }
            if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                return usuarioService.findUserIdByEmail(username);
            }
            if (principal instanceof String) {
                return usuarioService.findUserIdByEmail((String) principal);
            }
        } catch (Exception e) {
            log.warn("No se pudo resolver usuario desde principal: {}", e.getMessage());
        }

        return null;
    }

    // ================================
    // Preferencias de notificación
    // ================================
    
    /**
     * Obtener preferencias de notificación del usuario actual
     */
    @GetMapping(path = "/me/notification-preferences", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationPreferences() {
        try {
            UUID currentUserId = resolveCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(null, "No autenticado", false));
            }

            Map<String, Object> preferences = usuarioService.getNotificationPreferences(currentUserId);
            return ResponseEntity.ok(new ApiResponse<>(preferences, "Preferencias obtenidas", true));

        } catch (Exception e) {
            log.error("Error obteniendo preferencias de notificación", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error obteniendo preferencias", false));
        }
    }

    /**
     * Actualizar preferencias de notificación del usuario actual
     */
    @PutMapping(path = "/me/notification-preferences", 
                consumes = "application/json", 
                produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateNotificationPreferences(
            @RequestBody Map<String, Boolean> preferences) {
        try {
            UUID currentUserId = resolveCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(null, "No autenticado", false));
            }

            Map<String, Object> updated = usuarioService.updateNotificationPreferences(currentUserId, preferences);
            return ResponseEntity.ok(new ApiResponse<>(updated, "Preferencias actualizadas", true));

        } catch (Exception e) {
            log.error("Error actualizando preferencias de notificación", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error actualizando preferencias", false));
        }
    }

    /**
     * Obtener reviews pendientes de un usuario específico
     * GET /api/usuarios/{usuarioId}/pending-reviews
     */
    @GetMapping("/{usuarioId}/pending-reviews")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingReviews(
            @PathVariable UUID usuarioId,
            Authentication auth) {
        try {
            // Verificar que el usuario autenticado sea el mismo que está consultando
            UUID currentUserId = resolveCurrentUserId();
            if (currentUserId == null || !currentUserId.equals(usuarioId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(null, "No autorizado para ver estas reviews", false));
            }

            List<Map<String, Object>> pendientes = reviewService.obtenerReviewsPendientes(auth);
            return ResponseEntity.ok(new ApiResponse<>(pendientes, "Reviews pendientes", true));

        } catch (Exception e) {
            log.error("Error obteniendo reviews pendientes del usuario {}", usuarioId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener reviews pendientes", false));
        }
    }
    
    /**
     * Obtener estadísticas de la plataforma
     * GET /api/usuarios/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        try {
            Map<String, Object> stats = new java.util.HashMap<>();
            
            // Total de usuarios registrados (excluyendo soft-deleted)
            // ⚡ IMPORTANTE: No usar count() porque incluye usuarios con deletedAt != NULL
            long totalUsers = usuarioRepository.countActiveUsers();
            stats.put("totalUsuarios", totalUsers);
            
            // Usuarios activos en los últimos 15 minutos
            java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusMinutes(15);
            long activeUsers = usuarioRepository.countByLastActivityAtAfter(cutoff);
            stats.put("usuariosActivos", activeUsers);
            
            // Total de partidos
            long totalMatches = partidoRepository.count();
            stats.put("totalPartidos", totalMatches);
            
            return ResponseEntity.ok(new ApiResponse<>(stats, "Estadísticas de la plataforma", true));
            
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener estadísticas", false));
        }
    }
}