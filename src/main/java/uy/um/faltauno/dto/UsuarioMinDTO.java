package uy.um.faltauno.dto;

import java.util.UUID;

public class UsuarioMinDTO {
    private String id;
    private String nombre;
    private String apellido;
    private byte[] fotoPerfil;

    // Constructor sin parámetros para Jackson
    public UsuarioMinDTO() {
    }

    public UsuarioMinDTO(UUID id, String nombre, String apellido, byte[] fotoPerfil) {
        this.id = id.toString();
        this.nombre = nombre;
        this.apellido = apellido;
        this.fotoPerfil = fotoPerfil;
    }

    // getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public byte[] getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(byte[] fotoPerfil) { this.fotoPerfil = fotoPerfil; }
}