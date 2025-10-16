package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.um.faltauno.entity.Partido;

import java.util.UUID;

public interface PartidoRepository extends JpaRepository<Partido, UUID> { }
