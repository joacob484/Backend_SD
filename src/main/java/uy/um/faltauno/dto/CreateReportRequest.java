package uy.um.faltauno.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uy.um.faltauno.entity.Report;

/**
 * DTO para crear un nuevo reporte
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {
    
    private String reportedUserId;
    
    private Report.ReportReason reason;
    
    private String description;
}
