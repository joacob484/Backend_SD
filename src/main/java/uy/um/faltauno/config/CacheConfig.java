package uy.um.faltauno.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuración de cache usando Caffeine (in-memory).
 * Caffeine es rápido, simple y no requiere infraestructura externa.
 * Ideal para Cloud Run donde las instancias son efímeras.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {
    
    private final CustomCacheErrorHandler customCacheErrorHandler;
    
    public CacheConfig(CustomCacheErrorHandler customCacheErrorHandler) {
        this.customCacheErrorHandler = customCacheErrorHandler;
    }

    /**
     * Configuración de Caffeine cache manager.
     * - maximumSize: 10,000 entradas (ajustar según memoria disponible)
     * - expireAfterWrite: 10 minutos de TTL
     * - allowNullValues: false (no cachear nulls)
     */
    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .recordStats()); // Habilitar estadísticas para monitoreo
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    /**
     * Error handler para degradación graceful.
     * Si hay errores de cache, loguea warning pero continúa sin fallar.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return customCacheErrorHandler;
    }
}
