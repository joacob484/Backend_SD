package uy.um.faltauno.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Servicio para envío de SMS usando Twilio
 * Configuración vía application.yaml:
 * 
 * app:
 *   sms:
 *     enabled: true
 *     provider: twilio
 * twilio:
 *   account-sid: ${TWILIO_ACCOUNT_SID}
 *   auth-token: ${TWILIO_AUTH_TOKEN}
 *   from-number: ${TWILIO_FROM_NUMBER}
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

    /**
     * Inicializar Twilio al arrancar el servicio
     */
    @PostConstruct
    public void init() {
        if (smsEnabled && "twilio".equalsIgnoreCase(smsProvider)) {
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
        } else {
            log.info("[SMS] ℹ️ SMS modo: {} (enabled: {})", smsProvider, smsEnabled);
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
            // Modo desarrollo: solo loguear
            log.info("[SMS] 📱 MODO DESARROLLO - SMS a {}: {}", phoneNumber, message);
            return;
        }

        try {
            switch (smsProvider.toLowerCase()) {
                case "console":
                    enviarConConsole(phoneNumber, message);
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
     */
    private void enviarConConsole(String phoneNumber, String message) {
        log.info("[SMS] 📱 Console - Enviando a {}: {}", phoneNumber, message);
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
