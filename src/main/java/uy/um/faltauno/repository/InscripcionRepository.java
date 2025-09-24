package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.um.faltauno.entity.Inscripcion;
import java.util.List;
import java.util.UUID;

public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID> {
    List<Inscripcion> findByUsuario_Id(UUID usuarioId);
    List<Inscripcion> findByUsuario_IdAndEstado(UUID usuarioId, String estado);
    List<Inscripcion> findByPartido_Id(UUID partidoId);
    List<Inscripcion> findByPartido_IdAndEstado(UUID partidoId, String estado);
}
