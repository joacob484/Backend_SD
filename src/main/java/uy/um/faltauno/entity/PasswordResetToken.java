package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Token de recuperación de contraseña.
 * Expira después de 1 hora.
 */
@Entity
@Table(name = "password_reset_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "usado", nullable = false)
    @Builder.Default
    private boolean usado = false;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    /**
     * Verifica si el token está expirado
     */
    public boolean estaExpirado() {
        return LocalDateTime.now().isAfter(expiraEn);
    }

    /**
     * Verifica si el token es válido (no usado y no expirado)
     */
    public boolean esValido() {
        return !usado && !estaExpirado();
    }

    @PrePersist
    protected void onCreate() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
        if (expiraEn == null) {
            // Token válido por 1 hora
            expiraEn = LocalDateTime.now().plusHours(1);
        }
    }
}
