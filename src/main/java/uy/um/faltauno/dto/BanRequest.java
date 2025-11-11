package uy.um.faltauno.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitud de baneo de usuario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BanRequest {
    
    /**
     * Razón del baneo
     */
    private String reason;
    
    /**
     * Duración del baneo en días (null = permanente)
     */
    private Integer durationDays;
    
    /**
     * Tipo de baneo
     */
    private BanType type;
    
    public enum BanType {
        PERMANENT,      // Baneo permanente
        TEMPORARY,      // Baneo temporal
        WARNING         // Advertencia (sin baneo real)
    }
}
