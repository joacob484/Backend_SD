package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Reporte de usuario
 * Los usuarios pueden reportar a otros usuarios por comportamiento inapropiado
 */
@Entity
@Table(name = "reports", indexes = {
    @Index(name = "idx_report_reporter", columnList = "reporter_id"),
    @Index(name = "idx_report_reported", columnList = "reported_user_id"),
    @Index(name = "idx_report_status", columnList = "status"),
    @Index(name = "idx_report_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"reporter", "reportedUser", "resolvedBy"})
@EqualsAndHashCode(of = "id")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Usuario que realiza el reporte
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Usuario reporter;

    /**
     * Usuario reportado
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private Usuario reportedUser;

    /**
     * Razón del reporte
     */
    @Column(name = "reason", nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    /**
     * Descripción detallada del reporte
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Estado del reporte
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    /**
     * Fecha de creación del reporte
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Fecha de resolución del reporte
     */
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /**
     * Admin que resolvió el reporte
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private Usuario resolvedBy;

    /**
     * Notas de resolución del admin
     */
    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    /**
     * Acción tomada
     */
    @Column(name = "action_taken", length = 50)
    @Enumerated(EnumType.STRING)
    private ReportAction actionTaken;

    // ==============================================
    // ENUMS
    // ==============================================

    public enum ReportReason {
        COMPORTAMIENTO_INAPROPIADO("Comportamiento inapropiado"),
        LENGUAJE_OFENSIVO("Lenguaje ofensivo"),
        SPAM("Spam"),
        SUPLANTACION_IDENTIDAD("Suplantación de identidad"),
        ACOSO("Acoso o intimidación"),
        CONTENIDO_INAPROPIADO("Contenido inapropiado"),
        NO_APARECE_PARTIDOS("No aparece a los partidos"),
        JUGADOR_VIOLENTO("Jugador violento"),
        OTRO("Otro");

        private final String displayName;

        ReportReason(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ReportStatus {
        PENDING,      // Pendiente de revisión
        UNDER_REVIEW, // En revisión
        RESOLVED,     // Resuelto
        DISMISSED     // Descartado
    }

    public enum ReportAction {
        NO_ACTION,           // Sin acción
        WARNING_SENT,        // Advertencia enviada
        USER_BANNED,         // Usuario baneado
        CONTENT_REMOVED,     // Contenido removido
        FALSE_REPORT         // Reporte falso (posible penalización al reportador)
    }

    // ==============================================
    // MÉTODOS DE NEGOCIO
    // ==============================================

    /**
     * Resolver el reporte
     */
    public void resolve(Usuario admin, ReportAction action, String notes) {
        this.status = ReportStatus.RESOLVED;
        this.resolvedAt = Instant.now();
        this.resolvedBy = admin;
        this.actionTaken = action;
        this.resolutionNotes = notes;
    }

    /**
     * Descartar el reporte
     */
    public void dismiss(Usuario admin, String notes) {
        this.status = ReportStatus.DISMISSED;
        this.resolvedAt = Instant.now();
        this.resolvedBy = admin;
        this.actionTaken = ReportAction.FALSE_REPORT;
        this.resolutionNotes = notes;
    }

    /**
     * Marcar como en revisión
     */
    public void markUnderReview() {
        this.status = ReportStatus.UNDER_REVIEW;
    }

    /**
     * Verificar si está pendiente
     */
    public boolean isPending() {
        return this.status == ReportStatus.PENDING || this.status == ReportStatus.UNDER_REVIEW;
    }
}
