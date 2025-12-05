package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import uy.um.faltauno.dto.ObservabilityDTO;
import uy.um.faltauno.dto.ObservabilityDTO.*;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio para métricas de observabilidad del admin panel
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ObservabilityService {
    
    private final DataSource dataSource;
    private final CacheManager cacheManager;
    private final PartidoService partidoService;
    private final UsuarioService usuarioService;
    
    // Contadores en memoria para métricas de performance
    private final Map<String, AtomicLong> endpointCalls = new ConcurrentHashMap<>();
    private final Map<String, List<Double>> responseTimes = new ConcurrentHashMap<>();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    
    /**
     * Obtener todas las métricas de observabilidad
     */
    public ObservabilityDTO getObservabilityMetrics() {
        log.info("[OBSERVABILITY] Generando métricas de observabilidad");
        
        try {
            log.info("[OBSERVABILITY] Obteniendo performance metrics...");
            PerformanceMetrics perf = getPerformanceMetrics();
            
            log.info("[OBSERVABILITY] Obteniendo cost metrics...");
            CostMetrics cost = getCostMetrics();
            
            log.info("[OBSERVABILITY] Obteniendo user metrics...");
            UserMetrics users = getUserMetrics();
            
            log.info("[OBSERVABILITY] Obteniendo system metrics...");
            SystemMetrics sys = getSystemMetrics();
            
            log.info("[OBSERVABILITY] Obteniendo database metrics...");
            DatabaseMetrics db = getDatabaseMetrics();
            
            log.info("[OBSERVABILITY] Generando alerts...");
            List<Alert> alerts = generateAlerts();
            
            log.info("[OBSERVABILITY] Construyendo DTO final...");
            return ObservabilityDTO.builder()
                    .performance(perf)
                    .costs(cost)
                    .users(users)
                    .system(sys)
                    .database(db)
                    .alerts(alerts)
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("[OBSERVABILITY] Error generando métricas", e);
            throw e;
        }
    }
    
    /**
     * Métricas de rendimiento
     */
    private PerformanceMetrics getPerformanceMetrics() {
        // Calcular tiempos de respuesta
        List<Double> allTimes = new ArrayList<>();
        responseTimes.values().forEach(allTimes::addAll);
        allTimes.sort(Double::compareTo);
        
        double avgTime = allTimes.isEmpty() ? 0 : 
            allTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        double p50 = allTimes.isEmpty() ? 0 : 
            allTimes.get((int) (allTimes.size() * 0.5));
        double p95 = allTimes.isEmpty() ? 0 : 
            allTimes.get(Math.min((int) (allTimes.size() * 0.95), allTimes.size() - 1));
        double p99 = allTimes.isEmpty() ? 0 : 
            allTimes.get(Math.min((int) (allTimes.size() * 0.99), allTimes.size() - 1));
        
        long total = totalRequests.get();
        long failed = failedRequests.get();
        double errorRate = total > 0 ? (failed * 100.0 / total) : 0;
        double successRate = 100 - errorRate;
        
        // Top endpoints por llamadas
        Map<String, Long> topEndpoints = new HashMap<>();
        endpointCalls.forEach((k, v) -> topEndpoints.put(k, v.get()));
        
        // Endpoints más lentos
        Map<String, Double> slowest = new HashMap<>();
        responseTimes.forEach((endpoint, times) -> {
            double avg = times.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            slowest.put(endpoint, avg);
        });
        
        return PerformanceMetrics.builder()
                .avgResponseTime(Math.round(avgTime * 100.0) / 100.0)
                .p50ResponseTime(Math.round(p50 * 100.0) / 100.0)
                .p95ResponseTime(Math.round(p95 * 100.0) / 100.0)
                .p99ResponseTime(Math.round(p99 * 100.0) / 100.0)
                .requestsPerMinute(total / 60) // Aproximado
                .errorRate(Math.round(errorRate * 100.0) / 100.0)
                .successRate(Math.round(successRate * 100.0) / 100.0)
                .endpointCalls(topEndpoints)
                .slowestEndpoints(slowest)
                .build();
    }
    
    /**
     * Métricas de costos (estimación)
     */
    private CostMetrics getCostMetrics() {
        // Estimaciones basadas en configuración actual
        double cloudRunBackend = 12.50;   // 1GB RAM, 1 CPU, min=0, max=2
        double cloudRunFrontend = 5.50;   // 512MB RAM, 1 CPU, min=0, max=2
        double cloudSql = 25.00;          // db-f1-micro
        double storage = 0.50;            // Con lifecycle policies
        double bandwidth = 1.50;          // Comprimido
        
        double dailyCost = (cloudRunBackend + cloudRunFrontend + cloudSql + storage + bandwidth) / 30.0;
        double monthlyEstimate = cloudRunBackend + cloudRunFrontend + cloudSql + storage + bandwidth;
        
        Map<String, Double> breakdown = new HashMap<>();
        breakdown.put("Backend (Cloud Run)", cloudRunBackend);
        breakdown.put("Frontend (Cloud Run)", cloudRunFrontend);
        breakdown.put("Base de Datos (Cloud SQL)", cloudSql);
        breakdown.put("Storage", storage);
        breakdown.put("Bandwidth", bandwidth);
        
        // Trends últimos 7 días (simulado - en producción vendría de Cloud Billing API)
        List<CostTrend> trends = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            trends.add(CostTrend.builder()
                    .date(LocalDate.now().minusDays(i).toString())
                    .cost(Math.round((dailyCost + (Math.random() * 2 - 1)) * 100.0) / 100.0)
                    .build());
        }
        
        return CostMetrics.builder()
                .monthlyEstimate(Math.round(monthlyEstimate * 100.0) / 100.0)
                .dailyCost(Math.round(dailyCost * 100.0) / 100.0)
                .cloudRunBackend(cloudRunBackend)
                .cloudRunFrontend(cloudRunFrontend)
                .cloudSql(cloudSql)
                .storage(storage)
                .bandwidth(bandwidth)
                .costBreakdown(breakdown)
                .trends(trends)
                .build();
    }
    
    /**
     * Métricas de usuarios
     */
    private UserMetrics getUserMetrics() {
        try {
            log.info("[OBSERVABILITY] Contando usuarios activos (30d)...");
            long activeUsers = usuarioService.contarUsuariosConActividadReciente(30);
            log.info("[OBSERVABILITY] Active users: {}", activeUsers);
            
            log.info("[OBSERVABILITY] Contando usuarios semanales...");
            long weeklyActive = usuarioService.contarUsuariosConActividadReciente(7);
            log.info("[OBSERVABILITY] Weekly active: {}", weeklyActive);
            
            log.info("[OBSERVABILITY] Contando usuarios diarios...");
            long dailyActive = usuarioService.contarUsuariosConActividadReciente(1);
            log.info("[OBSERVABILITY] Daily active: {}", dailyActive);
            
            log.info("[OBSERVABILITY] Contando usuarios online...");
            long onlineNow = usuarioService.contarUsuariosConectados(); // Últimos 5 minutos
            log.info("[OBSERVABILITY] Online now: {}", onlineNow);
            
            // Por país (simulado - requeriría geolocalización)
            log.info("[OBSERVABILITY] Calculando distribución por país...");
            Map<String, Long> byCountry = Map.of(
                    "Uruguay", activeUsers * 70 / 100,
                    "Argentina", activeUsers * 20 / 100,
                    "Brasil", activeUsers * 7 / 100,
                    "Otros", activeUsers * 3 / 100
            );
            
            // Por dispositivo (simulado - requeriría User-Agent tracking)
            log.info("[OBSERVABILITY] Calculando distribución por dispositivo...");
            Map<String, Long> byDevice = Map.of(
                    "Mobile", activeUsers * 75 / 100,
                    "Desktop", activeUsers * 20 / 100,
                    "Tablet", activeUsers * 5 / 100
            );
            
            // Trends últimos 7 días
            log.info("[OBSERVABILITY] Calculando trends de usuarios...");
            List<UserTrend> trends = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                long count = usuarioService.contarRegistrosRecientes(i);
                trends.add(UserTrend.builder()
                        .date(LocalDate.now().minusDays(i).toString())
                        .users(count)
                        .build());
            }
            
            log.info("[OBSERVABILITY] Construyendo UserMetrics...");
            return UserMetrics.builder()
                    .activeUsers(activeUsers)
                    .dailyActiveUsers(dailyActive)
                    .weeklyActiveUsers(weeklyActive)
                    .onlineUsers(onlineNow)
                    .usersByCountry(byCountry)
                    .usersByDevice(byDevice)
                    .activityTrends(trends)
                    .build();
        } catch (Exception e) {
            log.error("[OBSERVABILITY] Error en getUserMetrics", e);
            throw e;
        }
    }
    
    /**
     * Métricas de sistema
     */
    private SystemMetrics getSystemMetrics() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
        double memoryUsage = (usedMemory * 100.0) / maxMemory;
        
        double cpuUsage = osBean.getSystemLoadAverage();
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000; // seconds
        
        Map<String, String> jvmInfo = new HashMap<>();
        jvmInfo.put("version", System.getProperty("java.version"));
        jvmInfo.put("vendor", System.getProperty("java.vendor"));
        jvmInfo.put("runtime", System.getProperty("java.runtime.name"));
        jvmInfo.put("gc", "SerialGC"); // Según configuración
        jvmInfo.put("maxHeap", maxMemory + " MB");
        
        return SystemMetrics.builder()
                .version("3.5.0")
                .environment("production")
                .uptime(uptime)
                .cpuUsage(Math.round(cpuUsage * 100.0) / 100.0)
                .memoryUsage(Math.round(memoryUsage * 100.0) / 100.0)
                .memoryUsedMB(usedMemory)
                .memoryTotalMB(maxMemory)
                .activeInstances(1) // Cloud Run actual
                .maxInstances(2)    // Configurado
                .jvmInfo(jvmInfo)
                .build();
    }
    
    /**
     * Métricas de base de datos
     */
    private DatabaseMetrics getDatabaseMetrics() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Conexiones activas
            ResultSet rs = stmt.executeQuery(
                    "SELECT count(*) as total, " +
                    "sum(case when state = 'active' then 1 else 0 end) as active, " +
                    "sum(case when state = 'idle' then 1 else 0 end) as idle " +
                    "FROM pg_stat_activity WHERE datname = current_database()");
            
            long total = 0, active = 0, idle = 0;
            if (rs.next()) {
                total = rs.getLong("total");
                active = rs.getLong("active");
                idle = rs.getLong("idle");
            }
            
            double poolUsage = (active * 100.0) / 3.0; // Pool size = 3
            
            // Cache hits (Caffeine + PostgreSQL)
            long cacheHits = 0;
            long cacheMisses = 0;
            if (cacheManager != null) {
                // Caffeine cache stats (estimado)
                cacheHits = totalRequests.get() * 60 / 100; // ~60% hit rate esperado
                cacheMisses = totalRequests.get() * 40 / 100;
            }
            
            double cacheHitRate = (cacheHits + cacheMisses) > 0 ? 
                    (cacheHits * 100.0) / (cacheHits + cacheMisses) : 0;
            
            // Query stats (con fallback si pg_stat_statements no está disponible)
            long totalQueries = 0;
            double avgQueryTime = 0;
            List<SlowQuery> slowQueries = new ArrayList<>();
            
            try {
                ResultSet queryStats = stmt.executeQuery(
                        "SELECT count(*) as total_queries, " +
                        "COALESCE(avg(mean_exec_time), 0) as avg_time " +
                        "FROM pg_stat_statements " +
                        "WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())");
                
                if (queryStats.next()) {
                    totalQueries = queryStats.getLong("total_queries");
                    avgQueryTime = queryStats.getDouble("avg_time");
                }
                
                // Slow queries (top 5)
                ResultSet slowRs = stmt.executeQuery(
                        "SELECT substring(query, 1, 100) as query, " +
                        "mean_exec_time as avg_time, calls " +
                        "FROM pg_stat_statements " +
                        "WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database()) " +
                        "ORDER BY mean_exec_time DESC LIMIT 5");
                
                while (slowRs.next()) {
                    slowQueries.add(SlowQuery.builder()
                            .query(slowRs.getString("query"))
                            .avgTime(Math.round(slowRs.getDouble("avg_time") * 100.0) / 100.0)
                            .calls(slowRs.getLong("calls"))
                            .build());
                }
            } catch (Exception pgStatEx) {
                log.warn("[OBSERVABILITY] pg_stat_statements no disponible, usando valores por defecto", pgStatEx);
                // Usar valores estimados si la extensión no está disponible
                totalQueries = totalRequests.get();
                avgQueryTime = 50.0; // ms estimado
            }
            
            // Tamaños de tablas
            Map<String, Long> tableSizes = new HashMap<>();
            ResultSet sizeRs = stmt.executeQuery(
                    "SELECT tablename, " +
                    "pg_total_relation_size(schemaname||'.'||tablename) / (1024*1024) as size_mb " +
                    "FROM pg_tables WHERE schemaname = 'public' ORDER BY size_mb DESC LIMIT 10");
            
            while (sizeRs.next()) {
                tableSizes.put(sizeRs.getString("tablename"), sizeRs.getLong("size_mb"));
            }
            
            return DatabaseMetrics.builder()
                    .totalConnections(total)
                    .activeConnections(active)
                    .idleConnections(idle)
                    .connectionPoolUsage(Math.round(poolUsage * 100.0) / 100.0)
                    .cacheHits(cacheHits)
                    .cacheMisses(cacheMisses)
                    .cacheHitRate(Math.round(cacheHitRate * 100.0) / 100.0)
                    .totalQueries(totalQueries)
                    .avgQueryTime(Math.round(avgQueryTime * 100.0) / 100.0)
                    .slowQueries(slowQueries)
                    .tablesSizes(tableSizes)
                    .build();
                    
        } catch (Exception e) {
            log.error("[OBSERVABILITY] Error obteniendo métricas de BD", e);
            // Return defaults on error
            return DatabaseMetrics.builder()
                    .totalConnections(0L)
                    .activeConnections(0L)
                    .idleConnections(0L)
                    .connectionPoolUsage(0.0)
                    .cacheHits(0L)
                    .cacheMisses(0L)
                    .cacheHitRate(0.0)
                    .totalQueries(0L)
                    .avgQueryTime(0.0)
                    .slowQueries(Collections.emptyList())
                    .tablesSizes(Collections.emptyMap())
                    .build();
        }
    }
    
    /**
     * Generar alertas basadas en métricas
     */
    private List<Alert> generateAlerts() {
        List<Alert> alerts = new ArrayList<>();
        
        // Verificar métricas y generar alertas
        PerformanceMetrics perf = getPerformanceMetrics();
        SystemMetrics sys = getSystemMetrics();
        DatabaseMetrics db = getDatabaseMetrics();
        CostMetrics cost = getCostMetrics();
        
        // Alerta: Error rate alto
        if (perf.getErrorRate() > 5.0) {
            alerts.add(Alert.builder()
                    .level("CRITICAL")
                    .category("PERFORMANCE")
                    .message("Tasa de errores elevada")
                    .details(String.format("Error rate: %.2f%% (threshold: 5%%)", perf.getErrorRate()))
                    .timestamp(LocalDateTime.now())
                    .action("Revisar logs y health checks")
                    .build());
        }
        
        // Alerta: Response time alto
        if (perf.getP95ResponseTime() > 500) {
            alerts.add(Alert.builder()
                    .level("WARNING")
                    .category("PERFORMANCE")
                    .message("Tiempo de respuesta P95 elevado")
                    .details(String.format("P95: %.2fms (threshold: 500ms)", perf.getP95ResponseTime()))
                    .timestamp(LocalDateTime.now())
                    .action("Revisar queries lentas y optimizar índices")
                    .build());
        }
        
        // Alerta: Memoria alta
        if (sys.getMemoryUsage() > 85.0) {
            alerts.add(Alert.builder()
                    .level("WARNING")
                    .category("SYSTEM")
                    .message("Uso de memoria elevado")
                    .details(String.format("Memory: %.2f%% (threshold: 85%%)", sys.getMemoryUsage()))
                    .timestamp(LocalDateTime.now())
                    .action("Considerar aumentar heap o revisar memory leaks")
                    .build());
        }
        
        // Alerta: Pool de conexiones alto
        if (db.getConnectionPoolUsage() > 80.0) {
            alerts.add(Alert.builder()
                    .level("WARNING")
                    .category("DATABASE")
                    .message("Pool de conexiones cerca del límite")
                    .details(String.format("Pool usage: %.2f%% (threshold: 80%%)", db.getConnectionPoolUsage()))
                    .timestamp(LocalDateTime.now())
                    .action("Revisar conexiones idle o aumentar pool size")
                    .build());
        }
        
        // Alerta: Cache hit rate bajo
        if (db.getCacheHitRate() < 50.0 && db.getCacheHitRate() > 0) {
            alerts.add(Alert.builder()
                    .level("INFO")
                    .category("DATABASE")
                    .message("Cache hit rate bajo")
                    .details(String.format("Hit rate: %.2f%% (óptimo: >70%%)", db.getCacheHitRate()))
                    .timestamp(LocalDateTime.now())
                    .action("Aumentar cache size o revisar patrones de acceso")
                    .build());
        }
        
        // Alerta: Costo proyectado
        if (cost.getMonthlyEstimate() > 40.0) {
            alerts.add(Alert.builder()
                    .level("WARNING")
                    .category("COST")
                    .message("Costo mensual cerca del presupuesto")
                    .details(String.format("Proyectado: $%.2f/mes (budget: $40/mes)", cost.getMonthlyEstimate()))
                    .timestamp(LocalDateTime.now())
                    .action("Revisar uso de recursos y aplicar optimizaciones")
                    .build());
        }
        
        // Si no hay alertas, agregar una de estado OK
        if (alerts.isEmpty()) {
            alerts.add(Alert.builder()
                    .level("INFO")
                    .category("SYSTEM")
                    .message("Sistema funcionando normalmente")
                    .details("Todas las métricas dentro de rangos aceptables")
                    .timestamp(LocalDateTime.now())
                    .action("Continuar monitoreo regular")
                    .build());
        }
        
        return alerts;
    }
    
    /**
     * Registrar llamada a endpoint (para métricas)
     */
    public void recordEndpointCall(String endpoint, double responseTimeMs) {
        endpointCalls.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();
        responseTimes.computeIfAbsent(endpoint, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(responseTimeMs);
        totalRequests.incrementAndGet();
        successfulRequests.incrementAndGet();
    }
    
    /**
     * Registrar error (para métricas)
     */
    public void recordError(String endpoint) {
        endpointCalls.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();
        totalRequests.incrementAndGet();
        failedRequests.incrementAndGet();
    }
    
    /**
     * Reset métricas (útil para testing)
     */
    public void resetMetrics() {
        endpointCalls.clear();
        responseTimes.clear();
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
    }
}
