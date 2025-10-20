package uy.um.faltauno.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Filtro de autenticación por JWT.
 * Extrae y valida el token, luego establece el Authentication en el SecurityContext.
 */
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        log.debug("JwtAuthenticationFilter processing: {} {}", request.getMethod(), path);

        try {
            String header = request.getHeader("Authorization");
            
            // Si no hay header Authorization, continuar sin autenticar
            if (header == null || !header.startsWith("Bearer ")) {
                log.debug("No Bearer token found for path: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            String token = header.substring(7).trim();
            log.debug("Token found, validating...");
            
            // Validar token
            if (!jwtUtil.validateToken(token)) {
                log.warn("Token JWT inválido o expirado para path: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            // Extraer información del token
            String email = jwtUtil.extractEmail(token);
            UUID userId = jwtUtil.extractUserId(token);
            
            if (email == null) {
                log.warn("Token JWT sin email (subject) para path: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            // Si ya hay autenticación en el contexto, no sobrescribir
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("Authentication already exists in context");
                filterChain.doFilter(request, response);
                return;
            }

            // Extraer roles del token
            List<SimpleGrantedAuthority> authorities = jwtUtil.getRolesFromToken(token)
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // Crear Authentication con el email como principal
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                            email,  // principal (username/email)
                            null,   // credentials (no necesarias después de validar el token)
                            authorities
                    );
            
            // Agregar detalles de la request (IP, session, etc.)
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Establecer en el contexto de seguridad
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.info("Usuario autenticado vía JWT: {} (userId: {}) para path: {}", email, userId, path);

        } catch (Exception ex) {
            // Limpiar el contexto si algo falla
            SecurityContextHolder.clearContext();
            log.error("Error en JwtAuthenticationFilter para path {}: {}", path, ex.getMessage(), ex);
            // No lanzar excepción - dejar que continue sin autenticación
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Opcional: ignorar ciertos paths (para optimización)
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        
        // No aplicar filtro SOLO a estos endpoints públicos específicos:
        // - Login
        if (path.startsWith("/api/auth/login")) return true;
        if (path.startsWith("/oauth2/")) return true;
        if (path.startsWith("/login/oauth2/")) return true;
        
        // - Registro (POST /api/usuarios pero NO /api/usuarios/me/*)
        if (path.equals("/api/usuarios") && "POST".equals(method)) return true;
        
        // - Endpoints públicos
        if (path.startsWith("/public/")) return true;
        if (path.startsWith("/actuator/health")) return true;
        if (path.startsWith("/api/auth/login")) return true;
        if (path.equals("/api/usuarios") && "POST".equals(method)) return true;
        if (path.startsWith("/public/")) return true;
        if (path.startsWith("/actuator/health")) return true;
        if (path.startsWith("/h2-console")) return true;
        if (path.equals("/error")) return true;
        // Todos los demás requieren autenticación
        return false;
    }
}