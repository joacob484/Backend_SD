package uy.um.faltauno.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mensaje")
@Data
@NoArgsConstructor
public class Mensaje {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "remitente_id", nullable = false)
    private UUID remitenteId;

    @Column(name = "destinatario_id", nullable = false)
    private UUID destinatarioId;

    @Column(name = "partido_id")
    private UUID partidoId;

    @Column(name = "contenido", nullable = false, length = 500)
    private String contenido;

    @Column(name = "leido", nullable = false)
    private boolean leido = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
