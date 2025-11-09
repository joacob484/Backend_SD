package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.*;
import uy.um.faltauno.entity.Report;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.security.RequireAdmin;
import uy.um.faltauno.service.ReportService;
import uy.um.faltauno.service.UsuarioService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador para gestión de reportes
 */
@RestController
@RequestMapping("/api/reports")
@Slf4j
@RequiredArgsConstructor
public class ReportController {
    
    private final ReportService reportService;
    private final UsuarioService usuarioService;
    
    /**
     * POST /api/reports
     * Crear un reporte
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReportDTO>> createReport(
            @AuthenticationPrincipal Usuario user,
            @RequestBody CreateReportRequest request) {
        try {
            log.info("[REPORT] Usuario {} creando reporte", user.getEmail());
            
            ReportDTO report = reportService.createReport(user.getId().toString(), request);
            
            return ResponseEntity.ok(new ApiResponse<>(report, 
                    "Reporte enviado correctamente. Será revisado por nuestro equipo.", 
                    true));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("[REPORT] Error al crear reporte: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("[REPORT] Error inesperado al crear reporte", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al crear el reporte", false));
        }
    }
    
    /**
     * GET /api/reports/can-report
     * Verificar si el usuario puede hacer más reportes este mes
     */
    @GetMapping("/can-report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> canReport(
            @AuthenticationPrincipal Usuario user) {
        try {
            boolean canReport = reportService.canUserReport(user.getId().toString());
            int remaining = reportService.getRemainingReports(user.getId().toString());
            
            Map<String, Object> data = new HashMap<>();
            data.put("canReport", canReport);
            data.put("remainingReports", remaining);
            data.put("maxReportsPerMonth", 5);
            
            return ResponseEntity.ok(new ApiResponse<>(data, 
                    canReport ? "Puedes hacer reportes" : "Has alcanzado el límite de reportes mensuales", 
                    true));
        } catch (Exception e) {
            log.error("[REPORT] Error al verificar límite de reportes", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al verificar límite de reportes", false));
        }
    }
    
    /**
     * GET /api/reports/admin/all
     * Obtener todos los reportes (solo admin)
     */
    @GetMapping("/admin/all")
    @RequireAdmin
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getAllReports(
            @AuthenticationPrincipal Usuario admin) {
        try {
            log.info("[REPORT] Admin {} obteniendo todos los reportes", admin.getEmail());
            
            List<ReportDTO> reports = reportService.getAllReports();
            
            return ResponseEntity.ok(new ApiResponse<>(reports, 
                    String.format("Total: %d reportes", reports.size()), 
                    true));
        } catch (Exception e) {
            log.error("[REPORT] Error al obtener reportes", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al obtener reportes", false));
        }
    }
    
    /**
     * GET /api/reports/admin/pending
     * Obtener reportes pendientes (solo admin)
     */
    @GetMapping("/admin/pending")
    @RequireAdmin
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getPendingReports(
            @AuthenticationPrincipal Usuario admin) {
        try {
            log.info("[REPORT] Admin {} obteniendo reportes pendientes", admin.getEmail());
            
            List<ReportDTO> reports = reportService.getPendingReports();
            
            return ResponseEntity.ok(new ApiResponse<>(reports, 
                    String.format("Reportes pendientes: %d", reports.size()), 
                    true));
        } catch (Exception e) {
            log.error("[REPORT] Error al obtener reportes pendientes", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al obtener reportes pendientes", false));
        }
    }
    
    /**
     * GET /api/reports/admin/count
     * Contar reportes pendientes (solo admin)
     */
    @GetMapping("/admin/count")
    @RequireAdmin
    public ResponseEntity<ApiResponse<Map<String, Long>>> countPendingReports(
            @AuthenticationPrincipal Usuario admin) {
        try {
            long count = reportService.countPendingReports();
            
            Map<String, Long> data = new HashMap<>();
            data.put("pendingCount", count);
            
            return ResponseEntity.ok(new ApiResponse<>(data, 
                    String.format("%d reportes pendientes", count), 
                    true));
        } catch (Exception e) {
            log.error("[REPORT] Error al contar reportes pendientes", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al contar reportes", false));
        }
    }
    
    /**
     * PUT /api/reports/admin/{id}/resolve
     * Resolver un reporte (solo admin)
     */
    @PutMapping("/admin/{id}/resolve")
    @RequireAdmin
    public ResponseEntity<ApiResponse<ReportDTO>> resolveReport(
            @AuthenticationPrincipal Usuario admin,
            @PathVariable String id,
            @RequestBody ResolveReportRequest request) {
        try {
            log.info("[REPORT] Admin {} resolviendo reporte {}", admin.getEmail(), id);
            
            // Si la acción es banear usuario, ejecutar el baneo
            if (request.getAction() == Report.ReportAction.USER_BANNED && 
                Boolean.TRUE.equals(request.getBanUser())) {
                
                // Primero obtener el reporte para saber a quién banear
                List<ReportDTO> reports = reportService.getAllReports();
                ReportDTO report = reports.stream()
                        .filter(r -> r.getId().equals(id))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado"));
                
                String banReason = request.getBanReason() != null ? 
                        request.getBanReason() : "Reportado por: " + report.getReason().getDisplayName();
                
                usuarioService.banUser(report.getReportedUser().getId(), admin.getId().toString(), banReason);
            }
            
            ReportDTO resolvedReport = reportService.resolveReport(id, admin.getId().toString(), request);
            
            return ResponseEntity.ok(new ApiResponse<>(resolvedReport, 
                    "Reporte resuelto correctamente", 
                    true));
        } catch (IllegalArgumentException e) {
            log.error("[REPORT] Error al resolver reporte: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("[REPORT] Error inesperado al resolver reporte", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al resolver el reporte", false));
        }
    }
    
    /**
     * PUT /api/reports/admin/{id}/dismiss
     * Descartar un reporte (solo admin)
     */
    @PutMapping("/admin/{id}/dismiss")
    @RequireAdmin
    public ResponseEntity<ApiResponse<ReportDTO>> dismissReport(
            @AuthenticationPrincipal Usuario admin,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            log.info("[REPORT] Admin {} descartando reporte {}", admin.getEmail(), id);
            
            String notes = body.getOrDefault("notes", "Reporte descartado sin notas");
            
            ReportDTO dismissedReport = reportService.dismissReport(id, admin.getId().toString(), notes);
            
            return ResponseEntity.ok(new ApiResponse<>(dismissedReport, 
                    "Reporte descartado", 
                    true));
        } catch (IllegalArgumentException e) {
            log.error("[REPORT] Error al descartar reporte: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("[REPORT] Error inesperado al descartar reporte", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al descartar el reporte", false));
        }
    }
    
    /**
     * GET /api/reports/admin/user/{userId}
     * Obtener reportes contra un usuario específico (solo admin)
     */
    @GetMapping("/admin/user/{userId}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getReportsAgainstUser(
            @AuthenticationPrincipal Usuario admin,
            @PathVariable String userId) {
        try {
            log.info("[REPORT] Admin {} obteniendo reportes contra usuario {}", admin.getEmail(), userId);
            
            List<ReportDTO> reports = reportService.getReportsAgainstUser(userId);
            
            return ResponseEntity.ok(new ApiResponse<>(reports, 
                    String.format("Reportes contra usuario: %d", reports.size()), 
                    true));
        } catch (Exception e) {
            log.error("[REPORT] Error al obtener reportes del usuario", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al obtener reportes", false));
        }
    }
}
