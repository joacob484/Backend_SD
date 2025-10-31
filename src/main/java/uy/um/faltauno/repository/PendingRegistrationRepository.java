package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.um.faltauno.entity.PendingRegistration;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, UUID> {

    /**
     * Buscar por email
     */
    Optional<PendingRegistration> findByEmail(String email);

    /**
     * Verificar si existe un registro pendiente para un email
     */
    boolean existsByEmail(String email);

    /**
     * Buscar por código de verificación
     */
    Optional<PendingRegistration> findByVerificationCode(String code);

    /**
     * Eliminar por email
     */
    void deleteByEmail(String email);

    /**
     * Limpiar registros expirados (llamar periódicamente)
     */
    @Modifying
    @Query("DELETE FROM PendingRegistration p WHERE p.verificationCodeExpiresAt < :cutoffTime")
    int deleteExpiredRegistrations(@Param("cutoffTime") LocalDateTime cutoffTime);
}
