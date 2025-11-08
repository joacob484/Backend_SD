package uy.um.faltauno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.um.faltauno.entity.Contacto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactoRepository extends JpaRepository<Contacto, Long> {
    
    /**
     * Obtener todos los contactos de un usuario
     */
    List<Contacto> findByUsuarioId(UUID usuarioId);
    
    /**
     * Buscar un contacto específico por usuario y celular
     */
    Optional<Contacto> findByUsuarioIdAndCelular(UUID usuarioId, String celular);
    
    /**
     * Eliminar todos los contactos de un usuario
     */
    void deleteByUsuarioId(UUID usuarioId);
    
    /**
     * Contar contactos de un usuario
     */
    long countByUsuarioId(UUID usuarioId);
}
