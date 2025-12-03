package uy.um.faltauno.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uy.um.faltauno.dto.OnboardingStatusDTO;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.service.UsuarioService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Evita que un usuario saltee el proceso obligatorio de onboarding accediendo directamente a rutas protegidas.
 * Bloquea cualquier request que no pertenezca al paso que corresponde ejecutar a ese usuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnboardingGuardInterceptor implements HandlerInterceptor {

    private static final List<String> VERIFICATION_PATHS = List.of(
            "/api/auth",
            "/api/verification",
            "/api/password",
            "/api/health",
            "/api/actuator"
    );

    private static final List<String> PROFILE_PATHS = List.of(
            "/api/usuarios/me",
            "/api/photos",
            "/api/photo",
            "/api/usuarios/me/foto",
            "/api/usuarios/me/profile",
            "/api/usuarios/me/amigos"
    );

    private static final List<String> CEDULA_PATHS = List.of(
            "/api/cedula",
            "/api/usuarios/me/verify-cedula"
    );

    private final UsuarioService usuarioService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return true;
        }

        Usuario usuario = resolveUsuario(authentication);
        if (usuario == null) {
            return true;
        }

        OnboardingStatusDTO onboarding = usuarioService.computeOnboardingStatus(usuario.getId());
        if (!onboarding.isRequiresAction()) {
            return true;
        }

        String path = normalizePath(request.getRequestURI());
        if (isPathAllowedForStep(path, onboarding.getNextStep())) {
            return true;
        }

        log.info("[OnboardingGuard] Bloqueando {} {} - step pendiente: {}", request.getMethod(), path, onboarding.getNextStep());
        writeBlockingResponse(response, onboarding);
        return false;
    }

    private Usuario resolveUsuario(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Usuario usuario) {
            return usuario;
        }
        String email = null;
        if (principal instanceof UserDetails details) {
            email = details.getUsername();
        } else if (principal instanceof String str) {
            email = str;
        }
        if (email == null) {
            return null;
        }
        return usuarioService.findByEmail(email);
    }

    private String normalizePath(String uri) {
        if (uri == null) {
            return "";
        }
        return uri.toLowerCase(Locale.ROOT);
    }

    private boolean isPathAllowedForStep(String path, OnboardingStatusDTO.Step step) {
        if (matchesAny(path, VERIFICATION_PATHS)) {
            return true;
        }
        switch (step) {
            case VERIFY_EMAIL:
                return false;
            case COMPLETE_PROFILE:
                return matchesAny(path, PROFILE_PATHS);
            case VERIFY_CEDULA:
                return matchesAny(path, PROFILE_PATHS) || matchesAny(path, CEDULA_PATHS);
            default:
                return true;
        }
    }

    private boolean matchesAny(String path, List<String> prefixes) {
        return prefixes.stream().anyMatch(path::startsWith);
    }

    private void writeBlockingResponse(HttpServletResponse response, OnboardingStatusDTO onboarding) throws IOException {
        response.setStatus(428); // Precondition Required
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");

        Map<String, Object> payload = Map.of(
                "success", false,
                "message", onboarding.getBlockingReason(),
                "data", Map.of(
                        "onboarding", onboarding,
                        "nextStep", onboarding.getNextStep()
                )
        );
        response.getWriter().write(objectMapper.writeValueAsString(payload));
    }
}
