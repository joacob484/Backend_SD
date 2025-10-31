package uy.um.faltauno;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // ✅ Habilitar scheduled tasks (cleanup automático)
@RequiredArgsConstructor
@Slf4j
public class FaltaUnoApplication {
    
    private final JdbcTemplate jdbcTemplate;
    
    public static void main(String[] args) {
        System.out.println("🚀 Falta Uno Backend - v3.0 - PRODUCTION READY 🎉");
        SpringApplication.run(FaltaUnoApplication.class, args);
    }
    
    @PostConstruct
    public void ensureVerificationColumns() {
        try {
            log.info("🔧 Verificando columnas de verificación email...");
            
            // Agregar columnas si no existen
            jdbcTemplate.execute(
                "ALTER TABLE usuario ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE"
            );
            jdbcTemplate.execute(
                "ALTER TABLE usuario ADD COLUMN IF NOT EXISTS verification_code VARCHAR(6)"
            );
            jdbcTemplate.execute(
                "ALTER TABLE usuario ADD COLUMN IF NOT EXISTS verification_code_expires_at TIMESTAMP"
            );
            
            // Marcar usuarios OAuth como verificados
            int updated = jdbcTemplate.update(
                "UPDATE usuario SET email_verified = TRUE WHERE provider = 'GOOGLE' AND email_verified = FALSE"
            );
            
            // Crear índice
            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_usuario_verification_code ON usuario(verification_code) WHERE verification_code IS NOT NULL"
            );
            
            log.info("✅ Columnas de verificación configuradas correctamente. Usuarios OAuth verificados: {}", updated);
            
        } catch (Exception e) {
            log.error("❌ Error configurando columnas de verificación: {}", e.getMessage());
            // No lanzar excepción para no bloquear el startup si las columnas ya existen
        }
    }
}
