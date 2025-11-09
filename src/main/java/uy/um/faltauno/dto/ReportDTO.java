package uy.um.faltauno.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uy.um.faltauno.entity.Report;

import java.time.Instant;

/**
 * DTO para mostrar reportes en admin panel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    
    private String id;
    
    // Reporter info
    private UsuarioMinDTO reporter;
    
    // Reported user info
    private UsuarioMinDTO reportedUser;
    
    private Report.ReportReason reason;
    
    private String reasonDisplayName;
    
    private String description;
    
    private Report.ReportStatus status;
    
    private Instant createdAt;
    
    private Instant resolvedAt;
    
    // Resolver info
    private UsuarioMinDTO resolvedBy;
    
    private String resolutionNotes;
    
    private Report.ReportAction actionTaken;
}
