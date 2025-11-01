package uy.um.faltauno.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uy.um.faltauno.config.CustomUserDetailsService.UserPrincipal;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interceptor que actualiza last_activity_at en cada request autenticado.
 * Permite calcular "Usuarios activos ahora" en tiempo real.
 * 
 * OPTIMIZACIÓN:
 * - Solo actualiza si han pasado más de 60 segundos desde la última actividad
 * - Ejecución asíncrona para no bloquear el request
 * - Solo para requests autenticados (con token válido)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityTrackingInterceptor implements HandlerInterceptor {

    private final UsuarioRepository usuarioRepository;
    
    // Actualizar solo si han pasado más de 60 segundos (reducir writes a DB)
    private static final long UPDATE_THRESHOLD_SECONDS = 60;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Solo para requests autenticados
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
            UUID userId = userPrincipal.getId();
            
            if (userId != null) {
                updateUserActivity(userId);
            }
        }
        
        return true; // Continuar con el request
    }

    /**
     * Actualización asíncrona de actividad del usuario.
     * Solo actualiza si han pasado más de 60 segundos desde la última actividad.
     */
    @Async
    public void updateUserActivity(UUID userId) {
        try {
            Usuario usuario = usuarioRepository.findById(userId).orElse(null);
            
            if (usuario == null) {
                log.trace("[ActivityTracking] Usuario no encontrado: {}", userId);
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastActivity = usuario.getLastActivityAt();
            
            // Solo actualizar si han pasado más de 60 segundos
            if (lastActivity == null || 
                lastActivity.plusSeconds(UPDATE_THRESHOLD_SECONDS).isBefore(now)) {
                
                usuario.setLastActivityAt(now);
                usuarioRepository.save(usuario);
                
                log.trace("[ActivityTracking] ✅ Actividad actualizada: userId={}", userId);
            } else {
                log.trace("[ActivityTracking] ⏭️ Skip (< 60s): userId={}", userId);
            }
            
        } catch (Exception e) {
            // No fallar el request por error de tracking
            log.warn("[ActivityTracking] Error actualizando actividad: {}", e.getMessage());
        }
    }
}
