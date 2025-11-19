package uy.um.faltauno.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {
    private UUID id;
    private String nombre;
    private String apellido;
    private String fechaNacimiento;
    private String email;
    
    private Double altura;
    private Double peso;
    private String posicion;
    private String fotoPerfil; // Base64 string
    
    // Indica explícitamente si el usuario tiene foto de perfil guardada en el backend
    // Esto evita que el frontend tenga que inferir la presencia de la foto por distintos aliases
    @JsonProperty("hasFotoPerfil")
    private Boolean hasFotoPerfil;

    // TODO: Cédula deshabilitada temporalmente - mantener para futura implementación de badge verificado
    private String cedula;
    
    private String genero; // Masculino o Femenino
    
    private String rol; // USER o ADMIN
    
    // TODO: Cédula deshabilitada temporalmente
    @JsonProperty("cedulaVerificada")
    private Boolean cedulaVerificada;
    
    @JsonProperty("perfilCompleto")
    private Boolean perfilCompleto;
    
    @JsonProperty("emailVerified")
    private Boolean emailVerified;
    
    // Ban information
    private String bannedAt;
    private String banReason;
    private String banUntil;
    private String bannedBy;
    
    // Activity tracking
    private String lastActivityAt;
    private String createdAt;
    private String deletedAt;
    
    /**
     * Password solo para recepción en registro/login.
     * WRITE_ONLY previene que sea serializado en respuestas JSON.
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /**
     * Código de verificación de email (solo para recepción).
     * WRITE_ONLY previene que sea serializado en respuestas JSON.
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String verificationCode;

     /**
     * Calcula si el perfil está completo basándose en los campos requeridos.
     * Perfil completo: nombre, apellido, fechaNacimiento Y foto
     */
    public Boolean getPerfilCompleto() {
        // ⚡ SIEMPRE calcular dinámicamente - NO respetar valor viejo
        // Perfil completo si tiene: nombre, apellido, fechaNacimiento Y foto
        boolean completo = nombre != null && !nombre.isEmpty()
                && apellido != null && !apellido.isEmpty()
                && fechaNacimiento != null && !fechaNacimiento.isEmpty()
                && (hasFotoPerfil != null && hasFotoPerfil);
        
        return completo;
    }

    /**
     * TODO: Cédula deshabilitada temporalmente - siempre retorna true
     * Calcula si la cédula está verificada.
     */
    public Boolean getCedulaVerificada() {
        // Si el flag se estableció explícitamente, respetarlo
        if (cedulaVerificada != null) return cedulaVerificada;

        // Si no, inferir por la existencia de la cédula
        return cedula != null && !cedula.isEmpty();
    }
}