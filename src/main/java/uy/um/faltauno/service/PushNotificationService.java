package uy.um.faltauno.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PushNotificationService {
    // Aquí iría la integración con FCM, OneSignal, etc.
    // Este servicio se suscribe a Pub/Sub y envía push a móviles

    public void enviarPush(String userId, String titulo, String mensaje) {
        // Integración con proveedor de push
        log.info("[Push] Notificación enviada a {}: {} - {}", userId, titulo, mensaje);
    }
}
