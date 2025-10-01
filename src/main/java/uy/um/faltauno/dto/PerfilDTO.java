package uy.um.faltauno.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilDTO {
    private String nombre;
    private String apellido;
    private String celular;
    private String posicion;
    private String altura; // stringify from frontend
    private String peso;   // stringify from frontend
    private String direccion; // opcional
    private String placeDetails; // JSON string de Google Places (opcional)
}