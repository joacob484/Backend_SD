package uy.um.faltauno.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import uy.um.faltauno.entity.Usuario;

@Slf4j
@Component
public class AdminRoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireAdmin requireAdmin = handlerMethod.getMethodAnnotation(RequireAdmin.class);
        
        if (requireAdmin == null) {
            // También verificar a nivel de clase
            requireAdmin = handlerMethod.getBeanType().getAnnotation(RequireAdmin.class);
        }

        if (requireAdmin != null) {
            // Verificar que el usuario sea admin
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !(authentication.getPrincipal() instanceof Usuario)) {
                log.warn("Intento de acceso a endpoint de admin sin autenticación");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\":false,\"message\":\"No autenticado\"}");
                return false;
            }

            Usuario usuario = (Usuario) authentication.getPrincipal();
            
            if (!"ADMIN".equals(usuario.getRol())) {
                log.warn("Usuario {} intentó acceder a endpoint de admin sin permisos", usuario.getEmail());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Acceso denegado. Se requieren permisos de administrador.\"}");
                return false;
            }

            log.info("Admin {} accediendo a {}", usuario.getEmail(), request.getRequestURI());
        }

        return true;
    }
}
