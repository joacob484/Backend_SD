package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.um.faltauno.entity.Amistad;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AmistadRepository extends JpaRepository<Amistad, UUID> {
    
    /**
     * Buscar solicitudes de amistad pendientes recibidas por un usuario
     */
    @Query("SELECT a FROM Amistad a WHERE a.amigoId = :amigoId AND a.estado = :estado")
    List<Amistad> findByAmigoIdAndEstado(
        @Param("amigoId") UUID amigoId, 
        @Param("estado") String estado
    );
    
    /**
     * Buscar solicitudes de amistad enviadas por un usuario
     */
    @Query("SELECT a FROM Amistad a WHERE a.usuarioId = :usuarioId AND a.estado = :estado")
    List<Amistad> findByUsuarioIdAndEstado(
        @Param("usuarioId") UUID usuarioId, 
        @Param("estado") String estado
    );
    
    /**
     * Buscar todas las amistades de un usuario (tanto enviadas como recibidas)
     */
    @Query("SELECT a FROM Amistad a WHERE (a.usuarioId = :usuarioId OR a.amigoId = :usuarioId) AND a.estado = 'ACEPTADO'")
    List<Amistad> findAmigosByUsuarioId(@Param("usuarioId") UUID usuarioId);
    
    /**
     * Verificar si existe una amistad entre dos usuarios (en cualquier dirección)
     */
    @Query("SELECT a FROM Amistad a WHERE " +
           "(a.usuarioId = :usuarioId AND a.amigoId = :amigoId) OR " +
           "(a.usuarioId = :amigoId AND a.amigoId = :usuarioId)")
    Optional<Amistad> findAmistadEntreUsuarios(
        @Param("usuarioId") UUID usuarioId, 
        @Param("amigoId") UUID amigoId
    );
    
    /**
     * Verificar si existe una solicitud pendiente entre dos usuarios
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Amistad a WHERE " +
           "((a.usuarioId = :usuarioId AND a.amigoId = :amigoId) OR " +
           "(a.usuarioId = :amigoId AND a.amigoId = :usuarioId)) AND " +
           "a.estado = 'PENDIENTE'")
    boolean existeSolicitudPendiente(
        @Param("usuarioId") UUID usuarioId, 
        @Param("amigoId") UUID amigoId
    );
    
    /**
     * Verificar si dos usuarios ya son amigos
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Amistad a WHERE " +
           "((a.usuarioId = :usuarioId AND a.amigoId = :amigoId) OR " +
           "(a.usuarioId = :amigoId AND a.amigoId = :usuarioId)) AND " +
           "a.estado = 'ACEPTADO'")
    boolean sonAmigos(
        @Param("usuarioId") UUID usuarioId, 
        @Param("amigoId") UUID amigoId
    );
    
    /**
     * Contar amigos de un usuario
     */
    @Query("SELECT COUNT(a) FROM Amistad a WHERE " +
           "(a.usuarioId = :usuarioId OR a.amigoId = :usuarioId) AND " +
           "a.estado = 'ACEPTADO'")
    long countAmigosByUsuarioId(@Param("usuarioId") UUID usuarioId);
    
    /**
     * Contar solicitudes pendientes recibidas
     */
    @Query("SELECT COUNT(a) FROM Amistad a WHERE a.amigoId = :usuarioId AND a.estado = 'PENDIENTE'")
    long countSolicitudesPendientes(@Param("usuarioId") UUID usuarioId);
}