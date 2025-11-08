package uy.um.faltauno.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un contacto importado desde el dispositivo del usuario
 */
@Entity
@Table(name = "contactos", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "celular"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contacto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Usuario dueño del contacto
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    /**
     * Nombre del contacto (del teléfono)
     */
    @Column(nullable = false)
    private String nombre;
    
    /**
     * Apellido del contacto (del teléfono) - puede estar vacío
     */
    @Column
    private String apellido;
    
    /**
     * Número de teléfono del contacto (normalizado)
     */
    @Column(nullable = false)
    private String celular;
    
    /**
     * Usuario de la app que coincide con este contacto (si existe)
     * Null si el contacto no está registrado en la app
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_app_id")
    private Usuario usuarioApp;
    
    /**
     * Indica si el contacto está en la app
     */
    @Column(name = "is_on_app", nullable = false)
    private Boolean isOnApp = false;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
