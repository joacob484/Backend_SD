package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mensaje")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "partido_id", nullable = false)
    private UUID partidoId;

    @Column(name = "remitente_id", nullable = false)
    private UUID remitenteId;

    @Column(name = "destinatario_id")
    private UUID destinatarioId; // null para mensajes grupales

    @Column(name = "contenido", nullable = false, length = 500)
    private String contenido;

    @Column(name = "leido", nullable = false)
    @Builder.Default
    private Boolean leido = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}