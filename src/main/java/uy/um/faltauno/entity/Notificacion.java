package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notificacion", indexes = {
    @Index(name = "idx_notificacion_usuario", columnList = "usuario_id"),
    @Index(name = "idx_notificacion_leida", columnList = "leida"),
    @Index(name = "idx_notificacion_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Usuario que recibe la notificación
     */
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    /**
     * Tipo de notificación
     */
    @Column(name = "tipo", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TipoNotificacion tipo;

    /**
     * Título de la notificación
     */
    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    /**
     * Mensaje/contenido de la notificación
     */
    @Column(name = "mensaje", nullable = false, length = 500)
    private String mensaje;

    /**
     * ID de la entidad relacionada (partido, usuario, etc.)
     */
    @Column(name = "entidad_id")
    private UUID entidadId;

    /**
     * Tipo de entidad relacionada
     */
    @Column(name = "entidad_tipo", length = 50)
    private String entidadTipo;

    /**
     * URL de acción (donde redirigir al hacer click)
     */
    @Column(name = "url_accion", length = 500)
    private String urlAccion;

    /**
     * Estado de lectura
     */
    @Column(name = "leida", nullable = false)
    @Builder.Default
    private Boolean leida = false;

    /**
     * Fecha de lectura
     */
    @Column(name = "fecha_lectura")
    private Instant fechaLectura;

    /**
     * Fecha de creación
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Prioridad de la notificación
     */
    @Column(name = "prioridad", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Prioridad prioridad = Prioridad.NORMAL;

    // ==============================================
    // ENUMS
    // ==============================================

    public enum TipoNotificacion {
        INVITACION_PARTIDO,
        SOLICITUD_AMISTAD,
        AMISTAD_ACEPTADA,
        INSCRIPCION_ACEPTADA,
        INSCRIPCION_RECHAZADA,
        NUEVA_SOLICITUD,
        PARTIDO_CANCELADO,
        PARTIDO_CONFIRMADO,
        PARTIDO_LISTO,
        PARTIDO_COMPLETADO,
        JUGADOR_UNIDO,
        JUGADOR_SALIO,
        REVIEW_PENDIENTE,
        NUEVO_MENSAJE,
        PARTIDO_PROXIMO,
        INVITACION_ACEPTADA,
        INVITACION_RECHAZADA
    }

    public enum Prioridad {
        BAJA,
        NORMAL,
        ALTA,
        URGENTE
    }

    // ==============================================
    // MÉTODOS DE NEGOCIO
    // ==============================================

    /**
     * Marca la notificación como leída
     */
    public void marcarComoLeida() {
        this.leida = true;
        this.fechaLectura = Instant.now();
    }

    /**
     * Marca la notificación como no leída
     */
    public void marcarComoNoLeida() {
        this.leida = false;
        this.fechaLectura = null;
    }
}
