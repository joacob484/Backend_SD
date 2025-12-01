package uy.um.faltauno.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Database Index Optimizer
 * 
 * Crea índices en la base de datos para optimizar queries frecuentes
 * Se ejecuta asíncronamente DESPUÉS de que la aplicación está lista para no bloquear el startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseIndexOptimizer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleIndexCreation() {
        createOptimizationIndexesAsync();
    }
    
    @Async
    public void createOptimizationIndexesAsync() {
        log.info("🔧 Optimizando índices de base de datos (async)...");
        
        try {
            // ✅ Índices para Usuario
            createIndexIfNotExists(
                "idx_usuario_email", 
                "usuario", 
                "email", 
                "Búsqueda rápida por email (login)"
            );
            
            createIndexIfNotExists(
                "idx_usuario_cedula", 
                "usuario", 
                "cedula", 
                "Validación de cédula"
            );
            
            createIndexIfNotExists(
                "idx_usuario_activo", 
                "usuario", 
                "deleted_at", 
                "WHERE deleted_at IS NULL",
                "Filtrado de usuarios activos"
            );
            
            // ✅ Índices para Partido
            createIndexIfNotExists(
                "idx_partido_fecha_hora", 
                "partido", 
                "fecha, hora", 
                "Ordenamiento por fecha/hora"
            );
            
            createIndexIfNotExists(
                "idx_partido_estado", 
                "partido", 
                "estado", 
                "Filtrado por estado"
            );
            
            createIndexIfNotExists(
                "idx_partido_creador", 
                "partido", 
                "creador_id", 
                "Partidos creados por usuario"
            );
            
            createIndexIfNotExists(
                "idx_partido_ubicacion", 
                "partido", 
                "latitud, longitud", 
                "Búsqueda geográfica"
            );
            
            // ✅ Índices para Inscripcion
            createIndexIfNotExists(
                "idx_inscripcion_usuario", 
                "inscripcion", 
                "usuario_id", 
                "Inscripciones por usuario"
            );
            
            createIndexIfNotExists(
                "idx_inscripcion_partido", 
                "inscripcion", 
                "partido_id", 
                "Inscripciones por partido"
            );
            
            createIndexIfNotExists(
                "idx_inscripcion_estado", 
                "inscripcion", 
                "estado", 
                "Filtrado por estado de inscripción"
            );
            
            // Índice compuesto para verificar inscripción usuario-partido
            createIndexIfNotExists(
                "idx_inscripcion_usuario_partido", 
                "inscripcion", 
                "usuario_id, partido_id", 
                "Verificación rápida usuario-partido"
            );
            
            // ✅ Índices para Amistad
            createIndexIfNotExists(
                "idx_amistad_usuario", 
                "amistad", 
                "usuario_id", 
                "Amigos de usuario"
            );
            
            createIndexIfNotExists(
                "idx_amistad_amigo", 
                "amistad", 
                "amigo_id", 
                "Búsqueda inversa de amistades"
            );
            
            createIndexIfNotExists(
                "idx_amistad_estado", 
                "amistad", 
                "estado", 
                "Filtrado por estado de amistad"
            );
            
            // ✅ Índices para Mensaje
            createIndexIfNotExists(
                "idx_mensaje_partido", 
                "mensaje", 
                "partido_id", 
                "Mensajes de un partido"
            );
            
            createIndexIfNotExists(
                "idx_mensaje_usuario", 
                "mensaje", 
                "usuario_id", 
                "Mensajes de un usuario"
            );
            
            createIndexIfNotExists(
                "idx_mensaje_timestamp", 
                "mensaje", 
                "timestamp DESC", 
                "Ordenamiento por fecha (más recientes primero)"
            );
            
            // ✅ Índices para Notificacion
            createIndexIfNotExists(
                "idx_notificacion_usuario", 
                "notificacion", 
                "usuario_id", 
                "Notificaciones por usuario"
            );
            
            createIndexIfNotExists(
                "idx_notificacion_leida", 
                "notificacion", 
                "leida", 
                "WHERE leida = FALSE",
                "Notificaciones no leídas"
            );
            
            createIndexIfNotExists(
                "idx_notificacion_timestamp", 
                "notificacion", 
                "timestamp DESC", 
                "Ordenamiento por fecha"
            );
            
            // ✅ Índices para Review
            createIndexIfNotExists(
                "idx_review_evaluado", 
                "review", 
                "evaluado_id", 
                "Reviews recibidas por usuario"
            );
            
            createIndexIfNotExists(
                "idx_review_evaluador", 
                "review", 
                "evaluador_id", 
                "Reviews dadas por usuario"
            );
            
            createIndexIfNotExists(
                "idx_review_partido", 
                "review", 
                "partido_id", 
                "Reviews de un partido"
            );
            
            log.info("✅ Índices de base de datos optimizados correctamente");
            
        } catch (Exception e) {
            log.error("❌ Error optimizando índices: {}", e.getMessage());
            // No lanzar excepción para no bloquear el startup
        }
    }
    
    private void createIndexIfNotExists(String indexName, String tableName, String columns, String description) {
        createIndexIfNotExists(indexName, tableName, columns, null, description);
    }
    
    private void createIndexIfNotExists(String indexName, String tableName, String columns, String whereClause, String description) {
        try {
            String sql = String.format(
                "CREATE INDEX IF NOT EXISTS %s ON %s (%s)%s",
                indexName,
                tableName,
                columns,
                whereClause != null ? " " + whereClause : ""
            );
            
            jdbcTemplate.execute(sql);
            log.debug("✓ Índice creado: {} - {}", indexName, description);
            
        } catch (Exception e) {
            log.warn("⚠️ No se pudo crear índice {}: {}", indexName, e.getMessage());
        }
    }
}
