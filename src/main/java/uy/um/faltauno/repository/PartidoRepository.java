package uy.um.faltauno.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.um.faltauno.entity.Partido;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartidoRepository extends JpaRepository<Partido, UUID>, JpaSpecificationExecutor<Partido> {
    
    /**
     * ✅ OPTIMIZACIÓN: EntityGraph evita queries adicionales para organizador
     * Mejor que JOIN FETCH porque funciona con paginación
     */
    @EntityGraph(attributePaths = {"organizador"})
    Optional<Partido> findById(UUID id);
    
    @Query("""
           select p
           from Partido p
           left join fetch p.organizador
           where p.id = :id
           """)
    Optional<Partido> findByIdWithOrganizador(@Param("id") UUID id);

    /**
     * Buscar partidos por organizador
     */
    List<Partido> findByOrganizador_Id(UUID organizadorId);
    
    /**
     * ✅ OPTIMIZACIÓN: Buscar partidos por organizador con JOIN FETCH
     * Evita LazyInitializationException al acceder a organizador.nombre/apellido
     */
    @Query("""
           SELECT p
           FROM Partido p
           LEFT JOIN FETCH p.organizador
           WHERE p.organizador.id = :organizadorId
           ORDER BY p.fecha DESC, p.hora DESC
           """)
    List<Partido> findByOrganizadorIdWithOrganizador(@Param("organizadorId") UUID organizadorId);
    
    /**
     * Buscar partidos por fecha
     */
    List<Partido> findByFecha(LocalDate fecha);
    
    /**
     * Buscar partidos futuros
     */
    @Query("SELECT p FROM Partido p WHERE p.fecha >= :fecha ORDER BY p.fecha ASC, p.hora ASC")
    List<Partido> findPartidosFuturos(@Param("fecha") LocalDate fecha);
    
    /**
     * Buscar partidos por tipo
     */
    List<Partido> findByTipoPartido(String tipoPartido);
    
    /**
     * Buscar partidos por ubicación (búsqueda parcial)
     */
    @Query("SELECT p FROM Partido p WHERE LOWER(p.nombreUbicacion) LIKE LOWER(CONCAT('%', :ubicacion, '%')) " +
           "OR LOWER(p.direccionUbicacion) LIKE LOWER(CONCAT('%', :ubicacion, '%'))")
    List<Partido> findByUbicacion(@Param("ubicacion") String ubicacion);
    
    /**
     * Contar partidos por estado
     */
    long countByEstado(String estado);
    
    /**
     * Contar partidos entre fechas
     */
    @Query("SELECT COUNT(p) FROM Partido p WHERE p.fecha >= :fechaInicio AND p.fecha < :fechaFin")
    long countPartidosEntreFechas(@Param("fechaInicio") LocalDate fechaInicio, @Param("fechaFin") LocalDate fechaFin);
    
    /**
     * Contar usuarios distintos que organizaron partidos desde una fecha
     */
    @Query("SELECT COUNT(DISTINCT p.organizador.id) FROM Partido p WHERE p.fecha >= :fechaDesde")
    long countDistinctOrganizadorByFechaGreaterThanEqual(@Param("fechaDesde") LocalDate fechaDesde);
    
    /**
     * Buscar partidos por estado y fecha
     * Filtrado de hora se hace en memoria en el service
     */
    List<Partido> findByEstadoAndFechaLessThanEqual(String estado, LocalDate fecha);
    
    /**
     * Buscar partidos disponibles por fecha
     * Usado para cancelaciones automáticas
     */
    List<Partido> findByEstadoAndFecha(String estado, LocalDate fecha);
    
    /**
     * ✅ OPTIMIZACIÓN: Buscar TODOS los partidos con JOIN FETCH del organizador
     * Evita N+1 queries y LazyInitializationException
     * Usar en listarPartidos() cuando se necesita el organizador
     */
    @Query("""
           SELECT DISTINCT p
           FROM Partido p
           LEFT JOIN FETCH p.organizador
           ORDER BY p.fecha DESC, p.hora DESC
           """)
    List<Partido> findAllWithOrganizador();
    
    /**
     * ✅ OPTIMIZACIÓN: Paginación con EntityGraph
     * Soluciona problema de JOIN FETCH que no funciona con paginación
     */
    @EntityGraph(attributePaths = {"organizador"})
    @Query("SELECT p FROM Partido p ORDER BY p.fecha DESC, p.hora DESC")
    Page<Partido> findAllPaginated(Pageable pageable);
    
    /**
     * ✅ OPTIMIZACIÓN: Buscar partidos futuros con paginación
     */
    @EntityGraph(attributePaths = {"organizador"})
    @Query("SELECT p FROM Partido p WHERE p.fecha >= :fecha ORDER BY p.fecha ASC, p.hora ASC")
    Page<Partido> findPartidosFuturosPaginated(@Param("fecha") LocalDate fecha, Pageable pageable);
    
    /**
     * ✅ OPTIMIZACIÓN: Buscar por estado con EntityGraph
     */
    @EntityGraph(attributePaths = {"organizador"})
    @Query("SELECT p FROM Partido p WHERE p.estado = :estado ORDER BY p.fecha DESC")
    List<Partido> findByEstadoWithOrganizador(@Param("estado") String estado);
    
    /**
     * ✅ OPTIMIZACIÓN: Búsqueda con filtros múltiples
     */
    @EntityGraph(attributePaths = {"organizador"})
    @Query("""
        SELECT p FROM Partido p 
        WHERE p.fecha >= :fechaDesde 
        AND (:estado IS NULL OR p.estado = :estado)
        AND (:tipoPartido IS NULL OR p.tipoPartido = :tipoPartido)
        ORDER BY p.fecha ASC, p.hora ASC
    """)
    Page<Partido> buscarConFiltros(
        @Param("fechaDesde") LocalDate fechaDesde,
        @Param("estado") String estado,
        @Param("tipoPartido") String tipoPartido,
        Pageable pageable
    );
}