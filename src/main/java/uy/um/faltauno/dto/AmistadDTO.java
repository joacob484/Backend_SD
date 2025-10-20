package uy.um.faltauno.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmistadDTO {
    
    private UUID id;
    
    @JsonProperty("usuario_id")
    private UUID usuarioId;
    
    @JsonProperty("amigo_id")
    private UUID amigoId;
    
    private String estado;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    // Información del usuario (quien envió o recibió)
    private UsuarioMinDTO usuario;
    
    // Información del amigo
    private UsuarioMinDTO amigo;
    
    // Campos calculados
    private String tiempoTranscurrido;
    private Boolean puedeAceptar;
    private Boolean puedeRechazar;
    private Boolean puedeCancelar;
}