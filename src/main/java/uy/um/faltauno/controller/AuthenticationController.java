package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.config.JwtUtil;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.dto.UsuarioDTO;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;

    public static record LoginRequest(String email, String password) {}

    @PostMapping(path = "/login-json", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginJson(@RequestBody LoginRequest req, HttpServletRequest request) {
        if (req == null || req.email() == null || req.password() == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(null, "email y password requeridos", false));
        }

        try {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(req.email(), req.password());

            Authentication auth = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(auth);
            HttpSession session = request.getSession(true);
            log.info("Login JSON exitoso para {}, session={}", req.email(), session != null ? session.getId() : "null");

            Usuario u = usuarioService.findByEmail(req.email());
            if (u == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(null, "Usuario no encontrado", false));
            }

            UsuarioDTO dto = usuarioService.getUsuario(u.getId());
            dto.setPassword(null);
            
            // Asegurar que los campos calculados están presentes
            dto.setPerfilCompleto(dto.getPerfilCompleto());
            dto.setCedulaVerificada(dto.getCedulaVerificada());

            String token = jwtUtil.generateToken(u.getId(), u.getEmail());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", token);
            responseData.put("user", dto);

            return ResponseEntity.ok(new ApiResponse<>(responseData, "Autenticado", true));
        } catch (BadCredentialsException ex) {
            log.warn("Credenciales inválidas para {}: {}", req.email(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(null, "Credenciales inválidas", false));
        } catch (DisabledException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(null, "Cuenta deshabilitada", false));
        } catch (LockedException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(null, "Cuenta bloqueada", false));
        } catch (AuthenticationException ex) {
            log.warn("Error autenticación: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(null, "No autorizado", false));
        } catch (Exception ex) {
            log.error("Error inesperado en loginJson", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error interno", false));
        }
    }

    @PostMapping(path = "/logout", produces = "application/json")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) session.invalidate();
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok(new ApiResponse<>(null, "Desconectado", true));
        } catch (Exception ex) {
            log.warn("Error en logout: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error cerrando sesión", false));
        }
    }

    @GetMapping(path = "/me", produces = "application/json")
    public ResponseEntity<ApiResponse<UsuarioDTO>> me() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(null, "No autenticado", false));
            }

            String username = null;
            Object principal = auth.getPrincipal();
            if (principal instanceof String) username = (String) principal;
            else {
                try {
                    var pd = (org.springframework.security.core.userdetails.UserDetails) principal;
                    username = pd.getUsername();
                } catch (ClassCastException ignored) { }
            }

            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(null, "No autenticado", false));
            }

            Usuario u = usuarioService.findByEmail(username);
            if (u == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(null, "Usuario no encontrado", false));
            }

            UsuarioDTO dto = usuarioService.getUsuario(u.getId());
            dto.setPassword(null);
            return ResponseEntity.ok(new ApiResponse<>(dto, "Usuario actual", true));
        } catch (Exception ex) {
            log.error("Error en /api/auth/me", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error interno", false));
        }
    }
}