package uy.um.faltauno.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de rate limiting simple basado en IP para proteger endpoints de autenticación.
 * Limita a 5 intentos por minuto por IP en endpoints sensibles.
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 5;
    private static final long WINDOW_SIZE_MS = 60_000; // 1 minuto
    
    // Mapa: IP -> [timestamp_ultima_limpieza, contador]
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String clientIP = getClientIP(request);
        String path = request.getServletPath();
        String method = request.getMethod();
        
        // Solo aplicar rate limiting a endpoints de autenticación
        if (shouldRateLimit(path, method)) {
            RequestCounter counter = requestCounts.computeIfAbsent(clientIP, k -> new RequestCounter());
            
            synchronized (counter) {
                long now = System.currentTimeMillis();
                
                // Resetear contador si pasó la ventana de tiempo
                if (now - counter.windowStart > WINDOW_SIZE_MS) {
                    counter.count.set(0);
                    counter.windowStart = now;
                }
                
                // Verificar límite
                if (counter.count.get() >= MAX_REQUESTS_PER_MINUTE) {
                    log.warn("🚫 Rate limit excedido para IP: {} en {} {}", clientIP, method, path);
                    response.setStatus(429); // HTTP 429 Too Many Requests
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"success\":false,\"message\":\"Demasiados intentos. Intenta nuevamente en un minuto.\",\"data\":null}"
                    );
                    return;
                }
                
                counter.count.incrementAndGet();
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean shouldRateLimit(String path, String method) {
        // Rate limit en endpoints de autenticación
        return ("POST".equals(method) && path.startsWith("/api/auth/login")) ||
               ("POST".equals(method) && path.startsWith("/api/usuarios") && !path.contains("/"));
    }

    private String getClientIP(HttpServletRequest request) {
        // Considerar headers de proxy
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Tomar la primera IP si hay múltiples
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Clase interna para mantener contador por IP
     */
    private static class RequestCounter {
        AtomicInteger count = new AtomicInteger(0);
        long windowStart = System.currentTimeMillis();
    }
    
    /**
     * Método para limpiar entradas antiguas (puede ser llamado por un scheduler)
     */
    public void cleanupOldEntries() {
        long now = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(entry -> 
            now - entry.getValue().windowStart > WINDOW_SIZE_MS * 2
        );
    }
}
