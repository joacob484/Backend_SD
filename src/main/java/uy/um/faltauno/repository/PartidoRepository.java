package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.um.faltauno.entity.Partido;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PartidoRepository extends JpaRepository<Partido, UUID>, JpaSpecificationExecutor<Partido> {
    /**
     * Buscar partidos por organizador
     */
    List<Partido> findByOrganizador_Id(UUID organizadorId);
    
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
}