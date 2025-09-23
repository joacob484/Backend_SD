package uy.um.faltauno.dto;
import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InscripcionDTO {
    private UUID id;
    private UUID partidoId;
    private UUID usuarioId;
    private String estado;
}
