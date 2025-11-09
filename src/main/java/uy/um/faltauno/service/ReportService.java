package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.um.faltauno.dto.*;
import uy.um.faltauno.entity.Report;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.ReportRepository;
import uy.um.faltauno.repository.UsuarioRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UsuarioRepository usuarioRepository;
    
    private static final int MAX_REPORTS_PER_MONTH = 5;

    /**
     * Crear un nuevo reporte
     */
    @Transactional
    public ReportDTO createReport(String reporterId, CreateReportRequest request) {
        log.info("[REPORT] Usuario {} reportando a {}", reporterId, request.getReportedUserId());
        
        UUID reporterUuid = UUID.fromString(reporterId);
        UUID reportedUuid = UUID.fromString(request.getReportedUserId());
        
        // Validar que no se reporte a sí mismo
        if (reporterUuid.equals(reportedUuid)) {
            throw new IllegalArgumentException("No puedes reportarte a ti mismo");
        }
        
        // Buscar usuarios
        Usuario reporter = usuarioRepository.findByIdIncludingDeleted(reporterUuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuario reportador no encontrado"));
        
        Usuario reportedUser = usuarioRepository.findByIdIncludingDeleted(reportedUuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuario reportado no encontrado"));
        
        // Validar límite de reportes por mes
        Instant oneMonthAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        long reportsThisMonth = reportRepository.countByReporterIdAndCreatedAtAfter(reporterUuid, oneMonthAgo);
        
        if (reportsThisMonth >= MAX_REPORTS_PER_MONTH) {
            log.warn("[REPORT] Usuario {} ha alcanzado el límite de reportes mensuales ({}/{})", 
                    reporterId, reportsThisMonth, MAX_REPORTS_PER_MONTH);
            throw new IllegalStateException("Has alcanzado el límite de " + MAX_REPORTS_PER_MONTH + " reportes por mes");
        }
        
        // Validar que no exista un reporte activo entre estos usuarios
        if (reportRepository.existsActiveReportBetween(reporterUuid, reportedUuid)) {
            log.warn("[REPORT] Ya existe un reporte activo de {} contra {}", reporterId, request.getReportedUserId());
            throw new IllegalStateException("Ya has reportado a este usuario. El reporte está siendo procesado.");
        }
        
        // Crear el reporte
        Report report = Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(Report.ReportStatus.PENDING)
                .build();
        
        report = reportRepository.save(report);
        
        log.info("[REPORT] Reporte creado: {} reportó a {} por {}", 
                reporter.getEmail(), reportedUser.getEmail(), request.getReason());
        
        return toDTO(report);
    }

    /**
     * Obtener todos los reportes (para admin)
     */
    @Transactional(readOnly = true)
    public List<ReportDTO> getAllReports() {
        log.info("[REPORT] Admin obteniendo todos los reportes");
        return reportRepository.findAllWithDetails().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener reportes pendientes
     */
    @Transactional(readOnly = true)
    public List<ReportDTO> getPendingReports() {
        log.info("[REPORT] Obteniendo reportes pendientes");
        return reportRepository.findPendingReportsWithDetails().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener reportes por estado
     */
    @Transactional(readOnly = true)
    public List<ReportDTO> getReportsByStatus(Report.ReportStatus status) {
        log.info("[REPORT] Obteniendo reportes con estado: {}", status);
        return reportRepository.findByStatus(status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Contar reportes pendientes
     */
    @Transactional(readOnly = true)
    public long countPendingReports() {
        return reportRepository.countPendingReports();
    }

    /**
     * Resolver un reporte
     */
    @Transactional
    public ReportDTO resolveReport(String reportId, String adminId, ResolveReportRequest request) {
        log.info("[REPORT] Admin {} resolviendo reporte {}", adminId, reportId);
        
        UUID adminUuid = UUID.fromString(adminId);
        UUID reportUuid = UUID.fromString(reportId);
        
        Usuario admin = usuarioRepository.findById(adminUuid)
                .orElseThrow(() -> new IllegalArgumentException("Admin no encontrado"));
        
        Report report = reportRepository.findById(reportUuid)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado"));
        
        // Resolver el reporte
        report.resolve(admin, request.getAction(), request.getNotes());
        
        // Si la acción es banear al usuario y se especificó banUser
        if (request.getAction() == Report.ReportAction.USER_BANNED && 
            Boolean.TRUE.equals(request.getBanUser())) {
            
            Usuario reportedUser = report.getReportedUser();
            if (reportedUser.getBannedAt() == null) {
                log.warn("[REPORT] Baneando usuario {} por reporte {}", reportedUser.getEmail(), reportId);
                // El baneo se manejará en UsuarioService
            }
        }
        
        report = reportRepository.save(report);
        
        log.info("[REPORT] Reporte {} resuelto con acción: {}", reportId, request.getAction());
        
        return toDTO(report);
    }

    /**
     * Descartar un reporte
     */
    @Transactional
    public ReportDTO dismissReport(String reportId, String adminId, String notes) {
        log.info("[REPORT] Admin {} descartando reporte {}", adminId, reportId);
        
        UUID adminUuid = UUID.fromString(adminId);
        UUID reportUuid = UUID.fromString(reportId);
        
        Usuario admin = usuarioRepository.findById(adminUuid)
                .orElseThrow(() -> new IllegalArgumentException("Admin no encontrado"));
        
        Report report = reportRepository.findById(reportUuid)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado"));
        
        report.dismiss(admin, notes);
        
        report = reportRepository.save(report);
        
        log.info("[REPORT] Reporte {} descartado", reportId);
        
        return toDTO(report);
    }

    /**
     * Obtener reportes de un usuario
     */
    @Transactional(readOnly = true)
    public List<ReportDTO> getReportsAgainstUser(String userId) {
        UUID uuid = UUID.fromString(userId);
        return reportRepository.findByReportedUserId(uuid).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Verificar si un usuario puede reportar a otro
     */
    @Transactional(readOnly = true)
    public boolean canUserReport(String reporterId) {
        UUID reporterUuid = UUID.fromString(reporterId);
        Instant oneMonthAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        long reportsThisMonth = reportRepository.countByReporterIdAndCreatedAtAfter(reporterUuid, oneMonthAgo);
        
        return reportsThisMonth < MAX_REPORTS_PER_MONTH;
    }

    /**
     * Obtener cuántos reportes le quedan a un usuario este mes
     */
    @Transactional(readOnly = true)
    public int getRemainingReports(String reporterId) {
        UUID reporterUuid = UUID.fromString(reporterId);
        Instant oneMonthAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        long reportsThisMonth = reportRepository.countByReporterIdAndCreatedAtAfter(reporterUuid, oneMonthAgo);
        
        return Math.max(0, MAX_REPORTS_PER_MONTH - (int) reportsThisMonth);
    }

    /**
     * Convertir Report a DTO
     */
    private ReportDTO toDTO(Report report) {
        return ReportDTO.builder()
                .id(report.getId().toString())
                .reporter(toMinDTO(report.getReporter()))
                .reportedUser(toMinDTO(report.getReportedUser()))
                .reason(report.getReason())
                .reasonDisplayName(report.getReason().getDisplayName())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .resolvedBy(report.getResolvedBy() != null ? toMinDTO(report.getResolvedBy()) : null)
                .resolutionNotes(report.getResolutionNotes())
                .actionTaken(report.getActionTaken())
                .build();
    }
    
    /**
     * Convertir Usuario a UsuarioMinDTO
     */
    private UsuarioMinDTO toMinDTO(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        
        String fotoPerfil = null;
        if (usuario.getFotoPerfil() != null && usuario.getFotoPerfil().length > 0) {
            fotoPerfil = java.util.Base64.getEncoder().encodeToString(usuario.getFotoPerfil());
        }
        
        return new UsuarioMinDTO(
            usuario.getId(),
            usuario.getNombre(),
            usuario.getApellido(),
            fotoPerfil,
            usuario.getDeletedAt()
        );
    }
}
