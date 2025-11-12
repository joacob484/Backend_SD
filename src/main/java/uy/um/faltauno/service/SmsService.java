package uy.um.faltauno.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para envío de SMS usando Termii
 * 
 * Termii: Servicio GRATUITO (10 SMS/mes) sin restricciones
 * - API Docs: https://developers.termii.com/messaging
 * - Dashboard: https://accounts.termii.com/
 * 
 * Configuración vía application.yaml:
 * app:
 *   sms:
 *     enabled: true
 *     provider: termii
 * termii:
 *   api-key: ${TERMII_API_KEY}
 *   sender-id: ${TERMII_SENDER_ID}
 */
@Service
@Slf4j
public class SmsService {

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;
    
    @Value("${app.sms.provider:console}")
    private String smsProvider;
    
    // Configuración Termii
    @Value("${termii.api-key:}")
    private String termiiApiKey;
    
    @Value("${termii.sender-id:FaltaUno}")
    private String termiiSenderId;
    
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Inicializar Termii al arrancar el servicio
     */
    @PostConstruct
    public void init() {
        if (!smsEnabled) {
            log.info("[SMS] ℹ️ SMS deshabilitado - Modo desarrollo (console)");
            return;
        }
        
        if ("termii".equalsIgnoreCase(smsProvider)) {
            if (termiiApiKey.isBlank()) {
                log.error("[SMS] ❌ Termii habilitado pero falta TERMII_API_KEY");
                return;
            }
            
            log.info("[SMS] ✅ Termii configurado - Sender ID: {}", termiiSenderId);
            log.info("[SMS] ℹ️ Termii Plan Gratuito: 10 SMS/mes sin verificación");
        } else {
            log.info("[SMS] ℹ️ SMS modo: console (desarrollo)");
        }
    }

    /**
     * Enviar SMS a un número de teléfono
     * 
     * @param phoneNumber Número en formato internacional (+XXX XXXXXXXXX)
     * @param message Mensaje a enviar
     */
    public void enviarSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            // Modo desarrollo: solo loguear de forma visible
            enviarConConsole(phoneNumber, message);
            return;
        }

        try {
            if ("termii".equalsIgnoreCase(smsProvider)) {
                enviarConTermii(phoneNumber, message);
            } else {
                // Default: console (desarrollo)
                enviarConConsole(phoneNumber, message);
            }
        } catch (Exception e) {
            log.error("[SMS] ❌ Error enviando SMS a {}", phoneNumber, e);
            throw new RuntimeException("Error al enviar SMS: " + e.getMessage(), e);
        }
    }

    /**
     * Modo consola (desarrollo/testing)
     * Muestra el código de forma MUY visible en los logs
     */
    private void enviarConConsole(String phoneNumber, String message) {
        // Extraer código del mensaje (asume formato "código: XXXXXX")
        String codigo = extraerCodigo(message);
        
        log.info("");
        log.info("═══════════════════════════════════════════════════════");
        log.info("📱 SMS SIMULADO (Modo Desarrollo - GRATIS)");
        log.info("═══════════════════════════════════════════════════════");
        log.info("Para: {}", phoneNumber);
        log.info("Mensaje: {}", message);
        if (codigo != null) {
            log.info("");
            log.info("╔═══════════════════════════════════════════════════════╗");
            log.info("║           🔑 CÓDIGO DE VERIFICACIÓN: {}           ║", codigo);
            log.info("╚═══════════════════════════════════════════════════════╝");
        }
        log.info("═══════════════════════════════════════════════════════");
        log.info("");
    }
    
    /**
     * Extrae el código de verificación del mensaje
     */
    private String extraerCodigo(String message) {
        if (message == null) return null;
        
        // Buscar patrón de 6 dígitos
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b(\\d{6})\\b");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    /**
     * Implementación con Termii (GRATIS - 10 SMS/mes)
     * API Docs: https://developers.termii.com/messaging
     */
    private void enviarConTermii(String phoneNumber, String message) {
        log.info("[SMS] 📱 Termii - Enviando a {}", phoneNumber);
        
        try {
            String url = "https://api.ng.termii.com/api/sms/send";
            
            // Construir request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("to", phoneNumber.replace("+", "")); // Termii no usa +
            requestBody.put("from", termiiSenderId);
            requestBody.put("sms", message);
            requestBody.put("type", "plain");
            requestBody.put("channel", "generic");
            requestBody.put("api_key", termiiApiKey);
            
            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Enviar request
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String messageId = response.getBody().get("message_id") != null 
                    ? response.getBody().get("message_id").toString() 
                    : "unknown";
                    
                log.info("[SMS] ✅ Termii - SMS enviado exitosamente. Message ID: {}", messageId);
                log.info("[SMS] ℹ️ Termii Response: {}", response.getBody());
            } else {
                log.error("[SMS] ❌ Termii - Error en respuesta: {}", response.getBody());
                throw new RuntimeException("Error en respuesta de Termii: " + response.getBody());
            }
            
        } catch (Exception e) {
            log.error("[SMS] ❌ Termii - Error enviando SMS", e);
            throw new RuntimeException("Error enviando SMS con Termii: " + e.getMessage(), e);
        }
    }
}
