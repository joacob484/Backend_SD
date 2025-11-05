package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uy.um.faltauno.entity.PasswordResetToken;
import uy.um.faltauno.entity.Usuario;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Buscar token por el string del token
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Invalidar todos los tokens anteriores de un usuario
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usado = true WHERE t.usuario = :usuario AND t.usado = false")
    void invalidarTokensDelUsuario(Usuario usuario);

    /**
     * Eliminar tokens expirados (cleanup automático)
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiraEn < :fecha")
    int eliminarTokensExpirados(LocalDateTime fecha);

    /**
     * Contar tokens válidos recientes de un usuario (para prevenir spam)
     */
    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.usuario = :usuario AND t.creadoEn > :desde AND t.usado = false")
    long contarTokensRecientesDelUsuario(Usuario usuario, LocalDateTime desde);
}
