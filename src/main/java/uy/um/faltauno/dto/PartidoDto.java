package uy.um.faltauno.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class PartidoDto {
    private UUID id;
    private String tipoPartido;        // F5, F7, etc
    private String genero;             // Mixto, Hombres, Mujeres
    private LocalDate fecha;
    private LocalTime hora;
    private Integer duracionMinutos;
    private String nombreUbicacion;
    private String direccionUbicacion;
    private BigDecimal precioTotal;
    private Integer maxJugadores;
    private String descripcion;
    private UUID organizadorId;
}