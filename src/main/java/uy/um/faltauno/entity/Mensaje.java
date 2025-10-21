package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mensaje", indexes = {
    @Index(name = "idx_mensaje_partido", columnList = "partido_id"),
    @Index(name = "idx_mensaje_remitente", columnList = "remitente_id"),
    @Index(name = "idx_mensaje_destinatario", columnList = "destinatario_id"),
    @Index(name = "idx_mensaje_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"remitente", "destinatario", "partido"})
@EqualsAndHashCode(of = "id")
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * OPCIÓN 1: Usar UUID directamente (más simple, recomendado)
     */
    @Column(name = "partido_id")
    private UUID partidoId;

    @Column(name = "remitente_id", nullable = false)
    private UUID remitenteId;

    @Column(name = "destinatario_id")
    private UUID destinatarioId;

    /**
     * OPCIÓN 2: Si necesitas cargar las entidades relacionadas
     * Descomentar SOLO si realmente necesitas acceso a las entidades completas
     */
    /*
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partido_id", insertable = false, updatable = false)
    private Partido partido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remitente_id", insertable = false, updatable = false)
    private Usuario remitente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id", insertable = false, updatable = false)
    private Usuario destinatario;
    */

    @Column(name = "contenido", length = 500, nullable = false)
    private String contenido;

    @Column(name = "leido", nullable = false)
    @Builder.Default
    private Boolean leido = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    // Métodos de negocio
    
    /**
     * Verifica si es un mensaje grupal (chat de partido)
     */
    public boolean isMensajeGrupal() {
        return partidoId != null && destinatarioId == null;
    }

    /**
     * Verifica si es un mensaje directo
     */
    public boolean isMensajeDirecto() {
        return destinatarioId != null;
    }

    /**
     * Marca el mensaje como leído
     */
    public void marcarComoLeido() {
        this.leido = true;
    }

    // Callbacks JPA
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (leido == null) {
            leido = false;
        }
    }
}