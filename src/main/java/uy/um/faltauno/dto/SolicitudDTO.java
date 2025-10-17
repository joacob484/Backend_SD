package uy.um.faltauno.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudDTO {
    
    @NotNull(message = "El ID del partido es requerido")
    private UUID partidoId;
    
    @NotNull(message = "El ID del usuario es requerido")
    private UUID usuarioId;
    
    private String comentario; // opcional: mensaje al organizador
}