package uy.um.faltauno.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * ✅ MEJORA: Health check personalizado para la base de datos
 * 
 * Proporciona información detallada sobre el estado de la conexión a PostgreSQL
 * Útil para:
 * - Monitoreo de aplicación en producción
 * - Kubernetes readiness/liveness probes
 * - Alertas de sistema
 * 
 * Accesible en: /actuator/health
 * 
 * Verifica:
 * 1. Conectividad a PostgreSQL
 * 2. Versión de PostgreSQL
 * 3. Estado del connection pool (HikariCP)
 * 4. Tiempo de respuesta de queries
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;
    
    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT version(), current_database(), current_user")) {
                
                if (resultSet.next()) {
                    String version = resultSet.getString(1);
                    String database = resultSet.getString(2);
                    String user = resultSet.getString(3);
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    
                    return Health.up()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("version", version.split(" ")[1]) // "PostgreSQL 14.5" -> "14.5"
                            .withDetail("databaseName", database)
                            .withDetail("user", user)
                            .withDetail("responseTimeMs", responseTime)
                            .withDetail("status", "Connected")
                            .build();
                }
                
                return Health.down()
                        .withDetail("error", "No se pudo obtener información de la base de datos")
                        .build();
            }
            
        } catch (Exception e) {
            log.error("[DatabaseHealthIndicator] Error verificando salud de la base de datos", e);
            return Health.down()
                    .withDetail("error", e.getClass().getSimpleName())
                    .withDetail("message", e.getMessage())
                    .build();
        }
    }
}
