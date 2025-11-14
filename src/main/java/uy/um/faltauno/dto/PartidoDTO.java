package uy.um.faltauno.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import uy.um.faltauno.validation.OnCreate;
import uy.um.faltauno.validation.OnUpdate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * ✅ MEJORA: Validaciones con grupos OnCreate y OnUpdate
 * - OnCreate: Todos los campos requeridos son validados
 * - OnUpdate: Validaciones opcionales, solo se validan si están presentes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartidoDTO {

    private UUID id;

    @JsonProperty("tipo_partido")
    @NotBlank(message = "El tipo de partido es requerido", groups = OnCreate.class)
    @Pattern(regexp = "^(Fútbol 5|Fútbol 7|Fútbol 11|Fútbol Sala)$", 
             message = "Tipo de partido inválido")
    private String tipoPartido;
    
    @Pattern(regexp = "^(MIXTO|MASCULINO|FEMENINO)$", message = "Género inválido")
    private String genero;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @NotNull(message = "La fecha es requerida", groups = OnCreate.class)
    @FutureOrPresent(message = "La fecha debe ser hoy o en el futuro")
    private LocalDate fecha;
    
    @NotNull(message = "La hora es requerida", groups = OnCreate.class)
    private LocalTime hora;
    
    @JsonProperty("duracion_minutos")
    @Min(value = 30, message = "La duración mínima es 30 minutos")
    @Max(value = 180, message = "La duración máxima es 180 minutos")
    private Integer duracionMinutos;
    
    @JsonProperty("nombre_ubicacion")
    @NotBlank(message = "El nombre de la ubicación es requerido", groups = OnCreate.class)
    @Size(max = 100, message = "El nombre de la ubicación no puede exceder 100 caracteres")
    private String nombreUbicacion;
    
    @JsonProperty("direccion_ubicacion")
    @Size(max = 200, message = "La dirección no puede exceder 200 caracteres")
    private String direccionUbicacion;
    
    @DecimalMin(value = "-90.0", message = "Latitud inválida")
    @DecimalMax(value = "90.0", message = "Latitud inválida")
    private BigDecimal latitud;
    
    @DecimalMin(value = "-180.0", message = "Longitud inválida")
    @DecimalMax(value = "180.0", message = "Longitud inválida")
    private BigDecimal longitud;
    
    @JsonProperty("cantidad_jugadores")
    @NotNull(message = "La cantidad de jugadores es requerida", groups = OnCreate.class)
    @Min(value = 6, message = "Mínimo 6 jugadores")
    @Max(value = 22, message = "Máximo 22 jugadores")
    private Integer cantidadJugadores;
    
    @JsonProperty("precio_total")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    private BigDecimal precioTotal;
    
    @JsonProperty("precio_por_jugador")
    private BigDecimal precioPorJugador;
    
    private String descripcion;
    
    private String estado;
    
    private String nivel;
    
    @JsonProperty("jugadores_actuales")
    private Integer jugadoresActuales;
    
    @JsonProperty("organizador_id")
    private UUID organizadorId;
    
    // Información del organizador
    private UsuarioMinDTO organizador;
    
    // Lista de jugadores inscritos
    private List<UsuarioMinDTO> jugadores;
    
    // Solicitudes pendientes (solo para organizador)
    @JsonProperty("solicitudes_pendientes")
    private List<InscripcionDTO> solicitudesPendientes;
    
    // Timestamp de creación
    @JsonProperty("created_at")
    private Instant createdAt;
    
    /**
     * Calcula el precio por jugador automáticamente
     */
    public BigDecimal getPrecioPorJugador() {
        if (precioPorJugador != null) {
            return precioPorJugador;
        }
        
        if (precioTotal != null && cantidadJugadores != null && cantidadJugadores > 0) {
            return precioTotal.divide(
                BigDecimal.valueOf(cantidadJugadores), 
                2, 
                java.math.RoundingMode.HALF_UP
            );
        }
        
        return BigDecimal.ZERO;
    }
}