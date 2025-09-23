package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.um.faltauno.entity.Usuario;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
}
