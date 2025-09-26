package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import uy.um.faltauno.entity.Inscripcion;
import java.util.List;
import java.util.UUID;

public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID> {
    @Query("SELECT i FROM Inscripcion i WHERE i.usuario.id = :usuarioId")
    List<Inscripcion> findByUsuarioId(@Param("usuarioId") UUID usuarioId);
    List<Inscripcion> findByUsuario_IdAndEstado(UUID usuarioId, String estado);
    @Query("SELECT i FROM Inscripcion i WHERE i.partido.id = :partidoId")
    List<Inscripcion> findByPartidoId(@Param("partidoId") UUID partidoId);
    List<Inscripcion> findByPartido_IdAndEstado(UUID partidoId, String estado);
}
