package uy.um.faltauno.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Representa el estado actual del onboarding obligatorio antes de liberar todas las rutas de la app.
 */
@Value
@Builder
public class OnboardingStatusDTO {

    public enum Step {
        VERIFY_EMAIL,
        COMPLETE_PROFILE,
        VERIFY_CEDULA,
        DONE
    }

    Step nextStep;
    boolean requiresAction;
    boolean emailVerified;
    boolean perfilCompleto;
    boolean cedulaVerificada;
    String blockingReason;
}
