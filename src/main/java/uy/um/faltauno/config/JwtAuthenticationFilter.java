package uy.um.faltauno.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
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
 * Filtro de autenticación JWT - VERSIÓN CORREGIDA
 * Extrae y valida el token, luego establece el Authentication en el SecurityContext
 */
@Component
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
        String method = request.getMethod();
        
        log.debug("🔍 JwtFilter processing: {} {}", method, path);

        try {
            String header = request.getHeader("Authorization");
            
            // Si no hay header Authorization, continuar sin autenticar
            if (header == null || !header.startsWith("Bearer ")) {
                log.debug("⚪ No Bearer token found for: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            String token = header.substring(7).trim();
            log.debug("🔑 Token found, validating...");
            
            // Validar token
            if (!jwtUtil.validateToken(token)) {
                log.warn("❌ Invalid or expired JWT token for: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            // Extraer información del token
            String email = jwtUtil.extractEmail(token);
            UUID userId = jwtUtil.extractUserId(token);
            
            if (email == null) {
                log.warn("❌ JWT token without email (subject) for: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            // Si ya hay autenticación en el contexto, no sobrescribir
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("⚠️ Authentication already exists in context");
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
            
            log.info("✅ User authenticated via JWT: {} (userId: {}) for: {}", email, userId, path);

        } catch (Exception ex) {
            // Limpiar el contexto si algo falla
            SecurityContextHolder.clearContext();
            log.error("💥 Error in JwtAuthenticationFilter for {}: {}", path, ex.getMessage());
            // No lanzar excepción - dejar que continue sin autenticación
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determinar si el filtro debe ejecutarse para esta request
     * @return true si NO debe aplicarse el filtro (endpoints públicos)
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        
        // ✅ Endpoints públicos que NO requieren JWT
        
        // 1. OAuth2 / Login social
        if (path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/")) {
            return true;
        }
        
        // 2. Login tradicional
        if (path.startsWith("/api/auth/login")) {
            return true;
        }
        
        // 3. Registro de nuevos usuarios (POST /api/usuarios)
        if (path.equals("/api/usuarios") && "POST".equals(method)) {
            return true;
        }
        
        // 4. Endpoints públicos
        if (path.startsWith("/public/")) {
            return true;
        }
        
        // 5. Health check
        if (path.startsWith("/actuator/health")) {
            return true;
        }
        
        // 6. H2 Console (solo en desarrollo)
        if (path.startsWith("/h2-console")) {
            return true;
        }
        
        // 7. Error page
        if (path.equals("/error")) {
            return true;
        }
        
        // ✅ Todos los demás requieren autenticación
        return false;
    }
}