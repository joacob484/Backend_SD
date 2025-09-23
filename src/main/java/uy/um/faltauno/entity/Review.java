package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "review")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "partido_id", nullable = false)
    private Partido partido;

    @ManyToOne
    @JoinColumn(name = "usuario_que_califica_id", nullable = false)
    private Usuario usuarioQueCalifica;

    @ManyToOne
    @JoinColumn(name = "usuario_calificado_id", nullable = false)
    private Usuario usuarioCalificado;

    @Column(nullable = false)
    private Integer nivel; // 1 a 5

    @Column(nullable = false)
    private Integer deportividad; // 1 a 5

    @Column(nullable = false)
    private Integer companerismo; // 1 a 5

    @Column(length = 300)
    private String comentario;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
