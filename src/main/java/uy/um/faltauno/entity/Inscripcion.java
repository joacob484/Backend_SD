package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad que representa un jugador ACEPTADO en un partido.
 * Si el usuario está en esta tabla = está dentro del partido.
 * Las solicitudes pendientes van en la tabla SolicitudPartido.
 */
@Entity
@Table(
    name = "inscripcion",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_inscripcion_partido_usuario",
            columnNames = {"partido_id", "usuario_id"}
        )
    },
    indexes = {
        @Index(name = "idx_inscripcion_partido_created", columnList = "partido_id,created_at DESC"),
        @Index(name = "idx_inscripcion_usuario_created", columnList = "usuario_id,created_at DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"partido", "usuario"})
@EqualsAndHashCode(of = "id")
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Partido al que se inscribe el usuario
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "partido_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_inscripcion_partido")
    )
    private Partido partido;

    /**
     * Usuario que se inscribe
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "usuario_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_inscripcion_usuario")
    )
    private Usuario usuario;

    /**
     * Comentario opcional del usuario al solicitar inscripción (migrado de solicitud)
     */
    @Column(name = "comentario", length = 500)
    private String comentario;

    /**
     * Fecha de creación de la inscripción
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Fecha de última actualización
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Fecha de inscripción (cuando el organizador aceptó)
     */
    @Column(name = "fecha_inscripcion")
    @Builder.Default
    private Instant fechaInscripcion = Instant.now();

    /**
     * ✅ OPTIMIZACIÓN: Control de concurrencia optimista
     */
    @Version
    @Column(name = "version")
    private Long version;

    // ==============================================
    // CALLBACKS DE JPA
    // ==============================================

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.fechaInscripcion == null) {
            this.fechaInscripcion = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}