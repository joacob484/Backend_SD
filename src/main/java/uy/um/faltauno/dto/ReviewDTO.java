package uy.um.faltauno.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {

    private UUID id;
    private UUID partidoId;
    private UUID usuarioQueCalificaId;
    private UUID usuarioCalificadoId;
    private Integer nivel;
    private Integer deportividad;
    private Integer companerismo;
    private String comentario;
}
