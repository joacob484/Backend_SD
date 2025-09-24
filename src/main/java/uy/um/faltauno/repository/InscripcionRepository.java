package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.um.faltauno.entity.Inscripcion;
import java.util.UUID;
import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID> {
    List<Inscripcion> findByUsuarioId(UUID usuarioId);
    List<Inscripcion> findByPartidoId(UUID partidoId);
    List<Inscripcion> findByUsuarioIdAndEstado(UUID usuarioId, String estado);
}
