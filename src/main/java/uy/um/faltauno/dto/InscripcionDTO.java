package uy.um.faltauno.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InscripcionDTO {
    
    private UUID id;
    
    @JsonProperty("partido_id")
    private UUID partidoId;
    
    @JsonProperty("usuario_id")
    private UUID usuarioId;
    
    private String estado;
    
    // Información del usuario (para mostrar en listas)
    private UsuarioMinDTO usuario;
    
    // Fecha de solicitud formateada
    @JsonProperty("fecha_solicitud")
    private String fechaSolicitud;
}