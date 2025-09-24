package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.um.faltauno.entity.Amistad;

import java.util.List;
import java.util.UUID;

public interface AmistadRepository extends JpaRepository<Amistad, UUID> {
    List<Amistad> findByAmigoIdAndEstado(UUID amigoId, String estado);
}
