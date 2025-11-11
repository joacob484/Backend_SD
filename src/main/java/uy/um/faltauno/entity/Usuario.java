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
    
    /**
     * Número de celular en formato internacional: +XXX XXXXXXXXX
     * Ejemplo: +598 91234567 (Uruguay), +54 1123456789 (Argentina)
     * Máximo 30 caracteres para soportar códigos de país y extensiones.
     */
    @Column(length = 30)
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
     * Rol del usuario: USER (normal) o ADMIN (administrador)
     */
    @Column(nullable = false)
    @Builder.Default
    private String rol = "USER";
    
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

    /**
     * Versión del token JWT para invalidación masiva.
     * Se incrementa cada vez que se necesita invalidar todos los tokens del usuario
     * (cambio de contraseña, compromiso de seguridad, etc.)
     * Estándar de la industria para token management.
     */
    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private Integer tokenVersion = 1;

    /**
     * Indicador de si el email ha sido verificado
     */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    /**
     * Código de verificación de email (6 dígitos)
     */
    @Column(name = "verification_code", length = 6)
    private String verificationCode;

    /**
     * Fecha de expiración del código de verificación
     */
    @Column(name = "verification_code_expires_at")
    private LocalDateTime verificationCodeExpiresAt;

    /**
     * Indicador de si el celular ha sido verificado
     */
    @Column(name = "celular_verificado", nullable = false)
    @Builder.Default
    private Boolean celularVerificado = false;

    /**
     * Código de verificación de celular (6 dígitos)
     */
    @Column(name = "codigo_verificacion", length = 6)
    private String codigoVerificacion;

    /**
     * Fecha de expiración del código de verificación de celular
     */
    @Column(name = "codigo_verificacion_expira")
    private LocalDateTime codigoVerificacionExpira;

    /**
     * Contador de intentos fallidos de verificación de celular (máx 3)
     */
    @Column(name = "codigo_verificacion_intentos", nullable = false)
    @Builder.Default
    private Integer codigoVerificacionIntentos = 0;

    /**
     * Última actividad del usuario (actualizada en cada request autenticado)
     * Permite calcular "Usuarios activos ahora" en tiempo real
     */
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    /**
     * Soft delete: fecha y hora de eliminación lógica del usuario.
     * NULL = usuario activo, NOT NULL = usuario eliminado.
     * Permite preservar integridad referencial y auditoría.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Usuario baneado: fecha y hora del baneo.
     * NULL = usuario no baneado, NOT NULL = usuario baneado.
     * Los usuarios baneados no pueden iniciar sesión ni realizar acciones.
     */
    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    /**
     * Razón del baneo
     */
    @Column(name = "ban_reason", length = 500)
    private String banReason;

    /**
     * Fecha hasta la cual el usuario está baneado (NULL = baneo permanente)
     */
    @Column(name = "ban_until")
    private LocalDateTime banUntil;

    /**
     * Admin que realizó el baneo
     */
    @Column(name = "banned_by")
    private UUID bannedBy;

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