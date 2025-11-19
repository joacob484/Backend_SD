package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.um.faltauno.dto.ContactoDTO;
import uy.um.faltauno.entity.Contacto;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.mapper.ContactoMapper;
import uy.um.faltauno.repository.ContactoRepository;
import uy.um.faltauno.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContactoService {
    
    private final ContactoRepository contactoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContactoMapper contactoMapper;
    
    /**
     * Obtener todos los contactos de un usuario
     */
    @Transactional(readOnly = true)
    public List<ContactoDTO> listarContactos(Usuario usuario) {
        try {
            log.info("Listando contactos para usuario: {}", usuario.getId());
            
            List<Contacto> contactos = contactoRepository.findByUsuarioId(usuario.getId());
            log.info("Encontrados {} contactos", contactos.size());
            
            if (contactos.isEmpty()) {
                log.info("Usuario no tiene contactos sincronizados, devolviendo lista vacía");
                return new ArrayList<>();
            }
            
            List<ContactoDTO> dtos = contactoMapper.toDTOList(contactos);
            log.info("Convertidos {} contactos a DTO", dtos.size());
            
            return dtos;
        } catch (Exception e) {
            log.error("Error al listar contactos para usuario {}: {}", usuario.getId(), e.getMessage(), e);
            throw new RuntimeException("Error al listar contactos", e);
        }
    }
    
    /**
     * Sincronizar contactos del dispositivo con la base de datos
     * @param usuario Usuario que sincroniza
     * @param contactosRequest Lista de contactos del dispositivo con formato:
     *                         [{"nombre": "Juan", "apellido": "Perez", "email": "juan@example.com"}, ...]
     * @return Lista de contactos sincronizados con información de cuáles están en la app
     */
    @Transactional
    public List<ContactoDTO> sincronizarContactos(Usuario usuario, List<Map<String, String>> contactosRequest) {
        log.info("Sincronizando {} contactos para usuario {}", contactosRequest.size(), usuario.getId());
        
        // Eliminar contactos anteriores
        contactoRepository.deleteByUsuarioId(usuario.getId());
        
        // Extraer emails
        List<String> emails = contactosRequest.stream()
                .map(c -> c.get("email"))
                .filter(e -> e != null && !e.trim().isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Buscando {} emails únicos en la app", emails.size());
        
        // Buscar usuarios existentes por email usando el método ya existente en repository
        List<Usuario> usuariosEnApp = usuarioRepository.findAllActive().stream()
                .filter(u -> u.getEmail() != null && emails.contains(u.getEmail().toLowerCase()))
                .collect(Collectors.toList());
        
        Map<String, Usuario> usuariosPorEmail = usuariosEnApp.stream()
                .collect(Collectors.toMap(u -> u.getEmail().toLowerCase(), u -> u));
        
        log.info("Encontrados {} usuarios en la app", usuariosEnApp.size());
        
        // Crear contactos
        List<Contacto> contactos = new ArrayList<>();
        for (Map<String, String> contactoData : contactosRequest) {
            String email = contactoData.get("email");
            
            if (email == null || email.trim().isEmpty()) {
                continue;
            }
            
            Contacto contacto = new Contacto();
            contacto.setUsuario(usuario);
            contacto.setNombre(contactoData.getOrDefault("nombre", ""));
            contacto.setApellido(contactoData.getOrDefault("apellido", ""));
            contacto.setCelular(email); // Reusamos el campo celular para guardar el email
            
            // Verificar si el contacto está en la app
            Usuario usuarioApp = usuariosPorEmail.get(email.toLowerCase());
            if (usuarioApp != null) {
                contacto.setUsuarioApp(usuarioApp);
                contacto.setIsOnApp(true);
            } else {
                contacto.setIsOnApp(false);
            }
            
            contactos.add(contacto);
        }
        
        // Guardar todos los contactos
        List<Contacto> savedContactos = contactoRepository.saveAll(contactos);
        log.info("Guardados {} contactos, {} están en la app", 
                savedContactos.size(),
                savedContactos.stream().filter(Contacto::getIsOnApp).count());
        
        return contactoMapper.toDTOList(savedContactos);
    }
    
    /**
     * Actualizar estado de contactos cuando un nuevo usuario se registra
     */
    @Transactional
    public void actualizarContactosConNuevoUsuario(Usuario nuevoUsuario) {
        if (nuevoUsuario.getEmail() == null) {
            return;
        }
        
        log.info("Actualizando contactos para nuevo usuario con email: {}", nuevoUsuario.getEmail());
        
        // Buscar todos los contactos que tengan este email (guardado en campo celular)
        String emailLower = nuevoUsuario.getEmail().toLowerCase();
        List<Contacto> contactosAActualizar = contactoRepository.findAll().stream()
                .filter(c -> c.getCelular() != null && emailLower.equals(c.getCelular().toLowerCase()))
                .collect(Collectors.toList());
        
        log.info("Encontrados {} contactos a actualizar", contactosAActualizar.size());
        
        // Actualizar cada contacto
        for (Contacto contacto : contactosAActualizar) {
            contacto.setUsuarioApp(nuevoUsuario);
            contacto.setIsOnApp(true);
        }
        
        contactoRepository.saveAll(contactosAActualizar);
    }
}
