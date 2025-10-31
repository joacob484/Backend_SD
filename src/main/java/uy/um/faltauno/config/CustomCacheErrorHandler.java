package uy.um.faltauno.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.stereotype.Component;

/**
 * Manejador de errores de cache que ignora fallos silenciosamente
 * Útil cuando Redis no está disponible pero queremos que la app funcione
 */
@Component
@Slf4j
public class CustomCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache GET error ignorado para cache: {}, key: {} - {}", 
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Cache PUT error ignorado para cache: {}, key: {} - {}", 
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache EVICT error ignorado para cache: {}, key: {} - {}", 
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache CLEAR error ignorado para cache: {} - {}", 
                cache.getName(), exception.getMessage());
    }
}
