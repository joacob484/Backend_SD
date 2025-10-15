package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import uy.um.faltauno.entity.Usuario;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    
    @Query("select u.id as id, u.email as email, u.password as password from Usuario u where u.email = :email")
    Optional<AuthProjection> findAuthProjectionByEmail(@Param("email") String email);

    interface AuthProjection {
        UUID getId();
        String getEmail();
        String getPassword();
    }
}