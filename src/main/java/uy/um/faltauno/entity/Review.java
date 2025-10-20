package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"partido_id", "usuario_que_califica_id", "usuario_calificado_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partido_id", nullable = false)
    private Partido partido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_que_califica_id", nullable = false)
    private Usuario usuarioQueCalifica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_calificado_id", nullable = false)
    private Usuario usuarioCalificado;

    @Column(nullable = false)
    private Integer nivel; // 1-5

    @Column(nullable = false)
    private Integer deportividad; // 1-5

    @Column(nullable = false)
    private Integer companerismo; // 1-5

    @Column(length = 300)
    private String comentario;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();  // ← Esto inicializa automáticamente
}