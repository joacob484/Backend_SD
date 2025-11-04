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
 * Configuracion de cache usando Caffeine (in-memory).
 * Caffeine es rapido, simple y no requiere infraestructura externa.
 * Ideal para Cloud Run donde las instancias son efimeras.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {
    
    private final CustomCacheErrorHandler customCacheErrorHandler;
    
    public CacheConfig(CustomCacheErrorHandler customCacheErrorHandler) {
        this.customCacheErrorHandler = customCacheErrorHandler;
    }

    /**
     * ⚡ OPTIMIZADO: Configuracion de Caffeine cache manager.
     * - maximumSize: 15,000 entradas (aumentado para mejor hit rate)
     * - expireAfterWrite: 10 minutos de TTL default
     * - allowNullValues: false (no cachear nulls)
     * - recordStats: true (monitoreo de rendimiento)
     */
    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(15_000)  // ✅ Aumentado de 10k a 15k
            .expireAfterWrite(Duration.ofMinutes(10))
            .recordStats());
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    /**
     * Error handler para degradacion graceful.
     * Si hay errores de cache, loguea warning pero continua sin fallar.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return customCacheErrorHandler;
    }
}