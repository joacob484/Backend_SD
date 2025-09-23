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
    private Integer edad;
    private String email;
    private String celular;
    private Double altura;
    private Double peso;
    private String posicion;
    private String fotoPerfil;
    private String cedula;
}
