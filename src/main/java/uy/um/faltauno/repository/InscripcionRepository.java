package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.um.faltauno.entity.Inscripcion;

import java.util.List;
import java.util.UUID;

public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID> {
    
    /**
     * Buscar inscripciones por usuario
     */
    @Query("SELECT i FROM Inscripcion i WHERE i.usuario.id = :usuarioId")
    List<Inscripcion> findByUsuarioId(@Param("usuarioId") UUID usuarioId);
    
    /**
     * Buscar inscripciones por usuario y estado
     */
    List<Inscripcion> findByUsuario_IdAndEstado(UUID usuarioId, String estado);
    
    /**
     * Buscar inscripciones por partido (SIN filtro de estado)
     */
    @Query("SELECT i FROM Inscripcion i WHERE i.partido.id = :partidoId")
    List<Inscripcion> findByPartidoId(@Param("partidoId") UUID partidoId);
    
    /**
     * Buscar inscripciones por partido y estado
     */
    List<Inscripcion> findByPartido_IdAndEstado(UUID partidoId, String estado);
    
    /**
     * Contar inscripciones aceptadas de un partido
     */
    @Query("SELECT COUNT(i) FROM Inscripcion i WHERE i.partido.id = :partidoId AND i.estado = 'ACEPTADO'")
    long countInscripcionesAceptadas(@Param("partidoId") UUID partidoId);
    
    /**
     * Verificar si un usuario ya está inscrito en un partido
     */
    @Query("SELECT COUNT(i) > 0 FROM Inscripcion i WHERE i.partido.id = :partidoId AND i.usuario.id = :usuarioId")
    boolean existeInscripcion(@Param("partidoId") UUID partidoId, @Param("usuarioId") UUID usuarioId);
}