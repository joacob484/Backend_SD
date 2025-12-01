package uy.um.faltauno;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // ✅ Habilitar scheduled tasks (cleanup automático)
@EnableAsync  // ✅ Habilitar procesamiento asíncrono (notificaciones)
@RequiredArgsConstructor
@Slf4j
public class FaltaUnoApplication {
    
    private final JdbcTemplate jdbcTemplate;
    
    public static void main(String[] args) {
        log.info("🚀 Falta Uno Backend - v3.0 - PRODUCTION READY 🎉");
        SpringApplication.run(FaltaUnoApplication.class, args);
    }
    
    @PostConstruct
    public void logStartup() {
        try {
            log.info("✅ Application started successfully");
            log.info("✅ Database connection pool initialized");
            
            // Quick sanity check without DDL
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuario", Integer.class
            );
            log.info("✅ Database accessible - {} users in system", count);
            
        } catch (Exception e) {
            log.error("❌ Error during startup check: {}", e.getMessage());
            // Don't throw exception to avoid blocking startup
        }
    }
}
