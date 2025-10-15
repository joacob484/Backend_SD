package uy.um.faltauno.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String altura;       // stringify from frontend
    private String peso;         // stringify from frontend
    private String direccion;    // opcional
    private String placeDetails; // JSON string de Google Places (opcional)

    @JsonProperty("fecha_nacimiento") // mapea JSON "fecha_nacimiento" a este campo camelCase
    private String fechaNacimiento;
}