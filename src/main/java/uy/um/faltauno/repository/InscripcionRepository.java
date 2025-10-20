package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.um.faltauno.entity.Inscripcion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID> {
    
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario u " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE u.id = :usuarioId " +
           "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByUsuarioId(@Param("usuarioId") UUID usuarioId);
    
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario u " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE u.id = :usuarioId AND CAST(i.estado AS string) = :estado " +
           "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByUsuarioIdAndEstado(
        @Param("usuarioId") UUID usuarioId, 
        @Param("estado") String estado
    );
    
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario u " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE p.id = :partidoId " +
           "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByPartidoId(@Param("partidoId") UUID partidoId);
    
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario u " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE p.id = :partidoId AND CAST(i.estado AS string) = :estado " +
           "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByPartidoIdAndEstado(
        @Param("partidoId") UUID partidoId, 
        @Param("estado") String estado
    );
    
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario u " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE p.id = :partidoId AND i.estado = 'PENDIENTE' " +
           "ORDER BY i.createdAt ASC")
    List<Inscripcion> findSolicitudesPendientesByPartidoId(@Param("partidoId") UUID partidoId);
    
    @Query("SELECT COUNT(i) FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId AND i.estado = 'ACEPTADO'")
    long countInscripcionesAceptadas(@Param("partidoId") UUID partidoId);
    
    @Query("SELECT COUNT(i) FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId AND i.estado = 'PENDIENTE'")
    long countInscripcionesPendientes(@Param("partidoId") UUID partidoId);
    
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END " +
           "FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId AND i.usuario.id = :usuarioId")
    boolean existeInscripcion(
        @Param("partidoId") UUID partidoId, 
        @Param("usuarioId") UUID usuarioId
    );
    
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END " +
           "FROM Inscripcion i " +
           "WHERE i.partido.id = :partidoId " +
           "AND i.usuario.id = :usuarioId " +
           "AND CAST(i.estado AS string) = :estado")
    boolean existeInscripcionConEstado(
        @Param("partidoId") UUID partidoId, 
        @Param("usuarioId") UUID usuarioId,
        @Param("estado") String estado
    );
    
    @Query("SELECT i FROM Inscripcion i " +
           "LEFT JOIN FETCH i.usuario u " +
           "LEFT JOIN FETCH i.partido p " +
           "WHERE p.id = :partidoId AND u.id = :usuarioId")
    Optional<Inscripcion> findByPartidoIdAndUsuarioId(
        @Param("partidoId") UUID partidoId,
        @Param("usuarioId") UUID usuarioId
    );
    
    List<Inscripcion> findByUsuario_IdAndEstado(UUID usuarioId, String estado);
    List<Inscripcion> findByPartido_IdAndEstado(UUID partidoId, String estado);
}