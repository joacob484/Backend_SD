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
    private String celular;
    private Double altura;
    private Double peso;
    private String posicion;
    private String fotoPerfil; // Base64 string
    private String cedula;
    
    @JsonProperty("cedulaVerificada")
    private Boolean cedulaVerificada;
    
    @JsonProperty("perfilCompleto")
    private Boolean perfilCompleto;
    
    private String password; // solo para recepción; se nulificará antes de devolver

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