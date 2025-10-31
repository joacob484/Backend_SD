package uy.um.faltauno;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // ✅ Habilitar scheduled tasks (cleanup automático)
public class FaltaUnoApplication {
    public static void main(String[] args) {
        System.out.println("🚀 Falta Uno Backend - v3.0 - PRODUCTION READY 🎉");
        SpringApplication.run(FaltaUnoApplication.class, args);
    }
}
