package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registra la última vez que un usuario visitó el chat de un partido
 * Usado para calcular mensajes no leídos de forma confiable
 */
@Entity
@Table(name = "chat_visits", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "partido_id"}),
       indexes = {
           @Index(name = "idx_chat_visit_usuario", columnList = "usuario_id"),
           @Index(name = "idx_chat_visit_partido", columnList = "partido_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatVisit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partido_id", nullable = false)
    private Partido partido;
    
    /**
     * Última vez que el usuario visitó este chat
     * Se actualiza cada vez que el usuario abre el chat
     */
    @Column(name = "last_visit_at", nullable = false)
    private LocalDateTime lastVisitAt;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
