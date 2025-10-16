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
        if (perfilCompleto != null) {
            return perfilCompleto;
        }
        // Perfil completo si tiene: nombre, apellido, celular, fechaNacimiento
        return nombre != null && !nombre.isEmpty()
                && apellido != null && !apellido.isEmpty()
                && celular != null && !celular.isEmpty()
                && fechaNacimiento != null && !fechaNacimiento.isEmpty();
    }

    /**
     * Calcula si la cédula está verificada.
     */
    public Boolean getCedulaVerificada() {
        if (cedulaVerificada != null) {
            return cedulaVerificada;
        }
        // Cédula verificada si existe y no está vacía
        return cedula != null && !cedula.isEmpty();
    }
}