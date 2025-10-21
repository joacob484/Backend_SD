package uy.um.faltauno.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartidoDTO {

    private UUID id;

    @JsonProperty("tipo_partido")
    private String tipoPartido;
    
    private String genero;
    
    private LocalDate fecha;
    
    private LocalTime hora;
    
    @JsonProperty("duracion_minutos")
    private Integer duracionMinutos;
    
    @JsonProperty("nombre_ubicacion")
    private String nombreUbicacion;
    
    @JsonProperty("direccion_ubicacion")
    private String direccionUbicacion;
    
    private BigDecimal latitud;
    
    private BigDecimal longitud;
    
    @JsonProperty("cantidad_jugadores")
    private Integer cantidadJugadores;
    
    @JsonProperty("precio_total")
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