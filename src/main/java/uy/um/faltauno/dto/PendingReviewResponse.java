package uy.um.faltauno.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingReviewResponse {
    private String partido_id;
    private String tipo_partido;
    private String fecha;
    private String nombre_ubicacion;
    private List<UsuarioMinDTO> jugadores_pendientes;

    public void setPartido_id(UUID partidoId) {
        this.partido_id = partidoId.toString();
    }
}