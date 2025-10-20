package uy.um.faltauno.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.Instant;
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
    
    // Timestamp de creación
    @JsonProperty("created_at")
    private Instant createdAt;

    // Representación mínima de Partido usada en InscripcionDTO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PartidoMinDTO {
        private java.util.UUID id;
        private String tipoPartido;
        private String genero;
        private String fecha;
        private String hora;
        private String nombreUbicacion;
        private String estado;
        private String organizadorNombre;
    }
}