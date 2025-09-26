package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "partido")
public class Partido {

    @Id
    @GeneratedValue
    private UUID id;

    private String tipoPartido; // F5, F7, ...
    private String genero; // Mixto, Hombres, Mujeres
    private LocalDate fecha;
    private LocalTime hora;
    private Integer duracionMinutos;
    private String nombreUbicacion;
    private String direccionUbicacion;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String descripcion;
    private Integer maxJugadores;

    @Builder.Default
    private Integer jugadoresActuales = 0;
    @Builder.Default
    private BigDecimal precioTotal = BigDecimal.ZERO;
    @Builder.Default
    private String estado = "PENDIENTE";

    @ManyToOne
    @JoinColumn(name = "organizador_id", nullable = false)
    private Usuario organizador;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "partido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Inscripcion> inscripciones;

    @OneToMany(mappedBy = "partido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews;
}