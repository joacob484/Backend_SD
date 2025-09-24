package uy.um.faltauno.dto;

import java.util.UUID;

public class UsuarioMinDTO {
    private String id;
    private String nombre;
    private String apellido;
    private String foto_perfil;

    public UsuarioMinDTO(UUID id, String nombre, String apellido, String foto_perfil) {
        this.id = id.toString(); // convertimos UUID a String
        this.nombre = nombre;
        this.apellido = apellido;
        this.foto_perfil = foto_perfil;
    }

    // getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getFoto_perfil() { return foto_perfil; }
    public void setFoto_perfil(String foto_perfil) { this.foto_perfil = foto_perfil; }
}
