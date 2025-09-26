package uy.um.faltauno.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import lombok.Data;

@Data
public class PartidoDTO {

    private UUID id;
    private String tipoPartido;
    private String genero;
    private LocalDate fecha;
    private LocalTime hora;
    private Integer duracionMinutos;
    private String nombreUbicacion;
    private String direccionUbicacion;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String descripcion;
    private Integer maxJugadores;
    private Integer jugadoresActuales;
    private BigDecimal precioTotal;
    private String estado;

    private UUID organizadorId;      // solo el id del organizador
    private String organizadorNombre; // opcional, para mostrar en la UI
}