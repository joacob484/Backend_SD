package uy.um.faltauno.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactoDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String celular;
    private Long usuarioAppId; // ID del usuario si está en la app
    private Boolean isOnApp;
    
    // Datos adicionales del usuario si está en la app
    private String fotoPerfil;
    private String email;
}
