package uy.um.faltauno.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
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
    
    private Double latitud;
    
    private Double longitud;
    
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
    
    // Información del organizador (para mostrar en el frontend)
    private UsuarioMinDTO organizador;
    
    // Lista de jugadores inscritos y aceptados
    private List<UsuarioMinDTO> jugadores;
    
    // Solicitudes pendientes (solo para organizador)
    @JsonProperty("solicitudes_pendientes")
    private List<InscripcionDTO> solicitudesPendientes;
    
    @JsonProperty("created_at")
    private String createdAt;
    
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