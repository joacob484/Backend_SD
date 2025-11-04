package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uy.um.faltauno.entity.SolicitudPartido;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SolicitudPartidoRepository extends JpaRepository<SolicitudPartido, UUID> {

    /**
     * Buscar solicitudes de un partido específico (ordenadas por fecha)
     */
    @Query("SELECT s FROM SolicitudPartido s " +
           "WHERE s.partido.id = :partidoId " +
           "ORDER BY s.createdAt ASC")
    List<SolicitudPartido> findByPartidoId(@Param("partidoId") UUID partidoId);

    /**
     * Buscar solicitudes de un usuario
     */
    @Query("SELECT s FROM SolicitudPartido s " +
           "WHERE s.usuario.id = :usuarioId " +
           "ORDER BY s.createdAt DESC")
    List<SolicitudPartido> findByUsuarioId(@Param("usuarioId") UUID usuarioId);

    /**
     * Buscar solicitud específica de un usuario en un partido
     */
    @Query("SELECT s FROM SolicitudPartido s " +
           "WHERE s.partido.id = :partidoId AND s.usuario.id = :usuarioId")
    Optional<SolicitudPartido> findByPartidoIdAndUsuarioId(
        @Param("partidoId") UUID partidoId,
        @Param("usuarioId") UUID usuarioId
    );

    /**
     * Verificar si existe solicitud
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SolicitudPartido s " +
           "WHERE s.partido.id = :partidoId AND s.usuario.id = :usuarioId")
    boolean existsByPartidoIdAndUsuarioId(
        @Param("partidoId") UUID partidoId,
        @Param("usuarioId") UUID usuarioId
    );

    /**
     * Contar solicitudes de un partido
     */
    @Query("SELECT COUNT(s) FROM SolicitudPartido s WHERE s.partido.id = :partidoId")
    long countByPartidoId(@Param("partidoId") UUID partidoId);
}
