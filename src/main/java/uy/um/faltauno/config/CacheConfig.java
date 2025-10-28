package uy.um.faltauno.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import org.springframework.lang.Nullable;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("10.217.135.172")
    private String redisHost;

    @Value("6379")
    private int redisPort;

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(ObjectMapper baseMapper) {
        ObjectMapper om = baseMapper.copy()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Enable polymorphic typing so cached objects include type information
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("uy.um.faltauno")
            // Allow a few JDK packages that may appear as type ids (e.g. BigDecimal)
            .allowIfSubType("java.math")
            .allowIfSubType("java.util")
            .allowIfSubType("java.lang")
            .build();
        om.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer valueSer = new GenericJackson2JsonRedisSerializer(om);

        return RedisCacheConfiguration.defaultCacheConfig()
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(valueSer)
        );
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-8601
        return om;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
    ObjectMapper mapper = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    // Enable polymorphic typing so cached objects include type information
    BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
        .allowIfSubType("uy.um.faltauno")
        // Allow common JDK packages used inside cached structures
        .allowIfSubType("java.math")
        .allowIfSubType("java.util")
        .allowIfSubType("java.lang")
        .build();
    mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

    GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

    RedisCacheConfiguration cfg = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(serializer)
        );

    return RedisCacheManager.builder(cf).cacheDefaults(cfg).build();
    }


    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        Logger log = LoggerFactory.getLogger("CacheErrorHandler");
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException ex, Cache cache, @Nullable Object key) {
                log.warn("Cache GET falló en {} para key {}: {}", cache.getName(), key, ex.toString());
            }

            @Override
            public void handleCachePutError(RuntimeException ex, Cache cache, @Nullable Object key, @Nullable Object value) {
                log.warn("Cache PUT falló en {} para key {}: {}", cache.getName(), key, ex.toString());
            }

            @Override
            public void handleCacheEvictError(RuntimeException ex, Cache cache, @Nullable Object key) {
                log.warn("Cache EVICT falló en {} para key {}: {}", cache.getName(), key, ex.toString());
            }

            @Override
            public void handleCacheClearError(RuntimeException ex, Cache cache) {
                log.warn("Cache CLEAR falló en {}: {}", cache.getName(), ex.toString());
            }
        };
    }
}