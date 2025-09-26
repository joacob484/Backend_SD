package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "inscripcion", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"partido_id", "usuario_id"})
})
public class Inscripcion {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partido_id", nullable = false)
    private Partido partido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Builder.Default
    @Column(nullable = false)
    private String estado = "PENDIENTE";

    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}