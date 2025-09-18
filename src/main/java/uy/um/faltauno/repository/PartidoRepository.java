package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.yourorg.backend.entity.Partido;
import java.util.List;

public interface PartidoRepository extends JpaRepository<Partido, Long> {
    List<Partido> findByZonaAndNivel(String zona, String nivel);
}
