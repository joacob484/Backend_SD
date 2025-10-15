package uy.um.faltauno.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Filtro de autenticación por JWT.
 * No inyecta UsuarioService para evitar ciclos. Solo valida el token y coloca Authentication.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil; // componente que valida token y extrae username/claims

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7).trim();
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.getUsernameFromToken(token);

                    // Si necesitás roles reales, jwtUtil puede devolverlos desde claims.
                    List<SimpleGrantedAuthority> authorities = jwtUtil.getRolesFromToken(token)
                            .stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    var auth = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception ex) {
            // limpiar el contexto si algo falla
            SecurityContextHolder.clearContext();
            logger.warn("Error en JwtAuthenticationFilter: " + ex.getMessage(), ex);
        }

        filterChain.doFilter(request, response);
    }
}