package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
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
    @JdbcTypeCode(SqlTypes.UUID) // <-- fuerza tipo UUID en Hibernate 6
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

    @Column(name = "latitud", precision = 18, scale = 10)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 18, scale = 10)
    private BigDecimal longitud;

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
    private String estado = "DISPONIBLE";

    @Column(name = "nivel", length = 20)
    @Builder.Default
    private String nivel = "INTERMEDIO";

    /**
     * ✅ OPTIMIZACIÓN: Control de concurrencia optimista
     * Previene race conditions cuando múltiples usuarios intentan inscribirse simultáneamente
     * Si hay conflicto, Hibernate lanza OptimisticLockException que debe ser manejada
     */
    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}