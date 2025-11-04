package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Solicitud de un usuario para unirse a un partido.
 * Cuando el organizador la acepta, se crea una Inscripcion.
 * Cuando la rechaza, simplemente se elimina.
 */
@Entity
@Table(
    name = "solicitud_partido",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_solicitud_partido_usuario",
            columnNames = {"partido_id", "usuario_id"}
        )
    },
    indexes = {
        @Index(name = "idx_solicitud_partido", columnList = "partido_id,created_at DESC"),
        @Index(name = "idx_solicitud_usuario", columnList = "usuario_id,created_at DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"partido", "usuario"})
@EqualsAndHashCode(of = "id")
public class SolicitudPartido {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Partido al que el usuario quiere unirse
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "partido_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_solicitud_partido")
    )
    private Partido partido;

    /**
     * Usuario que solicita unirse
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "usuario_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_solicitud_usuario")
    )
    private Usuario usuario;

    /**
     * Comentario opcional del usuario al solicitar
     */
    @Column(name = "comentario", length = 500)
    private String comentario;

    /**
     * Fecha de creación de la solicitud
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
