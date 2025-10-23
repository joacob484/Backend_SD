package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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
    private LocalDate fechaNacimiento;
    private String email;
    private String celular;
    private Double altura;
    private Double peso;
    private String posicion;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "foto_perfil", columnDefinition = "bytea")
    private byte[] fotoPerfil;

    private String cedula;
    
    /**
     * Género del usuario: Masculino o Femenino
     */
    private String genero;
    
    /**
     * Proveedor de autenticación (LOCAL para email/password, GOOGLE para OAuth, etc.)
     * - LOCAL: Usuario registrado con email y contraseña
     * - GOOGLE: Usuario registrado con Google OAuth (password = null)
     */
    private String provider;
    
    /**
     * Contraseña encriptada del usuario.
     * IMPORTANTE: Este campo es NULL para usuarios que se registran con OAuth (Google, Facebook, etc.)
     * Solo los usuarios con provider = "LOCAL" tienen contraseña.
     */
    private String password;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Preferencias de notificaciones por email
    @Column(name = "notif_email_invitaciones")
    @Builder.Default
    private Boolean notifEmailInvitaciones = true;

    @Column(name = "notif_email_solicitudes_amistad")
    @Builder.Default
    private Boolean notifEmailSolicitudesAmistad = true;

    @Column(name = "notif_email_actualizaciones_partido")
    @Builder.Default
    private Boolean notifEmailActualizacionesPartido = true;

    @Column(name = "notif_email_solicitudes_review")
    @Builder.Default
    private Boolean notifEmailSolicitudesReview = true;

    @Column(name = "notif_email_nuevos_mensajes")
    @Builder.Default
    private Boolean notifEmailNuevosMensajes = false;

    @Column(name = "notif_email_generales")
    @Builder.Default
    private Boolean notifEmailGenerales = false;

    // getters/setters generados por Lombok (@Getter/@Setter) seguirán funcionando
}