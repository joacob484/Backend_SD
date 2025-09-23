package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuario")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue
    private UUID id;

    private String nombre;
    private String apellido;
    private Integer edad;
    private String email;
    private String celular;
    private Double altura;
    private Double peso;
    private String posicion; // e.g. Arquero, Zaguero, etc.
    private String fotoPerfil;
    private String cedula; // para verificación
    private String provider; // GOOGLE, FACEBOOK, APPLE, LOCAL
    private String password; // null si es social login

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}