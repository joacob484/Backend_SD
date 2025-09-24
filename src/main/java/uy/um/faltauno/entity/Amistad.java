package uy.um.faltauno.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "amistad", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "amigo_id"}))
@Data
@NoArgsConstructor
public class Amistad {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "amigo_id", nullable = false)
    private UUID amigoId;

    @Column(nullable = false)
    private String estado = "PENDIENTE";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
