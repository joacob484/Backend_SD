package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uy.um.faltauno.entity.ChatVisit;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Usuario;

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
}
