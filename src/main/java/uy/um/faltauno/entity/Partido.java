package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "partido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partido {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tipo_partido", nullable = false, length = 20)
    private String tipoPartido;

    @Column(name = "genero", nullable = false, length = 10)
    private String genero;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    @Column(name = "duracion_minutos", nullable = false)
    @Builder.Default
    private Integer duracionMinutos = 90;

    @Column(name = "nombre_ubicacion", nullable = false, length = 255)
    private String nombreUbicacion;

    @Column(name = "direccion_ubicacion", length = 255)
    private String direccionUbicacion;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    @Column(name = "cantidad_jugadores", nullable = false)
    private Integer cantidadJugadores;

    @Column(name = "precio_total", precision = 10, scale = 2)
    private BigDecimal precioTotal;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizador_id", nullable = false, columnDefinition = "uuid",
            foreignKey = @ForeignKey(name = "partido_organizador_fk"))
    private Usuario organizador;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(name = "nivel", length = 20)
    @Builder.Default
    private String nivel = "INTERMEDIO";

    // created_at es manejado por DEFAULT now() en la DB
}