package uy.um.faltauno.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uy.um.faltauno.entity.Mensaje;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, UUID> {
    
    /**
     * Buscar mensajes de un partido con paginación
     */
    @Query("SELECT m FROM Mensaje m WHERE m.partidoId = :partidoId ORDER BY m.createdAt DESC")
    List<Mensaje> findByPartidoIdOrderByCreatedAtDesc(
            @Param("partidoId") UUID partidoId, 
            Pageable pageable);
    
    /**
     * Buscar todos los mensajes de un partido (sin paginación)
     */
    List<Mensaje> findByPartidoIdOrderByCreatedAtAsc(UUID partidoId);
    
    /**
     * Buscar mensajes no leídos por destinatario
     */
    List<Mensaje> findByDestinatarioIdAndLeido(UUID destinatarioId, Boolean leido);
    
    /**
     * Buscar mensajes no leídos de un partido para un usuario
     */
    @Query("SELECT m FROM Mensaje m WHERE m.partidoId = :partidoId " +
           "AND m.destinatarioId = :destinatarioId AND m.leido = :leido")
    List<Mensaje> findByPartidoIdAndDestinatarioIdAndLeido(
            @Param("partidoId") UUID partidoId,
            @Param("destinatarioId") UUID destinatarioId,
            @Param("leido") Boolean leido);
    
    /**
     * Contar mensajes no leídos de un partido para un usuario
     */
    @Query("SELECT COUNT(m) FROM Mensaje m WHERE m.partidoId = :partidoId " +
           "AND m.destinatarioId = :destinatarioId AND m.leido = false")
    long countMensajesNoLeidosPorPartido(
            @Param("partidoId") UUID partidoId,
            @Param("destinatarioId") UUID destinatarioId);
    
    /**
     * Marcar todos los mensajes de un partido como leídos para un usuario
     */
    @Modifying
    @Query("UPDATE Mensaje m SET m.leido = true " +
           "WHERE m.partidoId = :partidoId AND m.destinatarioId = :usuarioId AND m.leido = false")
    int marcarMensajesComoLeidos(
            @Param("partidoId") UUID partidoId,
            @Param("usuarioId") UUID usuarioId);
    
    /**
     * Eliminar todos los mensajes de un partido
     */
    @Modifying
    @Query("DELETE FROM Mensaje m WHERE m.partidoId = :partidoId")
    void deleteByPartidoId(@Param("partidoId") UUID partidoId);
    
    /**
     * Buscar mensajes grupales de un partido (sin destinatario específico)
     */
    @Query("SELECT m FROM Mensaje m WHERE m.partidoId = :partidoId " +
           "AND m.destinatarioId IS NULL ORDER BY m.createdAt ASC")
    List<Mensaje> findMensajesGrupalesByPartidoId(@Param("partidoId") UUID partidoId);
    
    /**
     * Buscar mensajes directos entre dos usuarios
     */
    @Query("SELECT m FROM Mensaje m WHERE " +
           "((m.remitenteId = :usuario1 AND m.destinatarioId = :usuario2) OR " +
           "(m.remitenteId = :usuario2 AND m.destinatarioId = :usuario1)) " +
           "ORDER BY m.createdAt ASC")
    List<Mensaje> findMensajesDirectosEntreUsuarios(
            @Param("usuario1") UUID usuario1,
            @Param("usuario2") UUID usuario2);
    
    /**
     * Contar total de mensajes de un partido
     */
    long countByPartidoId(UUID partidoId);
    
    /**
     * Contar mensajes de un partido después de cierta fecha, excluyendo los de un usuario
     * Usado para calcular mensajes no leídos
     */
    @Query("SELECT COUNT(m) FROM Mensaje m WHERE m.partidoId = :partidoId " +
           "AND m.createdAt > :cutoffTime AND m.remitenteId != :excludeUserId")
    long countByPartidoIdAndCreatedAtAfterAndRemitenteIdNot(
            @Param("partidoId") UUID partidoId,
            @Param("cutoffTime") Instant cutoffTime,
            @Param("excludeUserId") UUID excludeUserId);
}
```