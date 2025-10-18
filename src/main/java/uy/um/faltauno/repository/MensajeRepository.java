package uy.um.faltauno.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.um.faltauno.entity.Mensaje;

import java.util.List;
import java.util.UUID;

public interface MensajeRepository extends JpaRepository<Mensaje, UUID> {
    
    /**
     * Buscar mensajes de un partido con paginación
     */
    @Query("SELECT m FROM Mensaje m WHERE m.partidoId = :partidoId ORDER BY m.createdAt DESC")
    List<Mensaje> findByPartidoId(@Param("partidoId") UUID partidoId, Pageable pageable);
    
    /**
     * Buscar todos los mensajes de un partido
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
     * Eliminar todos los mensajes de un partido
     */
    void deleteByPartidoId(UUID partidoId);
}