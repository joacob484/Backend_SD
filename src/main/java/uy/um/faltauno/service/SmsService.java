package uy.um.faltauno.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio para envío de SMS
 * Implementación placeholder que se puede integrar con:
 * - Twilio
 * - AWS SNS
 * - Firebase Cloud Messaging
 * - Cualquier otro proveedor de SMS
 */
@Service
@Slf4j
public class SmsService {

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;
    
    @Value("${app.sms.provider:console}")
    private String smsProvider; // console, twilio, aws-sns, etc.

    /**
     * Enviar SMS a un número de teléfono
     * 
     * @param phoneNumber Número en formato internacional (+XXX XXXXXXXXX)
     * @param message Mensaje a enviar
     */
    public void enviarSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            // Modo desarrollo: solo loguear
            log.info("[SMS] 📱 MODO DESARROLLO - SMS a {}: {}", phoneNumber, message);
            return;
        }

        try {
            switch (smsProvider.toLowerCase()) {
                case "console":
                    // Solo log (útil para desarrollo)
                    log.info("[SMS] 📱 Console - Enviando a {}: {}", phoneNumber, message);
                    break;
                    
                case "twilio":
                    // TODO: Implementar con Twilio
                    enviarConTwilio(phoneNumber, message);
                    break;
                    
                case "aws-sns":
                    // TODO: Implementar con AWS SNS
                    enviarConAwsSns(phoneNumber, message);
                    break;
                    
                default:
                    log.warn("[SMS] ⚠️ Proveedor SMS desconocido: {}", smsProvider);
                    log.info("[SMS] 📱 Fallback - SMS a {}: {}", phoneNumber, message);
            }
        } catch (Exception e) {
            log.error("[SMS] ❌ Error enviando SMS a {}", phoneNumber, e);
            throw new RuntimeException("Error al enviar SMS", e);
        }
    }

    /**
     * Implementación con Twilio (placeholder)
     * 
     * Para activar:
     * 1. Agregar dependencia en pom.xml:
     *    <dependency>
     *        <groupId>com.twilio.sdk</groupId>
     *        <artifactId>twilio</artifactId>
     *        <version>9.14.1</version>
     *    </dependency>
     * 
     * 2. Configurar en application.yaml:
     *    app:
     *      sms:
     *        enabled: true
     *        provider: twilio
     *    twilio:
     *      account-sid: your_account_sid
     *      auth-token: your_auth_token
     *      from-number: your_twilio_number
     */
    private void enviarConTwilio(String phoneNumber, String message) {
        // TODO: Implementar cuando se agregue Twilio
        log.info("[SMS] 📱 Twilio - Enviando a {}: {}", phoneNumber, message);
        
        /* Ejemplo de implementación:
        Twilio.init(accountSid, authToken);
        Message.creator(
            new PhoneNumber(phoneNumber),
            new PhoneNumber(fromNumber),
            message
        ).create();
        */
    }

    /**
     * Implementación con AWS SNS (placeholder)
     */
    private void enviarConAwsSns(String phoneNumber, String message) {
        // TODO: Implementar cuando se agregue AWS SDK
        log.info("[SMS] 📱 AWS SNS - Enviando a {}: {}", phoneNumber, message);
        
        /* Ejemplo de implementación:
        SnsClient snsClient = SnsClient.builder()
            .region(Region.US_EAST_1)
            .build();
            
        PublishRequest request = PublishRequest.builder()
            .message(message)
            .phoneNumber(phoneNumber)
            .build();
            
        snsClient.publish(request);
        */
    }
}
