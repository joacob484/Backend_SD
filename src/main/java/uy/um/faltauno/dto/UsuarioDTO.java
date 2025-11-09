package uy.um.faltauno.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
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
    
    /**
     * Número de celular en formato internacional: +XXX XXXXXXXXX
     * Acepta entre 6 y 30 caracteres, comenzando con + opcional
     * Ejemplos válidos: +598 91234567, +54 1123456789, +1 2025551234
     */
    @Pattern(regexp = "^\\+?[0-9\\s]{6,30}$", 
             message = "Número de celular inválido. Formato esperado: +XXX XXXXXXXXX")
    private String celular;
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
    private String bannedBy;
    
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
     * El celular es obligatorio pero se valida por separado en la navegación del frontend
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