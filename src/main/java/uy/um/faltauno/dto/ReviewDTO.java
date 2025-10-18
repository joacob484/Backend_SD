package uy.um.faltauno.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ReviewDTO {
    
    private UUID id;
    
    @NotNull(message = "El ID del partido es requerido")
    @JsonProperty("partido_id")
    private UUID partidoId;
    
    @JsonProperty("usuario_que_califica_id")
    private UUID usuarioQueCalificaId;
    
    @NotNull(message = "El ID del usuario calificado es requerido")
    @JsonProperty("usuario_calificado_id")
    private UUID usuarioCalificadoId;
    
    @NotNull(message = "La calificación de nivel es requerida")
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    private Integer nivel;
    
    @NotNull(message = "La calificación de deportividad es requerida")
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    private Integer deportividad;
    
    @NotNull(message = "La calificación de compañerismo es requerida")
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    private Integer companerismo;
    
    @Size(max = 300, message = "El comentario no puede exceder 300 caracteres")
    private String comentario;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    // Información adicional para mostrar
    @JsonProperty("usuario_calificado")
    private UsuarioMinDTO usuarioCalificado;
    
    @JsonProperty("promedio")
    private Double promedio;
    
    // Calcular promedio
    public Double getPromedio() {
        if (nivel != null && deportividad != null && companerismo != null) {
            return (nivel + deportividad + companerismo) / 3.0;
        }
        return promedio;
    }
}