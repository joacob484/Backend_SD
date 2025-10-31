package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.um.faltauno.entity.Review;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    
    /**
     * Buscar reviews recibidas por un usuario
     */
    List<Review> findByUsuarioCalificado_Id(UUID usuarioCalificadoId);
    
    /**
     * Buscar reviews hechas por un usuario
     */
    List<Review> findByUsuarioQueCalifica_Id(UUID usuarioQueCalificaId);
    
    /**
     * Buscar reviews de un partido específico
     */
    List<Review> findByPartido_Id(UUID partidoId);
    
    /**
     * Verificar si ya existe una review específica
     */
    boolean existsByPartido_IdAndUsuarioQueCalifica_IdAndUsuarioCalificado_Id(
            UUID partidoId,
            UUID usuarioQueCalificaId,
            UUID usuarioCalificadoId);
    
    /**
     * Obtener review específica
     */
    @Query("SELECT r FROM Review r WHERE r.partido.id = :partidoId " +
           "AND r.usuarioQueCalifica.id = :calificadorId " +
           "AND r.usuarioCalificado.id = :calificadoId")
    Review findReviewEspecifica(
            @Param("partidoId") UUID partidoId,
            @Param("calificadorId") UUID calificadorId,
            @Param("calificadoId") UUID calificadoId);
    
    /**
     * Contar reviews de un usuario
     */
    long countByUsuarioCalificado_Id(UUID usuarioCalificadoId);
    
    /**
     * Calcular promedio de nivel de un usuario
     */
    @Query("SELECT AVG(r.nivel) FROM Review r WHERE r.usuarioCalificado.id = :usuarioId")
    Double calcularPromedioNivel(@Param("usuarioId") UUID usuarioId);
    
    /**
     * Calcular promedio de deportividad de un usuario
     */
    @Query("SELECT AVG(r.deportividad) FROM Review r WHERE r.usuarioCalificado.id = :usuarioId")
    Double calcularPromedioDeportividad(@Param("usuarioId") UUID usuarioId);
    
    /**
     * Calcular promedio de compañerismo de un usuario
     */
    @Query("SELECT AVG(r.companerismo) FROM Review r WHERE r.usuarioCalificado.id = :usuarioId")
    Double calcularPromedioCompanerismo(@Param("usuarioId") UUID usuarioId);
    
    /**
     * Calcular promedio global de calificación (promedio de nivel, deportividad y compañerismo)
     */
    @Query("SELECT AVG((r.nivel + r.deportividad + r.companerismo) / 3.0) FROM Review r")
    Double findAverageCalificacionGlobal();
}