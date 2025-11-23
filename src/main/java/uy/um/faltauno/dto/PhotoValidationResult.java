package uy.um.faltauno.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resultado de validación de fotos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoValidationResult {
    private boolean valid;
    private boolean hasFace;
    private int faceCount;
    private boolean isAppropriate;
    private Double confidence;
    private String message;
    private String reason;
}
