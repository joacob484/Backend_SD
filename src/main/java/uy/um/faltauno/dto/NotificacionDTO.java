package uy.um.faltauno.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionDTO {
    
    private UUID id;
    
    @JsonProperty("usuario_id")
    private UUID usuarioId;
    
    private String tipo;
    
    private String titulo;
    
    private String mensaje;
    
    @JsonProperty("entidad_id")
    private UUID entidadId;
    
    @JsonProperty("entidad_tipo")
    private String entidadTipo;
    
    @JsonProperty("url_accion")
    private String urlAccion;
    
    private Boolean leida;
    
    @JsonProperty("fecha_lectura")
    private Instant fechaLectura;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    private String prioridad;
}
