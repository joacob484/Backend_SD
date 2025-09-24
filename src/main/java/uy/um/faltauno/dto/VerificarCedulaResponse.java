package uy.um.faltauno.dto;

public class VerificarCedulaResponse {
    private boolean verified;

    public VerificarCedulaResponse(boolean verified) {
        this.verified = verified;
    }

    // getter y setter
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}
