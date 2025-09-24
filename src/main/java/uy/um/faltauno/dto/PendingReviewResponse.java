package uy.um.faltauno.dto;

import java.util.List;
import java.util.UUID;

public class PendingReviewResponse {
    private String partido_id;
    private String tipo_partido;
    private String fecha;
    private String nombre_ubicacion;
    private List<UsuarioMinDTO> jugadores_pendientes;

    public void setPartido_id(UUID partidoId) {
        this.partido_id = partidoId.toString(); // convierte UUID a String
    }

    // otros setters y getters
    public String getPartido_id() { return partido_id; }
    public void setTipo_partido(String tipo_partido) { this.tipo_partido = tipo_partido; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public void setNombre_ubicacion(String nombre_ubicacion) { this.nombre_ubicacion = nombre_ubicacion; }
    public void setJugadores_pendientes(List<UsuarioMinDTO> jugadores_pendientes) { this.jugadores_pendientes = jugadores_pendientes; }
}