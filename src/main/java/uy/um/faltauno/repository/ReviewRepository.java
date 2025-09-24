package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.um.faltauno.entity.Review;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByUsuarioCalificadoId(UUID usuarioId);
    List<Review> findByPartidoId(UUID partidoId);
    List<Review> findByUsuarioQueCalificaAndNivelIsNull(UUID usuarioId);
}
