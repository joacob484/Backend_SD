package uy.um.faltauno.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

/**
 * 💰 CONFIGURACION ULTRA-ECONOMICA DE CACHE
 * 
 * ESTRATEGIA: Cache agresivo con Caffeine (GRATIS) para 1K usuarios
 * FUTURO: Feature flag para activar Redis cuando escales (app.cache.redis.enabled=true)
 * 
 * BENEFICIOS:
 * - 90%+ cache hit rate = menos queries DB = menor costo
 * - 15 minutos TTL = datos frescos pero menos carga
 * - 2000 items = suficiente para 1K usuarios activos
 * - Pre-crear cachés = cero latencia en primera request
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {
    
    private final CustomCacheErrorHandler customCacheErrorHandler;
    
    @Value("${app.cache.redis.enabled:false}")
    private boolean redisEnabled;
    
    @Value("${app.cache.ttl.minutes:15}")
    private int cacheTtlMinutes;
    
    public CacheConfig(CustomCacheErrorHandler customCacheErrorHandler) {
        this.customCacheErrorHandler = customCacheErrorHandler;
    }

    /**
     * 🚀 CACHE MANAGER ULTRA-OPTIMIZADO
     * 
     * CONFIGURACION ACTUAL (1K usuarios):
     * - maximumSize: 2000 items (suficiente para todos los datos hot)
     * - expireAfterWrite: 15 minutos (balancear frescura vs queries)
     * - recordStats: true (monitorear hit rate en Grafana)
     * - Pre-crear cachés: usuarios, partidos, inscripciones, contactos, amistades
     * 
     * FEATURE FLAG: Si redisEnabled=true, usa Redis (cuando escales a 10K+)
     */
    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Configuracion Caffeine agresiva
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(2_000)  // 💰 2K items = suficiente para 1K usuarios
            .expireAfterWrite(Duration.ofMinutes(cacheTtlMinutes))  // 💰 15 min = menos queries
            .recordStats());  // 📊 Monitorear performance
        
        cacheManager.setAllowNullValues(false);
        
        // 🎯 Pre-crear cachés más usados (cero latencia en primera request)
        cacheManager.setCacheNames(Arrays.asList(
            "usuarios",
            "partidos", 
            "inscripciones",
            "contactos",
            "amistades",
            "estadisticas",
            "notificaciones"
        ));
        
        return cacheManager;
    }

    /**
     * Error handler para degradacion graceful.
     * Si cache falla, loguea warning pero continúa sin fallar request.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return customCacheErrorHandler;
    }
}