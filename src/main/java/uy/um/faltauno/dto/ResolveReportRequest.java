package uy.um.faltauno.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uy.um.faltauno.entity.Report;

/**
 * DTO para resolver un reporte
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveReportRequest {
    
    private Report.ReportAction action;
    
    private String notes;
    
    /**
     * Si action = USER_BANNED, especificar si banear al usuario
     */
    private Boolean banUser;
    
    /**
     * Razón del baneo (si banUser = true)
     */
    private String banReason;
    
    /**
     * Duración del baneo en días (si banUser = true)
     * null o 0 = permanente
     */
    private Integer banDuration;
}
