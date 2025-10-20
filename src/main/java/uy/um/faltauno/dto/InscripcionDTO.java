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
    
    private UsuarioMinDTO usuario;
    
    private PartidoMinDTO partido;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    @JsonProperty("updated_at")
    private Instant updatedAt;
    
    @JsonProperty("fecha_aceptacion")
    private Instant fechaAceptacion;
    
    @JsonProperty("fecha_rechazo")
    private Instant fechaRechazo;
    
    @JsonProperty("fecha_cancelacion")
    private Instant fechaCancelacion;
    
    private String tiempoTranscurrido;
    private Boolean puedeCancelar;
    private Boolean puedeAceptar;
    private Boolean puedeRechazar;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartidoMinDTO {
        private UUID id;
        
        @JsonProperty("tipo_partido")
        private String tipoPartido;
        
        private String genero;
        private String fecha;
        private String hora;
        
        @JsonProperty("nombre_ubicacion")
        private String nombreUbicacion;
        
        private String estado;
        
        @JsonProperty("organizador_nombre")
        private String organizadorNombre;
    }
}