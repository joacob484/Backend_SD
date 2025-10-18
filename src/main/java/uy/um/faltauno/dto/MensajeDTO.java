package uy.um.faltauno.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeDTO {
    
    private UUID id;
    
    @JsonProperty("usuario_id")
    private UUID usuarioId;
    
    @JsonProperty("partido_id")
    private UUID partidoId;
    
    @NotBlank(message = "El contenido del mensaje no puede estar vacío")
    @Size(max = 500, message = "El mensaje no puede exceder 500 caracteres")
    private String contenido;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    private Boolean leido;
    
    // Información del usuario (para mostrar en el chat)
    private UsuarioMinDTO usuario;
}