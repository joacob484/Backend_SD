package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "usuario")
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
    private String posicion;

    @Lob
    @Column(name = "foto_perfil")
    private byte[] fotoPerfil; // BYTEA en Postgres

    private String cedula;
    private String provider;
    private String password;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}