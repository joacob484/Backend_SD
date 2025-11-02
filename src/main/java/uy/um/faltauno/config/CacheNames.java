package uy.um.faltauno.config;

/**
 * Constantes centralizadas para nombres de caché.
 * 
 * ✅ ARQUITECTURA: Centralizar nombres de caché para evitar inconsistencias
 * y facilitar mantenimiento. Usar estas constantes en lugar de strings literales
 * en @Cacheable, @CacheEvict y @CachePut.
 */
public final class CacheNames {
    
    // Prevenir instanciación
    private CacheNames() {
        throw new UnsupportedOperationException("Esta es una clase de constantes");
    }
    
    // ===== PARTIDOS =====
    public static final String PARTIDOS_V2 = "partidos_v2";
    public static final String PARTIDOS_DISPONIBLES = "partidos-disponibles";
    
    // ===== USUARIOS =====
    public static final String USUARIOS_PUBLICO = "usuarios-publico";
    
    // ===== ESTADÍSTICAS =====
    public static final String COMMUNITY_STATS = "community-stats";
    public static final String SYSTEM_STATS = "system-stats";
    
    // ===== NOTIFICACIONES =====
    public static final String NOTIFICACIONES = "notificaciones";
    
    // ===== REVIEWS =====
    public static final String REVIEWS = "reviews";
    
    // ===== AMISTADES =====
    public static final String AMISTADES = "amistades";
    
    // ===== NOVEDADES =====
    public static final String NOVEDADES_GITHUB = "novedades-github";
    
    /**
     * Array con todos los nombres de caché para configuración.
     * Usar en CacheConfig para crear todos los cachés al iniciar.
     */
    public static final String[] ALL_CACHE_NAMES = {
        PARTIDOS_V2,
        PARTIDOS_DISPONIBLES,
        USUARIOS_PUBLICO,
        COMMUNITY_STATS,
        SYSTEM_STATS,
        NOTIFICACIONES,
        REVIEWS,
        AMISTADES,
        NOVEDADES_GITHUB
    };
}
