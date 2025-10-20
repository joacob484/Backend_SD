package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.um.faltauno.entity.Inscripcion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID> {
    
    // ============================================
    // BÚSQUEDAS POR USUARIO
    // ============================================
    
    /**
     * Buscar TODAS las inscripciones de un usuario (sin filtro de estado)
     */
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario u " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE u.id = :usuarioId " +
           "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByUsuarioId(@Param("usuarioId") UUID usuarioId);
    
    /**
     * Buscar inscripciones de un usuario filtradas por estado
     */
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario u " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE u.id = :usuarioId AND i.estado = :estado " +
           "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByUsuarioIdAndEstado(
        @Param("usuarioId") UUID usuarioId, 
        @Param("estado") String estado
    );
    
    // ============================================
    // BÚSQUEDAS POR PARTIDO
    // ============================================
    
    /**
     * Buscar TODAS las inscripciones de un partido (sin filtro de estado)
     * IMPORTANTE: Incluye PENDIENTE, ACEPTADO y RECHAZADO
     */
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario u " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE p.id = :partidoId " +
           "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByPartidoId(@Param("partidoId") UUID partidoId);
    
    /**
     * Buscar inscripciones de un partido filtradas por estado
     */
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario u " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE p.id = :partidoId AND i.estado = :estado " +
           "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByPartidoIdAndEstado(
        @Param("partidoId") UUID partidoId, 
        @Param("estado") String estado
    );
    
    // ============================================
    // CONTEOS
    // ============================================
    
    /**
     * Contar inscripciones ACEPTADAS de un partido
     * CRÍTICO: Solo cuenta los jugadores confirmados
     */
    @Query("SELECT COUNT(i) FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId AND i.estado = 'ACEPTADO'")
    long countInscripcionesAceptadas(@Param("partidoId") UUID partidoId);
    
    /**
     * Contar inscripciones PENDIENTES de un partido
     */
    @Query("SELECT COUNT(i) FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId AND i.estado = 'PENDIENTE'")
    long countInscripcionesPendientes(@Param("partidoId") UUID partidoId);
    
    // ============================================
    // VERIFICACIONES
    // ============================================
    
    /**
     * Verificar si existe CUALQUIER inscripción entre usuario y partido
     * (sin importar el estado)
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END " +
           "FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId AND i.usuario.id = :usuarioId")
    boolean existeInscripcion(
        @Param("partidoId") UUID partidoId, 
        @Param("usuarioId") UUID usuarioId
    );
    
    /**
     * Verificar si existe inscripción en estado específico
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END " +
           "FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId " +
           "AND i.usuario.id = :usuarioId " +
           "AND i.estado = :estado")
    boolean existeInscripcionConEstado(
        @Param("partidoId") UUID partidoId, 
        @Param("usuarioId") UUID usuarioId,
        @Param("estado") String estado
    );
    
    /**
     * Buscar inscripción específica entre usuario y partido
     */
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario u " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE p.id = :partidoId AND u.id = :usuarioId")
    Optional<Inscripcion> findByPartidoIdAndUsuarioId(
        @Param("partidoId") UUID partidoId,
        @Param("usuarioId") UUID usuarioId
    );
    
    // ============================================
    // MÉTODOS DE CONVENIENCIA (usando nombrado)
    // ============================================
    
    /**
     * ALTERNATIVA: Usar naming convention de Spring Data JPA
     * Estos métodos son equivalentes a los de arriba pero usando
     * el sistema de nombres automático
     */
    List<Inscripcion> findByUsuario_IdAndEstado(UUID usuarioId, String estado);
    List<Inscripcion> findByPartido_IdAndEstado(UUID partidoId, String estado);
}