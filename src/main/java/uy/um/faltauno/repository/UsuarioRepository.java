package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import uy.um.faltauno.entity.Usuario;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    // ✅ Métodos que excluyen usuarios eliminados (deleted_at IS NULL)
    @Query("SELECT u FROM Usuario u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<Usuario> findByEmail(@Param("email") String email);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM Usuario u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM Usuario u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<Usuario> findById(@Param("id") UUID id);
    
    @Query("select u.id as id, u.email as email, u.password as password from Usuario u where u.email = :email and u.deletedAt is null")
    Optional<AuthProjection> findAuthProjectionByEmail(@Param("email") String email);

    /**
     * Contar usuarios registrados después de una fecha (excluyendo eliminados)
     */
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.createdAt > :fecha AND u.deletedAt IS NULL")
    long countByCreatedAtAfter(@Param("fecha") LocalDateTime fecha);
    
    /**
     * Contar usuarios activos después de una fecha (para calcular "Usuarios activos ahora")
     */
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.lastActivityAt > :fecha AND u.deletedAt IS NULL")
    long countByLastActivityAtAfter(@Param("fecha") LocalDateTime fecha);

    /**
     * Buscar usuario eliminado por email (para recuperación de cuenta)
     */
    @Query("SELECT u FROM Usuario u WHERE u.email = :email AND u.deletedAt IS NOT NULL")
    Optional<Usuario> findDeletedByEmail(@Param("email") String email);

    /**
     * Buscar usuarios eliminados hace más de 30 días (para cleanup físico)
     */
    @Query("SELECT u FROM Usuario u WHERE u.deletedAt IS NOT NULL AND u.deletedAt < :cutoffDate")
    java.util.List<Usuario> findExpiredDeletedUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    interface AuthProjection {
        UUID getId();
        String getEmail();
        String getPassword();
    }
}