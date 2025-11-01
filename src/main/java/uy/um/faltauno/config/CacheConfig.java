package uy.um.faltauno.config;package uy.um.faltauno.config;



import com.github.benmanes.caffeine.cache.Caffeine;import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.cache.annotation.CachingConfigurer;import org.springframework.cache.annotation.CachingConfigurer;

import org.springframework.cache.annotation.EnableCaching;import org.springframework.cache.annotation.EnableCaching;

import org.springframework.cache.caffeine.CaffeineCacheManager;import org.springframework.cache.caffeine.CaffeineCacheManager;

import org.springframework.cache.interceptor.CacheErrorHandler;import org.springframework.cache.interceptor.CacheErrorHandler;

import org.springframework.context.annotation.Bean;import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;import org.springframework.context.annotation.Configuration;



import java.time.Duration;import java.time.Duration;



/**/**

 * Configuracion de cache usando Caffeine (in-memory). * ConfiguraciÃ³n de cache usando Caffeine (in-memory).

 * Caffeine es rapido, simple y no requiere infraestructura externa. * Caffeine es rÃ¡pido, simple y no requiere infraestructura externa.

 * Ideal para Cloud Run donde las instancias son efimeras. * Ideal para Cloud Run donde las instancias son efÃ­meras.

 */ */

@Configuration@Configuration

@EnableCaching@EnableCaching

public class CacheConfig implements CachingConfigurer {public class CacheConfig implements CachingConfigurer {

        

    private final CustomCacheErrorHandler customCacheErrorHandler;    private final CustomCacheErrorHandler customCacheErrorHandler;

        

    public CacheConfig(CustomCacheErrorHandler customCacheErrorHandler) {    public CacheConfig(CustomCacheErrorHandler customCacheErrorHandler) {

        this.customCacheErrorHandler = customCacheErrorHandler;        this.customCacheErrorHandler = customCacheErrorHandler;

    }    }



    /**    /**

     * Configuracion de Caffeine cache manager.     * ConfiguraciÃ³n de Caffeine cache manager.

     * - maximumSize: 10,000 entradas (ajustar segun memoria disponible)     * - maximumSize: 10,000 entradas (ajustar segÃºn memoria disponible)

     * - expireAfterWrite: 10 minutos de TTL     * - expireAfterWrite: 10 minutos de TTL

     * - allowNullValues: false (no cachear nulls)     * - allowNullValues: false (no cachear nulls)

     */     */

    @Bean    @Bean

    public CaffeineCacheManager cacheManager() {    public CaffeineCacheManager cacheManager() {

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.setCaffeine(Caffeine.newBuilder()        cacheManager.setCaffeine(Caffeine.newBuilder()

            .maximumSize(10_000)            .maximumSize(10_000)

            .expireAfterWrite(Duration.ofMinutes(10))            .expireAfterWrite(Duration.ofMinutes(10))

            .recordStats()); // Habilitar estadisticas para monitoreo            .recordStats()); // Habilitar estadÃ­sticas para monitoreo

        cacheManager.setAllowNullValues(false);        cacheManager.setAllowNullValues(false);

        return cacheManager;        return cacheManager;

    }    }



    /**    /**

     * Error handler para degradacion graceful.     * Error handler para degradaciÃ³n graceful.

     * Si hay errores de cache, loguea warning pero continua sin fallar.     * Si hay errores de cache, loguea warning pero continÃºa sin fallar.

     */     */

    @Override    @Override

    public CacheErrorHandler errorHandler() {    public CacheErrorHandler errorHandler() {

        return customCacheErrorHandler;        return customCacheErrorHandler;

    }    }

}}

