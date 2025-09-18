package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.um.faltauno.entity.Cancha;

public interface CanchaRepository extends JpaRepository<Cancha, Long> {
}
