package uy.um.faltauno.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uy.um.faltauno.config.CustomUserDetailsService.UserPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final uy.um.faltauno.repository.UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
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
            Integer tokenVersion = jwtUtil.extractTokenVersion(token);
            
            if (email == null) {
                log.warn("❌ JWT token without email (subject) for: {}", path);
                filterChain.doFilter(request, response);
                return;
            }
            
            if (userId == null) {
                log.warn("❌ JWT token without userId claim for: {}", path);
                filterChain.doFilter(request, response);
                return;
            }
            
            // ✅ ESTÁNDAR INDUSTRIA: Validar tokenVersion contra DB
            if (tokenVersion != null) {
                var userOpt = usuarioRepository.findById(userId);
                if (userOpt.isPresent()) {
                    var user = userOpt.get();
                    if (!tokenVersion.equals(user.getTokenVersion())) {
                        log.warn("❌ Token version mismatch for user {}: token={}, db={}", 
                                userId, tokenVersion, user.getTokenVersion());
                        log.warn("   Token was invalidated (password change, security issue, etc.)");
                        filterChain.doFilter(request, response);
                        return;
                    }
                } else {
                    log.warn("❌ User {} not found in database", userId);
                    filterChain.doFilter(request, response);
                    return;
                }
            } else {
                // Tokens viejos sin tokenVersion: permitir pero loggear warning
                log.warn("⚠️ Token without version for user {} - consider refreshing", userId);
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

            // ✅ CRÍTICO: Crear UserPrincipal con userId como principal
            // Esto evita consultas a la BD en cada request
            UserPrincipal userPrincipal = new UserPrincipal(
                    userId,
                    email,
                    null, // No necesitamos el password para JWT auth
                    authorities
            );
            
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                            userPrincipal, // Principal ahora es UserPrincipal, no String
                            null,
                            authorities
                    );
            
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.info("✅ User authenticated via JWT: {} (userId: {}) for: {}", email, userId, path);

        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.error("💥 Error in JwtAuthenticationFilter for {}: {}", path, ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        
        // ✅ CRÍTICO: No filtrar OPTIONS (CORS preflight)
        if ("OPTIONS".equals(method)) {
            log.debug("⚪ Skipping JWT filter for OPTIONS request: {}", path);
            return true;
        }
        
        // ✅ OAuth2 / Login social
        if (path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/")) {
            return true;
        }
        
        // ✅ Login tradicional
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        
        // ✅ CRÍTICO: Registro de nuevos usuarios (POST /api/usuarios)
        if (path.equals("/api/usuarios") && "POST".equals(method)) {
            log.debug("⚪ Skipping JWT filter for user registration: POST /api/usuarios");
            return true;
        }
        
        // ✅ Endpoints públicos
        if (path.startsWith("/public/")) {
            return true;
        }
        
        // ✅ Health check
        if (path.startsWith("/actuator/health")) {
            return true;
        }
        
        // ✅ H2 Console
        if (path.startsWith("/h2-console")) {
            return true;
        }
        
        // ✅ Error page
        if (path.equals("/error")) {
            return true;
        }
        
        return false;
    }
}