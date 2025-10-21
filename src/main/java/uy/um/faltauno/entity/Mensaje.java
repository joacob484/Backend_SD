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

    @ManyToOne(optional = true) // <- ahora opcional
    @JoinColumn(name = "partido_id", nullable = true)
    private Partido partido;

    @ManyToOne(optional = false)
    @JoinColumn(name = "remitente_id", nullable = false)
    private Usuario remitente;

    @ManyToOne(optional = true) // <- ahora opcional
    @JoinColumn(name = "destinatario_id", nullable = true)
    private Usuario destinatario;

    @Column(name = "contenido", length = 500, nullable = false)
    private String contenido;

    @Column(name = "leido", nullable = false)
    @Builder.Default
    private Boolean leido = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}