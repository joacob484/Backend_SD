package uy.um.faltauno.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class UsuarioMinDTO {
    private String id;
    private String nombre;
    private String apellido;
    private String fotoPerfil; // Base64 encoded string
    private LocalDateTime deletedAt; // Para indicar si el usuario está eliminado

    // Constructor sin parámetros para Jackson
    public UsuarioMinDTO() {
    }

    public UsuarioMinDTO(UUID id, String nombre, String apellido, String fotoPerfil) {
        this.id = id.toString();
        this.nombre = nombre;
        this.apellido = apellido;
        this.fotoPerfil = fotoPerfil;
    }

    public UsuarioMinDTO(UUID id, String nombre, String apellido, String fotoPerfil, LocalDateTime deletedAt) {
        this.id = id.toString();
        this.nombre = nombre;
        this.apellido = apellido;
        this.fotoPerfil = fotoPerfil;
        this.deletedAt = deletedAt;
    }

    // getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}