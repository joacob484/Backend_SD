package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.um.faltauno.entity.Partido;

public interface PartidoRepository extends JpaRepository<Partido, Long> {
  // acá podés agregar queries tipo findByZonaAndNivel(...)
}
