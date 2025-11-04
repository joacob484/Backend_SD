package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.um.faltauno.entity.Inscripcion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para Inscripcion (solo usuarios ACEPTADOS).
 * Las solicitudes pendientes están en SolicitudPartidoRepository.
 */
public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID> {
    
    /**
     * Listar inscripciones de un usuario (todas aceptadas)
     */
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario u " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE u.id = :usuarioId " +
           "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByUsuarioId(@Param("usuarioId") UUID usuarioId);
    
    /**
     * Listar inscripciones de un partido (todas aceptadas)
     */
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario u " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE p.id = :partidoId " +
           "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByPartidoId(@Param("partidoId") UUID partidoId);
    
    /**
     * Contar inscripciones de un partido
     */
    @Query("SELECT COUNT(i) FROM Inscripcion i WHERE i.partido.id = :partidoId")
    long countByPartidoId(@Param("partidoId") UUID partidoId);
    
    /**
     * Buscar inscripción específica
     */
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario u " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE p.id = :partidoId AND u.id = :usuarioId")
    Optional<Inscripcion> findByPartidoIdAndUsuarioId(
        @Param("partidoId") UUID partidoId,
        @Param("usuarioId") UUID usuarioId
    );
    
    /**
     * Verificar si existe inscripción
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END " +
           "FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId AND i.usuario.id = :usuarioId")
    boolean existeInscripcion(
        @Param("partidoId") UUID partidoId, 
        @Param("usuarioId") UUID usuarioId
    );
    
    /**
     * Contar usuarios únicos que participaron desde una fecha
     */
    @Query("SELECT COUNT(DISTINCT i.usuario.id) FROM Inscripcion i " +
           "WHERE i.partido.fecha >= :fechaDesde")
    long countDistinctUsuariosActivosDesde(@Param("fechaDesde") java.time.LocalDate fechaDesde);
}