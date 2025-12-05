package uy.um.faltauno.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO para datos de observabilidad del admin panel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservabilityDTO {
    
    // ===== MÉTRICAS DE RENDIMIENTO =====
    private PerformanceMetrics performance;
    
    // ===== MÉTRICAS DE COSTOS =====
    private CostMetrics costs;
    
    // ===== MÉTRICAS DE USUARIOS =====
    private UserMetrics users;
    
    // ===== MÉTRICAS DE SISTEMA =====
    private SystemMetrics system;
    
    // ===== MÉTRICAS DE BASE DE DATOS =====
    private DatabaseMetrics database;
    
    // ===== ALERTAS =====
    private List<Alert> alerts;
    
    // ===== TIMESTAMP =====
    private LocalDateTime timestamp;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private Double avgResponseTime;      // ms
        private Double p50ResponseTime;      // ms
        private Double p95ResponseTime;      // ms
        private Double p99ResponseTime;      // ms
        private Long requestsPerMinute;
        private Double errorRate;            // %
        private Double successRate;          // %
        private Map<String, Long> endpointCalls; // endpoint -> count
        private Map<String, Double> slowestEndpoints; // endpoint -> avg_time
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostMetrics {
        private Double monthlyEstimate;      // USD
        private Double dailyCost;            // USD
        private Double cloudRunBackend;      // USD
        private Double cloudRunFrontend;     // USD
        private Double cloudSql;             // USD
        private Double storage;              // USD
        private Double bandwidth;            // USD
        private Map<String, Double> costBreakdown;
        private List<CostTrend> trends;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostTrend {
        private String date;
        private Double cost;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserMetrics {
        private Long activeUsers;            // últimos 30 días
        private Long dailyActiveUsers;       // últimas 24 horas
        private Long weeklyActiveUsers;      // últimos 7 días
        private Long onlineUsers;            // ahora
        private Map<String, Long> usersByCountry;
        private Map<String, Long> usersByDevice;
        private List<UserTrend> activityTrends;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserTrend {
        private String date;
        private Long users;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemMetrics {
        private String version;
        private String environment;
        private Long uptime;                 // seconds
        private Double cpuUsage;             // %
        private Double memoryUsage;          // %
        private Long memoryUsedMB;
        private Long memoryTotalMB;
        private Integer activeInstances;
        private Integer maxInstances;
        private Map<String, String> jvmInfo;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseMetrics {
        private Long totalConnections;
        private Long activeConnections;
        private Long idleConnections;
        private Double connectionPoolUsage;  // %
        private Long cacheHits;
        private Long cacheMisses;
        private Double cacheHitRate;         // %
        private Long totalQueries;
        private Double avgQueryTime;         // ms
        private List<SlowQuery> slowQueries;
        private Map<String, Long> tablesSizes; // MB
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlowQuery {
        private String query;
        private Double avgTime;              // ms
        private Long calls;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alert {
        private String level;                // INFO, WARNING, CRITICAL
        private String category;             // PERFORMANCE, COST, SYSTEM, DATABASE
        private String message;
        private String details;
        private LocalDateTime timestamp;
        private String action;               // Recommended action
    }
}
