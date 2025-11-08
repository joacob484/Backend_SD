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
     *                         [{"nombre": "Juan", "apellido": "Perez", "celular": "+59899123456"}, ...]
     * @return Lista de contactos sincronizados con información de cuáles están en la app
     */
    @Transactional
    public List<ContactoDTO> sincronizarContactos(Usuario usuario, List<Map<String, String>> contactosRequest) {
        log.info("Sincronizando {} contactos para usuario {}", contactosRequest.size(), usuario.getId());
        
        // Eliminar contactos anteriores
        contactoRepository.deleteByUsuarioId(usuario.getId());
        
        // Extraer números de teléfono
        List<String> telefonos = contactosRequest.stream()
                .map(c -> c.get("celular"))
                .filter(t -> t != null && !t.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Buscando {} números de teléfono únicos en la app", telefonos.size());
        
        // Buscar usuarios existentes por teléfono
        List<Usuario> usuariosEnApp = usuarioRepository.findByCelularIn(telefonos);
        Map<String, Usuario> usuariosPorTelefono = usuariosEnApp.stream()
                .collect(Collectors.toMap(Usuario::getCelular, u -> u));
        
        log.info("Encontrados {} usuarios en la app", usuariosEnApp.size());
        
        // Crear contactos
        List<Contacto> contactos = new ArrayList<>();
        for (Map<String, String> contactoData : contactosRequest) {
            String celular = contactoData.get("celular");
            
            if (celular == null || celular.trim().isEmpty()) {
                continue;
            }
            
            Contacto contacto = new Contacto();
            contacto.setUsuario(usuario);
            contacto.setNombre(contactoData.getOrDefault("nombre", ""));
            contacto.setApellido(contactoData.getOrDefault("apellido", ""));
            contacto.setCelular(celular);
            
            // Verificar si el contacto está en la app
            Usuario usuarioApp = usuariosPorTelefono.get(celular);
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
        if (nuevoUsuario.getCelular() == null) {
            return;
        }
        
        log.info("Actualizando contactos para nuevo usuario con teléfono: {}", nuevoUsuario.getCelular());
        
        // Buscar todos los contactos que tengan este número
        List<Contacto> contactosAActualizar = contactoRepository.findAll().stream()
                .filter(c -> nuevoUsuario.getCelular().equals(c.getCelular()))
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
