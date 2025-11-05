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
    private String cedula;
    private String genero; // Masculino o Femenino
    
    @JsonProperty("cedulaVerificada")
    private Boolean cedulaVerificada;
    
    @JsonProperty("perfilCompleto")
    private Boolean perfilCompleto;
    
    @JsonProperty("emailVerified")
    private Boolean emailVerified;
    
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
     */
    public Boolean getPerfilCompleto() {
        // Perfil completo si tiene: nombre, apellido, celular, fechaNacimiento
        boolean completo = nombre != null && !nombre.isEmpty()
                && apellido != null && !apellido.isEmpty()
                && celular != null && !celular.isEmpty()
                && fechaNacimiento != null && !fechaNacimiento.isEmpty();
        
        // Si perfilCompleto está seteado explícitamente, respetarlo
        if (perfilCompleto != null) {
            return perfilCompleto;
        }
        
        return completo;
    }

    /**
     * Calcula si la cédula está verificada.
     */
    public Boolean getCedulaVerificada() {
        // Cédula verificada si existe y no está vacía
        boolean verificada = cedula != null && !cedula.isEmpty();
        
        // Si cedulaVerificada está seteado explícitamente, respetarlo
        if (cedulaVerificada != null) {
            return cedulaVerificada;
        }
        
        return verificada;
    }
}