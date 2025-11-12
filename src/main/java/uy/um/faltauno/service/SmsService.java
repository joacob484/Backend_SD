package uy.um.faltauno.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para envío de SMS usando múltiples proveedores
 * 
 * Proveedores soportados:
 * - console: Modo desarrollo (gratis, muestra código en logs)
 * - termii: Termii API (10 SMS gratis/mes, sin verificación)
 * - twilio: Twilio API (trial requiere verificación)
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
    
    // Configuración Twilio
    @Value("${twilio.account-sid:}")
    private String twilioAccountSid;
    
    @Value("${twilio.auth-token:}")
    private String twilioAuthToken;
    
    @Value("${twilio.from-number:}")
    private String twilioFromNumber;
    
    // Configuración Termii
    @Value("${termii.api-key:}")
    private String termiiApiKey;
    
    @Value("${termii.sender-id:FaltaUno}")
    private String termiiSenderId;
    
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Inicializar proveedores al arrancar el servicio
     */
    @PostConstruct
    public void init() {
        if (!smsEnabled) {
            log.info("[SMS] ℹ️ SMS deshabilitado - Modo desarrollo (console)");
            return;
        }
        
        switch (smsProvider.toLowerCase()) {
            case "twilio":
                initTwilio();
                break;
            case "termii":
                initTermii();
                break;
            case "console":
                log.info("[SMS] ℹ️ SMS modo: console (desarrollo)");
                break;
            default:
                log.warn("[SMS] ⚠️ Proveedor desconocido: {}. Usando console.", smsProvider);
        }
    }
    
    private void initTwilio() {
        if (twilioAccountSid.isBlank() || twilioAuthToken.isBlank()) {
            log.error("[SMS] ❌ Twilio habilitado pero falta configuración (TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)");
            return;
        }
        
        try {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            log.info("[SMS] ✅ Twilio inicializado correctamente");
        } catch (Exception e) {
            log.error("[SMS] ❌ Error inicializando Twilio", e);
        }
    }
    
    private void initTermii() {
        if (termiiApiKey.isBlank()) {
            log.error("[SMS] ❌ Termii habilitado pero falta TERMII_API_KEY");
            return;
        }
        
        log.info("[SMS] ✅ Termii configurado - Sender ID: {}", termiiSenderId);
        log.info("[SMS] ℹ️ Termii Plan Gratuito: 10 SMS/mes sin verificación");
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
            switch (smsProvider.toLowerCase()) {
                case "console":
                    enviarConConsole(phoneNumber, message);
                    break;
                    
                case "termii":
                    enviarConTermii(phoneNumber, message);
                    break;
                    
                case "twilio":
                    enviarConTwilio(phoneNumber, message);
                    break;
                    
                default:
                    log.warn("[SMS] ⚠️ Proveedor SMS desconocido: {}", smsProvider);
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
    
    /**
     * Implementación con Twilio
     */
    private void enviarConTwilio(String phoneNumber, String message) {
        log.info("[SMS] 📱 Twilio - Enviando a {}: {}", phoneNumber, message);
        
        try {
            // Validar configuración
            if (twilioFromNumber.isBlank()) {
                throw new IllegalStateException("TWILIO_FROM_NUMBER no configurado");
            }
            
            // Enviar SMS
            Message twilioMessage = Message.creator(
                new PhoneNumber(phoneNumber),  // To
                new PhoneNumber(twilioFromNumber),  // From
                message  // Body
            ).create();
            
            log.info("[SMS] ✅ Twilio - SMS enviado exitosamente. SID: {}, Status: {}", 
                    twilioMessage.getSid(), 
                    twilioMessage.getStatus());
            
        } catch (Exception e) {
            log.error("[SMS] ❌ Twilio - Error enviando SMS", e);
            throw new RuntimeException("Error enviando SMS con Twilio: " + e.getMessage(), e);
        }
    }
}
