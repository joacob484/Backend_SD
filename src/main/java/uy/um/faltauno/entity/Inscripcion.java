package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad que representa una inscripción de un usuario a un partido.
 * Gestiona el flujo: PENDIENTE -> ACEPTADO/RECHAZADO -> CANCELADO
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
        @Index(name = "idx_inscripcion_partido", columnList = "partido_id"),
        @Index(name = "idx_inscripcion_usuario", columnList = "usuario_id"),
        @Index(name = "idx_inscripcion_estado", columnList = "estado"),
        @Index(name = "idx_inscripcion_created", columnList = "created_at")
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
     * Estado de la inscripción
     * PENDIENTE: Esperando aprobación del organizador
     * ACEPTADO: Aprobado, el usuario está confirmado en el partido
     * RECHAZADO: Rechazado por el organizador
     * CANCELADO: Cancelado por el usuario
     */
    @Column(name = "estado", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoInscripcion estado = EstadoInscripcion.PENDIENTE;

    /**
     * Comentario opcional del usuario al solicitar inscripción
     */
    @Column(name = "comentario", length = 500)
    private String comentario;

    /**
     * Motivo de rechazo (si aplica)
     */
    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

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
     * Fecha de aceptación (si fue aceptada)
     */
    @Column(name = "fecha_aceptacion")
    private Instant fechaAceptacion;

    /**
     * Fecha de rechazo (si fue rechazada)
     */
    @Column(name = "fecha_rechazo")
    private Instant fechaRechazo;

    /**
     * ✅ OPTIMIZACIÓN: Control de concurrencia optimista
     * Previene race conditions al aceptar/rechazar inscripciones simultáneas
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Fecha de cancelación (si fue cancelada)
     */
    @Column(name = "fecha_cancelacion")
    private Instant fechaCancelacion;

    /**
     * Enum para los estados de la inscripción
     */
    public enum EstadoInscripcion {
        PENDIENTE("Pendiente de aprobación"),
        ACEPTADO("Aceptado en el partido"),
        RECHAZADO("Rechazado por el organizador"),
        CANCELADO("Cancelado por el usuario");

        private final String descripcion;

        EstadoInscripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    // ==============================================
    // MÉTODOS DE NEGOCIO
    // ==============================================

    /**
     * Acepta la inscripción
     */
    public void aceptar() {
        if (this.estado != EstadoInscripcion.PENDIENTE) {
            throw new IllegalStateException(
                "Solo se pueden aceptar inscripciones en estado PENDIENTE. Estado actual: " + this.estado
            );
        }
        this.estado = EstadoInscripcion.ACEPTADO;
        this.fechaAceptacion = Instant.now();
    }

    /**
     * Rechaza la inscripción
     * @param motivo Motivo del rechazo
     */
    public void rechazar(String motivo) {
        if (this.estado != EstadoInscripcion.PENDIENTE) {
            throw new IllegalStateException(
                "Solo se pueden rechazar inscripciones en estado PENDIENTE. Estado actual: " + this.estado
            );
        }
        this.estado = EstadoInscripcion.RECHAZADO;
        this.motivoRechazo = motivo;
        this.fechaRechazo = Instant.now();
    }

    /**
     * Cancela la inscripción (por parte del usuario)
     */
    public void cancelar() {
        if (this.estado == EstadoInscripcion.CANCELADO) {
            throw new IllegalStateException("La inscripción ya está cancelada");
        }
        this.estado = EstadoInscripcion.CANCELADO;
        this.fechaCancelacion = Instant.now();
    }

    /**
     * Verifica si la inscripción está activa (PENDIENTE o ACEPTADO)
     */
    public boolean isActiva() {
        return this.estado == EstadoInscripcion.PENDIENTE || 
               this.estado == EstadoInscripcion.ACEPTADO;
    }

    /**
     * Verifica si la inscripción está pendiente
     */
    public boolean isPendiente() {
        return this.estado == EstadoInscripcion.PENDIENTE;
    }

    /**
     * Verifica si la inscripción fue aceptada
     */
    public boolean isAceptada() {
        return this.estado == EstadoInscripcion.ACEPTADO;
    }

    /**
     * Verifica si la inscripción fue rechazada
     */
    public boolean isRechazada() {
        return this.estado == EstadoInscripcion.RECHAZADO;
    }

    /**
     * Verifica si la inscripción fue cancelada
     */
    public boolean isCancelada() {
        return this.estado == EstadoInscripcion.CANCELADO;
    }

    // ==============================================
    // CALLBACKS DE JPA
    // ==============================================

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.estado == null) {
            this.estado = EstadoInscripcion.PENDIENTE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}