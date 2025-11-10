package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uy.um.faltauno.entity.ChatVisit;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatVisitRepository extends JpaRepository<ChatVisit, Long> {
    
    /**
     * Encuentra la visita de un usuario específico a un partido específico
     */
    Optional<ChatVisit> findByUsuarioAndPartido(Usuario usuario, Partido partido);
    
    /**
     * Encuentra la visita de un usuario específico a un partido específico (por IDs)
     */
    @Query("SELECT cv FROM ChatVisit cv WHERE cv.usuario.id = :usuarioId AND cv.partido.id = :partidoId")
    Optional<ChatVisit> findByUsuarioIdAndPartidoId(
        @Param("usuarioId") UUID usuarioId, 
        @Param("partidoId") UUID partidoId
    );
    
    /**
     * Encuentra todas las visitas de chat de un usuario
     */
    @Query("SELECT cv FROM ChatVisit cv WHERE cv.usuario.id = :usuarioId")
    List<ChatVisit> findByUsuarioId(@Param("usuarioId") UUID usuarioId);
    
    /**
     * Encuentra todas las visitas de chat de un partido
     */
    @Query("SELECT cv FROM ChatVisit cv WHERE cv.partido.id = :partidoId")
    List<ChatVisit> findByPartidoId(@Param("partidoId") UUID partidoId);
}
