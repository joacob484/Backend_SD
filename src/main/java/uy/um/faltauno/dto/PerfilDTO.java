package uy.um.faltauno.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilDTO {
    private String nombre;
    private String apellido;
    
    /**
     * Número de celular en formato internacional: +XXX XXXXXXXXX
     * Acepta entre 6 y 30 caracteres, comenzando con + opcional
     * Ejemplos válidos: +598 91234567, +54 1123456789, +1 2025551234
     */
    @Pattern(regexp = "^\\+?[0-9\\s]{6,30}$", 
             message = "Número de celular inválido. Formato esperado: +XXX XXXXXXXXX")
    private String celular;
    private String posicion;
    private String altura;       // stringify from frontend
    private String peso;         // stringify from frontend
    private String direccion;    // opcional
    private String placeDetails; // JSON string de Google Places (opcional)
    private String genero;       // Masculino o Femenino

    @JsonProperty("fecha_nacimiento") // mapea JSON "fecha_nacimiento" a este campo camelCase
    private String fechaNacimiento;
}