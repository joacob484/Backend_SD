package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.um.faltauno.entity.Mensaje;

import java.util.List;
import java.util.UUID;

public interface MensajeRepository extends JpaRepository<Mensaje, UUID> {
    List<Mensaje> findByDestinatarioIdAndLeido(UUID destinatarioId, boolean leido);
}
