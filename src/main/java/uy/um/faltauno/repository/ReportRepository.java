package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uy.um.faltauno.entity.Report;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    /**
     * Buscar reportes por estado
     */
    @Query("SELECT r FROM Report r WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<Report> findByStatus(@Param("status") Report.ReportStatus status);

    /**
     * Buscar reportes pendientes (PENDING o UNDER_REVIEW)
     */
    @Query("SELECT r FROM Report r WHERE r.status IN ('PENDING', 'UNDER_REVIEW') ORDER BY r.createdAt DESC")
    List<Report> findPendingReports();

    /**
     * Contar reportes pendientes
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.status IN ('PENDING', 'UNDER_REVIEW')")
    long countPendingReports();

    /**
     * Buscar reportes de un usuario reportado
     */
    @Query("SELECT r FROM Report r WHERE r.reportedUser.id = :userId ORDER BY r.createdAt DESC")
    List<Report> findByReportedUserId(@Param("userId") UUID userId);

    /**
     * Buscar reportes hechos por un usuario
     */
    @Query("SELECT r FROM Report r WHERE r.reporter.id = :userId ORDER BY r.createdAt DESC")
    List<Report> findByReporterId(@Param("userId") UUID userId);

    /**
     * Contar reportes hechos por un usuario en un periodo
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.reporter.id = :reporterId AND r.createdAt > :since")
    long countByReporterIdAndCreatedAtAfter(@Param("reporterId") UUID reporterId, @Param("since") Instant since);

    /**
     * Verificar si existe un reporte activo (pendiente o en revisión) entre dos usuarios
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Report r " +
           "WHERE r.reporter.id = :reporterId " +
           "AND r.reportedUser.id = :reportedId " +
           "AND r.status IN ('PENDING', 'UNDER_REVIEW')")
    boolean existsActiveReportBetween(@Param("reporterId") UUID reporterId, @Param("reportedId") UUID reportedId);

    /**
     * Buscar reporte activo entre dos usuarios
     */
    @Query("SELECT r FROM Report r " +
           "WHERE r.reporter.id = :reporterId " +
           "AND r.reportedUser.id = :reportedId " +
           "AND r.status IN ('PENDING', 'UNDER_REVIEW')")
    Optional<Report> findActiveReportBetween(@Param("reporterId") UUID reporterId, @Param("reportedId") UUID reportedId);

    /**
     * Contar reportes recibidos por un usuario (para determinar usuarios problemáticos)
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.reportedUser.id = :userId AND r.status = 'RESOLVED'")
    long countResolvedReportsAgainstUser(@Param("userId") UUID userId);

    /**
     * Obtener todos los reportes con detalles (para admin)
     */
    @Query("SELECT r FROM Report r " +
           "LEFT JOIN FETCH r.reporter " +
           "LEFT JOIN FETCH r.reportedUser " +
           "LEFT JOIN FETCH r.resolvedBy " +
           "ORDER BY r.createdAt DESC")
    List<Report> findAllWithDetails();

    /**
     * Obtener reportes pendientes con detalles
     */
    @Query("SELECT r FROM Report r " +
           "LEFT JOIN FETCH r.reporter " +
           "LEFT JOIN FETCH r.reportedUser " +
           "WHERE r.status IN ('PENDING', 'UNDER_REVIEW') " +
           "ORDER BY r.createdAt DESC")
    List<Report> findPendingReportsWithDetails();

    /**
     * Obtener estadísticas de reportes por usuario reportado
     */
    @Query("SELECT r.reportedUser.id, COUNT(r) FROM Report r " +
           "WHERE r.status IN ('PENDING', 'UNDER_REVIEW') " +
           "GROUP BY r.reportedUser.id " +
           "HAVING COUNT(r) >= :minCount " +
           "ORDER BY COUNT(r) DESC")
    List<Object[]> findMostReportedUsers(@Param("minCount") long minCount);
}
