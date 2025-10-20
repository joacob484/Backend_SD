package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uy.um.faltauno.entity.Inscripcion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para gestión de inscripciones
 * Incluye queries optimizados y métodos de estadísticas
 */
@Repository
public interface InscripcionRepository extends 
        JpaRepository<Inscripcion, UUID>, 
        JpaSpecificationExecutor<Inscripcion> {
    
    // ==============================================
    // BÚSQUEDAS BÁSICAS
    // ==============================================
    
    /**
     * Buscar inscripción por partido y usuario
     */
    @Query("SELECT i FROM Inscripcion i WHERE i.partido.id = :partidoId AND i.usuario.id = :usuarioId")
    Optional<Inscripcion> findByPartidoIdAndUsuarioId(
            @Param("partidoId") UUID partidoId, 
            @Param("usuarioId") UUID usuarioId
    );
    
    /**
     * Buscar inscripciones por usuario
     */
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.partido p " +
           "LEFT JOIN FETCH p.organizador " +
           "WHERE i.usuario.id = :usuarioId " +
           "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByUsuarioId(@Param("usuarioId") UUID usuarioId);
    
    /**
     * Buscar inscripciones por usuario y estado (Spring Data JPA naming)
     */
    List<Inscripcion> findByUsuario_IdAndEstado(UUID usuarioId, Inscripcion.EstadoInscripcion estado);
    
    /**
     * Buscar inscripciones por usuario y estado (con JOIN FETCH optimizado)
     */
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.partido p " +
           "LEFT JOIN FETCH p.organizador " +
           "WHERE i.usuario.id = :usuarioId AND i.estado = :estado " +
           "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByUsuarioIdAndEstado(
            @Param("usuarioId") UUID usuarioId,
            @Param("estado") Inscripcion.EstadoInscripcion estado
    );
    
    /**
     * Buscar inscripciones por partido
     */
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario " +
           "WHERE i.partido.id = :partidoId " +
           "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByPartidoId(@Param("partidoId") UUID partidoId);
    
    /**
     * Buscar inscripciones por partido y estado (Spring Data JPA naming)
     */
    List<Inscripcion> findByPartido_IdAndEstado(UUID partidoId, Inscripcion.EstadoInscripcion estado);
    
    /**
     * Buscar inscripciones por partido y estado (con JOIN FETCH optimizado)
     */
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario " +
           "WHERE i.partido.id = :partidoId AND i.estado = :estado " +
           "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByPartidoIdAndEstado(
            @Param("partidoId") UUID partidoId,
            @Param("estado") Inscripcion.EstadoInscripcion estado
    );
    
    // ==============================================
    // BÚSQUEDAS CON JOIN FETCH (optimizadas)
    // ==============================================
    
    /**
     * Buscar inscripciones activas de un usuario con toda la info necesaria
     */
    @Query("SELECT DISTINCT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.partido p " +
           "LEFT JOIN FETCH p.organizador " +
           "WHERE i.usuario.id = :usuarioId " +
           "AND i.estado IN ('PENDIENTE', 'ACEPTADO') " +
           "ORDER BY p.fecha DESC, p.hora DESC")
    List<Inscripcion> findInscripcionesActivasByUsuarioId(@Param("usuarioId") UUID usuarioId);
    
    /**
     * Buscar solicitudes pendientes de un partido con info del usuario
     */
    @Query("SELECT DISTINCT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario u " +
           "WHERE i.partido.id = :partidoId " +
           "AND i.estado = 'PENDIENTE' " +
           "ORDER BY i.createdAt ASC")
    List<Inscripcion> findSolicitudesPendientesByPartidoId(@Param("partidoId") UUID partidoId);
    
    // ==============================================
    // CONTADORES Y ESTADÍSTICAS
    // ==============================================
    
    /**
     * Contar inscripciones aceptadas de un partido
     */
    @Query("SELECT COUNT(i) FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId AND i.estado = 'ACEPTADO'")
    long countInscripcionesAceptadas(@Param("partidoId") UUID partidoId);
    
    /**
     * Contar inscripciones pendientes de un partido
     */
    @Query("SELECT COUNT(i) FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId AND i.estado = 'PENDIENTE'")
    long countInscripcionesPendientes(@Param("partidoId") UUID partidoId);
    
    /**
     * Contar inscripciones activas (PENDIENTE o ACEPTADO) de un partido
     */
    @Query("SELECT COUNT(i) FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId " +
           "AND i.estado IN ('PENDIENTE', 'ACEPTADO')")
    long countInscripcionesActivas(@Param("partidoId") UUID partidoId);
    
    /**
     * Contar inscripciones de un usuario por estado
     */
    @Query("SELECT i.estado, COUNT(i) FROM Inscripcion i " +
           "WHERE i.usuario.id = :usuarioId " +
           "GROUP BY i.estado")
    List<Object[]> countInscripcionesByUsuarioAndEstado(@Param("usuarioId") UUID usuarioId);
    
    // ==============================================
    // VERIFICACIONES DE EXISTENCIA
    // ==============================================
    
    /**
     * Verificar si existe una inscripción
     */
    @Query("SELECT COUNT(i) > 0 FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId AND i.usuario.id = :usuarioId")
    boolean existeInscripcion(
            @Param("partidoId") UUID partidoId, 
            @Param("usuarioId") UUID usuarioId
    );
    
    /**
     * Verificar si existe una inscripción activa
     */
    @Query("SELECT COUNT(i) > 0 FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId " +
           "AND i.usuario.id = :usuarioId " +
           "AND i.estado IN ('PENDIENTE', 'ACEPTADO')")
    boolean existeInscripcionActiva(
            @Param("partidoId") UUID partidoId, 
            @Param("usuarioId") UUID usuarioId
    );
    
    /**
     * Verificar si un usuario está aceptado en un partido
     */
    @Query("SELECT COUNT(i) > 0 FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId " +
           "AND i.usuario.id = :usuarioId " +
           "AND i.estado = 'ACEPTADO'")
    boolean usuarioAceptadoEnPartido(
            @Param("partidoId") UUID partidoId, 
            @Param("usuarioId") UUID usuarioId
    );
    
    // ==============================================
    // BÚSQUEDAS AVANZADAS
    // ==============================================
    
    /**
     * Buscar inscripciones antiguas sin respuesta (PENDIENTE > X días)
     */
    @Query("SELECT i FROM Inscripcion i " +
           "WHERE i.estado = 'PENDIENTE' " +
           "AND i.createdAt < :fecha " +
           "ORDER BY i.createdAt ASC")
    List<Inscripcion> findInscripcionesPendientesAntiguas(@Param("fecha") Instant fecha);
    
    /**
     * Buscar inscripciones de un usuario en partidos futuros
     */
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE i.usuario.id = :usuarioId " +
           "AND p.fecha >= CURRENT_DATE " +
           "AND i.estado IN ('PENDIENTE', 'ACEPTADO') " +
           "ORDER BY p.fecha ASC, p.hora ASC")
    List<Inscripcion> findInscripcionesFuturasByUsuarioId(@Param("usuarioId") UUID usuarioId);
    
    /**
     * Buscar inscripciones de un usuario en partidos pasados
     */
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE i.usuario.id = :usuarioId " +
           "AND p.fecha < CURRENT_DATE " +
           "AND i.estado = 'ACEPTADO' " +
           "ORDER BY p.fecha DESC, p.hora DESC")
    List<Inscripcion> findInscripcionesPasadasByUsuarioId(@Param("usuarioId") UUID usuarioId);
    
    /**
     * Buscar inscripciones de partidos de un organizador
     */
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE p.organizador.id = :organizadorId " +
           "AND i.estado = 'PENDIENTE' " +
           "ORDER BY i.createdAt DESC")
    List<Inscripcion> findSolicitudesPendientesByOrganizadorId(
            @Param("organizadorId") UUID organizadorId
    );
    
    // ==============================================
    // OPERACIONES MASIVAS
    // ==============================================
    
    /**
     * Cancelar todas las inscripciones pendientes de un partido
     * (útil cuando se cancela un partido)
     */
    @Modifying
    @Query("UPDATE Inscripcion i SET i.estado = 'CANCELADO', " +
           "i.fechaCancelacion = CURRENT_TIMESTAMP, " +
           "i.motivoRechazo = :motivo " +
           "WHERE i.partido.id = :partidoId " +
           "AND i.estado IN ('PENDIENTE', 'ACEPTADO')")
    int cancelarInscripcionesDePartido(
            @Param("partidoId") UUID partidoId,
            @Param("motivo") String motivo
    );
    
    /**
     * Rechazar automáticamente solicitudes antiguas
     */
    @Modifying
    @Query("UPDATE Inscripcion i SET i.estado = 'RECHAZADO', " +
           "i.fechaRechazo = CURRENT_TIMESTAMP, " +
           "i.motivoRechazo = 'Solicitud expirada' " +
           "WHERE i.estado = 'PENDIENTE' " +
           "AND i.createdAt < :fecha")
    int rechazarSolicitudesAntiguas(@Param("fecha") Instant fecha);
    
    /**
     * Eliminar inscripciones rechazadas/canceladas antiguas (limpieza)
     */
    @Modifying
    @Query("DELETE FROM Inscripcion i " +
           "WHERE i.estado IN ('RECHAZADO', 'CANCELADO') " +
           "AND i.updatedAt < :fecha")
    int limpiarInscripcionesAntiguas(@Param("fecha") Instant fecha);
    
    // ==============================================
    // PROYECCIONES PARA OPTIMIZACIÓN
    // ==============================================
    
    /**
     * Proyección ligera para listar inscripciones sin cargar entidades completas
     */
    interface InscripcionProjection {
        UUID getId();
        Inscripcion.EstadoInscripcion getEstado();
        String getComentario();
        Instant getCreatedAt();
        UUID getUsuarioId();
        String getUsuarioNombre();
        String getUsuarioApellido();
    }
    
    /**
     * Obtener proyección ligera de inscripciones de un partido
     */
    @Query("SELECT i.id as id, i.estado as estado, i.comentario as comentario, " +
           "i.createdAt as createdAt, i.usuario.id as usuarioId, " +
           "i.usuario.nombre as usuarioNombre, i.usuario.apellido as usuarioApellido " +
           "FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId " +
           "ORDER BY i.createdAt DESC")
    List<InscripcionProjection> findInscripcionesProjectionByPartidoId(
            @Param("partidoId") UUID partidoId
    );
}