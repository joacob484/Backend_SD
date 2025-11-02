package uy.um.faltauno.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración de RestTemplate para llamadas a APIs externas.
 * 
 * ✅ ARQUITECTURA: Configuración separada para evitar conflictos con WebConfig
 * y facilitar testing/mocking del RestTemplate.
 */
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
