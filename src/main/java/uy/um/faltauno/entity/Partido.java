package uy.um.faltauno.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "partido")
public class Partido {

    @Id
    @GeneratedValue
    @UuidGenerator // genera UUID del lado de la app (compatible con columna uuid de la DB)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tipo_partido", nullable = false, length = 5)
    private String tipoPartido;

    @Column(name = "genero", nullable = false, length = 10)
    private String genero;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

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

    @Column(name = "estado", nullable = false)
    private String estado = "PENDIENTE";

    @Column(name = "nivel")
    private String nivel = "INTERMEDIO";
        // created_at existe en la DB y lo maneja DEFAULT now()

    public Partido() { }

    public Partido(UUID id, String tipoPartido, String genero, LocalDate fecha, LocalTime hora,
                   Integer duracionMinutos, String nombreUbicacion, String direccionUbicacion,
                   Double latitud, Double longitud, Integer cantidadJugadores, BigDecimal precioTotal,
                   String descripcion, Usuario organizador) {
        this.id = id;
        this.tipoPartido = tipoPartido;
        this.genero = genero;
        this.fecha = fecha;
        this.hora = hora;
        this.duracionMinutos = duracionMinutos;
        this.nombreUbicacion = nombreUbicacion;
        this.direccionUbicacion = direccionUbicacion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.cantidadJugadores = cantidadJugadores;
        this.precioTotal = precioTotal;
        this.descripcion = descripcion;
        this.organizador = organizador;
    }

    // getters / setters
    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public String getTipoPartido() {
        return tipoPartido;
    }
    public void setTipoPartido(String tipoPartido) {
        this.tipoPartido = tipoPartido;
    }
    public String getGenero() {
        return genero;
    }
    public void setGenero(String genero) {
        this.genero = genero;
    }
    public LocalDate getFecha() {
        return fecha;
    }
    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
    public LocalTime getHora() {
        return hora;
    }
    public void setHora(LocalTime hora) {
        this.hora = hora;
    }
    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }
    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }
    public String getNombreUbicacion() {
        return nombreUbicacion;
    }
    public void setNombreUbicacion(String nombreUbicacion) {
        this.nombreUbicacion = nombreUbicacion;
    }
    public String getDireccionUbicacion() {
        return direccionUbicacion;
    }
    public void setDireccionUbicacion(String direccionUbicacion) {
        this.direccionUbicacion = direccionUbicacion;
    }
    public Double getLatitud() {
        return latitud;
    }
    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }
    public Double getLongitud() {
        return longitud;
    }
    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }
    public Integer getCantidadJugadores() {
        return cantidadJugadores;
    }
    public void setCantidadJugadores(Integer cantidadJugadores) {
        this.cantidadJugadores = cantidadJugadores;
    }
    public BigDecimal getPrecioTotal() {
        return precioTotal;
    }
    public void setPrecioTotal(BigDecimal precioTotal) {
        this.precioTotal = precioTotal;
    }
    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    public Usuario getOrganizador() {
        return organizador;
    }
    public void setOrganizador(Usuario organizador) {
        this.organizador = organizador;
    }
}
