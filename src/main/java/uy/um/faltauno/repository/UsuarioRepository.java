package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import uy.um.faltauno.entity.Usuario;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    
    @Query("select u.id as id, u.email as email, u.password as password from Usuario u where u.email = :email")
    Optional<AuthProjection> findAuthProjectionByEmail(@Param("email") String email);

    /**
     * Contar usuarios registrados después de una fecha
     */
    long countByFechaRegistroAfter(LocalDateTime fecha);
    
    /**
     * Contar usuarios activos (que participaron en al menos un partido desde una fecha)
     */
    @Query("""
        SELECT COUNT(DISTINCT u.id) 
        FROM Usuario u 
        JOIN u.partidosJugados p 
        WHERE p.fecha >= :fechaDesde
    """)
    long countUsuariosActivosDesde(@Param("fechaDesde") LocalDateTime fechaDesde);

    interface AuthProjection {
        UUID getId();
        String getEmail();
        String getPassword();
    }
}