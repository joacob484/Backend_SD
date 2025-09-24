package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.um.faltauno.entity.Review;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByUsuarioCalificado_Id(UUID usuarioCalificadoId);
    List<Review> findByUsuarioQueCalifica_Id(UUID usuarioQueCalificaId);
    List<Review> findByUsuarioQueCalifica_IdAndNivelIsNull(UUID usuarioQueCalificaId);
    boolean existsByPartido_IdAndUsuarioQueCalifica_IdAndUsuarioCalificado_Id(
        UUID partidoId,
        UUID usuarioQueCalificaId,
        UUID usuarioCalificadoId
    );
    List<Review> findByPartido_Id(UUID partidoId);
}
