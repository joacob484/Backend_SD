package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.um.faltauno.entity.Partido;

import java.util.UUID;
import java.util.List;

@Repository
public interface PartidoRepository extends JpaRepository<Partido, UUID> {
    List<Partido> findByFechaGreaterThanEqualOrderByFechaAsc(java.time.LocalDate fecha);
}
