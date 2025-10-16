package uy.um.faltauno.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class PartidoDTO {

    private UUID id;

    private String tipoPartido;
    private String genero;
    private LocalDate fecha;
    private LocalTime hora;
    private Integer duracionMinutos;
    private String nombreUbicacion;
    private String direccionUbicacion;
    private Double latitud;
    private Double longitud;
    private Integer cantidadJugadores;
    private BigDecimal precioTotal;
    private String descripcion;

    private UUID organizadorId;        // FK
    private String organizadorNombre;  // para mostrar en el front

    public PartidoDTO() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTipoPartido() { return tipoPartido; }
    public void setTipoPartido(String tipoPartido) { this.tipoPartido = tipoPartido; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }

    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public String getNombreUbicacion() { return nombreUbicacion; }
    public void setNombreUbicacion(String nombreUbicacion) { this.nombreUbicacion = nombreUbicacion; }

    public String getDireccionUbicacion() { return direccionUbicacion; }
    public void setDireccionUbicacion(String direccionUbicacion) { this.direccionUbicacion = direccionUbicacion; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public Integer getCantidadJugadores() { return cantidadJugadores; }
    public void setCantidadJugadores(Integer cantidadJugadores) { this.cantidadJugadores = cantidadJugadores; }

    public BigDecimal getPrecioTotal() { return precioTotal; }
    public void setPrecioTotal(BigDecimal precioTotal) { this.precioTotal = precioTotal; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public UUID getOrganizadorId() { return organizadorId; }
    public void setOrganizadorId(UUID organizadorId) { this.organizadorId = organizadorId; }

    public String getOrganizadorNombre() { return organizadorNombre; }
    public void setOrganizadorNombre(String organizadorNombre) { this.organizadorNombre = organizadorNombre; }
}
