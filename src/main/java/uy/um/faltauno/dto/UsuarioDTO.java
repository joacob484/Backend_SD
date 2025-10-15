package uy.um.faltauno.dto;

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
    private String fotoPerfil;
    private String cedula;
    private Boolean cedulaVerificada;
    private String password; // solo para recepción; se nulificará antes de devolver
}