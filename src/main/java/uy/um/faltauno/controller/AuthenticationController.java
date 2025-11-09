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
    private final uy.um.faltauno.service.VerificationService verificationService;
    private final JwtUtil jwtUtil;

    public static record LoginRequest(String email, String password) {}

    @PostMapping(path = "/login-json", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginJson(@RequestBody LoginRequest req, HttpServletRequest request) {
        if (req == null || req.email() == null || req.password() == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(null, "email y password requeridos", false));
        }

        try {
            // ✅ NUEVO: Verificar si existe usuario eliminado recuperable ANTES de autenticar
            if (usuarioService.hasRecoverableDeletedUser(req.email())) {
                Map<String, Object> deletedUserResponse = new HashMap<>();
                deletedUserResponse.put("deletedUser", true);
                deletedUserResponse.put("canRecover", true);
                deletedUserResponse.put("email", req.email());
                
                return ResponseEntity.status(HttpStatus.FORBIDDEN)  // 403 Forbidden
                        .body(new ApiResponse<>(deletedUserResponse, 
                            "Tu cuenta fue eliminada. ¿Deseas recuperarla o esperar a que se elimine definitivamente en 30 días?", 
                            false));
            }

            // ...existing code...

            // ⚡ IMPORTANTE: NO rechazar si emailVerified=false
            // El frontend manejará la redirección a verificación
            // Solo autenticar email + password

            // 🔍 DEBUG: Log para diagnosticar problema de password
            Usuario existingUser = usuarioService.findByEmail(req.email());

            // Si NO existe usuario -> si hay pre-registro informar PRE_REGISTERED; si no, responder USER_NOT_FOUND
            if (existingUser == null) {
                if (verificationService.tienePreRegistroPendiente(req.email())) {
                    Map<String, Object> preRegResp = new HashMap<>();
                    preRegResp.put("preRegistered", true);
                    preRegResp.put("email", req.email());
                    // In dev mode include the code for convenience (do NOT expose in production)
                    String mailUsername = System.getenv("MAIL_USERNAME");
                    boolean isEmailConfigured = mailUsername != null && !mailUsername.isBlank();
                    if (!isEmailConfigured) {
                        preRegResp.put("debugMode", true);
                    }

                    // 401: authentication required, but indicate pre-registration state
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new ApiResponse<>(preRegResp, "Email registrado pero pendiente de verificación.", false));
                }

        // 400 explícito: email no registrado (usamos 400 para que el frontend reciba el mensaje detallado)
        Map<String, Object> notFound = new HashMap<>();
        notFound.put("errorCode", "USER_NOT_FOUND");
        notFound.put("email", req.email());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ApiResponse<>(notFound, "Email no registrado.", false));
            }
            if (existingUser != null) {
                log.info("[AuthenticationController] 🔍 Usuario encontrado: {}", req.email());
                log.info("[AuthenticationController] 🔍 Provider: {}", existingUser.getProvider());
                log.info("[AuthenticationController] 🔍 Password hash presente: {}", 
                    existingUser.getPassword() != null && !existingUser.getPassword().isEmpty());
                Boolean emailVerificado = existingUser.getEmailVerified();
                log.info("[AuthenticationController] 🔍 Email verificado: {}", emailVerificado != null ? emailVerificado : false);
                
                if (existingUser.getPassword() != null && !existingUser.getPassword().isEmpty()) {
                    log.info("[AuthenticationController] 🔍 Password hash (primeros 20 chars): {}", 
                        existingUser.getPassword().substring(0, Math.min(20, existingUser.getPassword().length())));
                } else {
                    log.warn("[AuthenticationController] ⚠️ Password es NULL - Usuario no puede hacer login con contraseña");
                }
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(req.email(), req.password());

            Authentication auth = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(auth);
            HttpSession session = request.getSession(true);
            log.info("Login JSON exitoso para {}, session={}", req.email(), session != null ? session.getId() : "null");

            UsuarioDTO dto = usuarioService.getUsuario(existingUser.getId());
            dto.setPassword(null);
            
            // Asegurar que los campos calculados están presentes
            dto.setPerfilCompleto(dto.getPerfilCompleto());
            dto.setCedulaVerificada(dto.getCedulaVerificada());

            String token = jwtUtil.generateToken(existingUser.getId(), existingUser.getEmail(), existingUser.getTokenVersion(), existingUser.getRol());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", token);
            responseData.put("user", dto);

            return ResponseEntity.ok(new ApiResponse<>(responseData, "Autenticado", true));
        } catch (BadCredentialsException ex) {
            log.warn("[AuthenticationController] ❌ Credenciales inválidas para {}: {}", req.email(), ex.getMessage());

            // Construir respuesta estructurada
            Usuario user = usuarioService.findByEmail(req.email());
            Map<String, Object> err = new HashMap<>();
            err.put("errorCode", "BAD_CREDENTIALS");
            err.put("email", req.email());

            if (user != null && user.getPassword() == null) {
                log.error("[AuthenticationController] ❌❌❌ Usuario {} tiene PASSWORD NULL - No puede hacer login", req.email());
                err.put("errorCode", "PASSWORD_NOT_SET");
                err.put("message", "Tu cuenta no tiene contraseña configurada. Por favor contacta soporte o registra una nueva cuenta.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(err, "Tu cuenta no tiene contraseña configurada.", false));
            }

            err.put("message", "Email o contraseña incorrectos.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(err, "Email o contraseña incorrectos.", false));
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