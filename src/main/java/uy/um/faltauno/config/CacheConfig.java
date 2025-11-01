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
 * ConfiguraciÃ³n de cache usando Caffeine (in-memory).
 * Caffeine es rÃ¡pido, simple y no requiere infraestructura externa.
 * Ideal para Cloud Run donde las instancias son efÃ­meras.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {
    
    private final CustomCacheErrorHandler customCacheErrorHandler;
    
    public CacheConfig(CustomCacheErrorHandler customCacheErrorHandler) {
        this.customCacheErrorHandler = customCacheErrorHandler;
    }

    /**
     * ConfiguraciÃ³n de Caffeine cache manager.
     * - maximumSize: 10,000 entradas (ajustar segÃºn memoria disponible)
     * - expireAfterWrite: 10 minutos de TTL
     * - allowNullValues: false (no cachear nulls)
     */
    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .recordStats()); // Habilitar estadÃ­sticas para monitoreo
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    /**
     * Error handler para degradaciÃ³n graceful.
     * Si hay errores de cache, loguea warning pero continÃºa sin fallar.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return customCacheErrorHandler;
    }
}
