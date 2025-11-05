package uy.um.faltauno.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uy.um.faltauno.entity.Notificacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    /**
     * ✅ OPTIMIZACIÓN: Obtener notificaciones paginadas
     * Mejora performance cuando el usuario tiene muchas notificaciones
     */
    @Query("SELECT n FROM Notificacion n WHERE n.usuarioId = :usuarioId ORDER BY n.createdAt DESC")
    Page<Notificacion> findByUsuarioIdPaginated(@Param("usuarioId") UUID usuarioId, Pageable pageable);

    /**
     * Obtener todas las notificaciones de un usuario, ordenadas por fecha descendente
     * DEPRECADO: Usar findByUsuarioIdPaginated para mejor performance
     */
    @Query("SELECT n FROM Notificacion n WHERE n.usuarioId = :usuarioId ORDER BY n.createdAt DESC")
    List<Notificacion> findByUsuarioIdOrderByCreatedAtDesc(@Param("usuarioId") UUID usuarioId);

    /**
     * ✅ OPTIMIZACIÓN: Obtener solo IDs de notificaciones no leídas (ultra rápido)
     * Para badge count sin cargar toda la entidad
     */
    @Query("SELECT n.id FROM Notificacion n WHERE n.usuarioId = :usuarioId AND n.leida = false")
    List<UUID> findNoLeidasIds(@Param("usuarioId") UUID usuarioId);

    /**
     * Obtener notificaciones no leídas de un usuario
     */
    @Query("SELECT n FROM Notificacion n WHERE n.usuarioId = :usuarioId AND n.leida = false ORDER BY n.createdAt DESC")
    List<Notificacion> findNoLeidasByUsuarioId(@Param("usuarioId") UUID usuarioId);

    /**
     * Contar notificaciones no leídas de un usuario
     */
    @Query("SELECT COUNT(n) FROM Notificacion n WHERE n.usuarioId = :usuarioId AND n.leida = false")
    long countNoLeidasByUsuarioId(@Param("usuarioId") UUID usuarioId);

    /**
     * Obtener notificaciones por tipo
     */
    @Query("SELECT n FROM Notificacion n WHERE n.usuarioId = :usuarioId AND n.tipo = :tipo ORDER BY n.createdAt DESC")
    List<Notificacion> findByUsuarioIdAndTipo(
        @Param("usuarioId") UUID usuarioId,
        @Param("tipo") String tipo
    );

    /**
     * Marcar todas las notificaciones de un usuario como leídas
     */
    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true, n.fechaLectura = :fecha WHERE n.usuarioId = :usuarioId AND n.leida = false")
    int marcarTodasComoLeidas(@Param("usuarioId") UUID usuarioId, @Param("fecha") Instant fecha);

    /**
     * Eliminar notificaciones antiguas (más de X días)
     */
    @Modifying
    @Query("DELETE FROM Notificacion n WHERE n.createdAt < :fecha")
    int eliminarAntiguasAntesDe(@Param("fecha") Instant fecha);

    /**
     * Obtener notificaciones recientes (últimas 50)
     */
    @Query("SELECT n FROM Notificacion n WHERE n.usuarioId = :usuarioId ORDER BY n.createdAt DESC LIMIT 50")
    List<Notificacion> findRecientesByUsuarioId(@Param("usuarioId") UUID usuarioId);

    /**
     * Verificar si existe notificación para una entidad específica
     */
    @Query("SELECT COUNT(n) > 0 FROM Notificacion n WHERE n.usuarioId = :usuarioId AND n.entidadId = :entidadId AND n.tipo = :tipo")
    boolean existeNotificacion(
        @Param("usuarioId") UUID usuarioId,
        @Param("entidadId") UUID entidadId,
        @Param("tipo") String tipo
    );
}
