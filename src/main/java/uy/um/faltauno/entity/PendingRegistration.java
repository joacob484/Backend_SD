package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad para almacenar registros pendientes de verificación de email.
 * Los usuarios LOCAL deben verificar su email antes de ser creados en la tabla usuario.
 * Después de verificar el código, se crea el usuario real y se elimina este registro.
 */
@Entity
@Table(name = "pending_registration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Email del usuario (temporal hasta confirmar)
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Contraseña encriptada (temporal)
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Código de verificación de 6 dígitos
     */
    @Column(name = "verification_code", nullable = false, length = 6)
    private String verificationCode;

    /**
     * Fecha de expiración del código de verificación
     */
    @Column(name = "verification_code_expires_at", nullable = false)
    private LocalDateTime verificationCodeExpiresAt;

    /**
     * Fecha de creación del pre-registro
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Verificar si el código ha expirado
     */
    public boolean isCodeExpired() {
        return LocalDateTime.now().isAfter(verificationCodeExpiresAt);
    }

    /**
     * Verificar si el código es válido
     */
    public boolean isValidCode(String code) {
        return !isCodeExpired() && verificationCode.equals(code);
    }
}
