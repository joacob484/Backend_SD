package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.um.faltauno.config.CustomUserDetailsService;
import uy.um.faltauno.dto.MensajeDTO;
import uy.um.faltauno.dto.UsuarioMinDTO;
import uy.um.faltauno.entity.Inscripcion;
import uy.um.faltauno.entity.Mensaje;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.InscripcionRepository;
import uy.um.faltauno.repository.MensajeRepository;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.repository.UsuarioRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final PartidoRepository partidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InscripcionRepository inscripcionRepository;

    /**
     * Obtener mensajes del chat de un partido
     */
    @Transactional(readOnly = true)
    public List<MensajeDTO> obtenerMensajesPartido(UUID partidoId, int limit, Authentication auth) {
        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new RuntimeException("Partido no encontrado"));

        // Verificar que el usuario tenga acceso al chat
        UUID userId = getUserIdFromAuth(auth);
        validarAccesoChat(partido, userId);

        // Obtener mensajes ordenados por fecha descendente, limitados
        PageRequest pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Mensaje> mensajes = mensajeRepository.findByPartidoId(partidoId, pageable);

        // Convertir a DTO e invertir orden para mostrar más antiguos primero
        List<MensajeDTO> mensajesDTO = mensajes.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
        
        // Revertir para que los más antiguos aparezcan primero
        java.util.Collections.reverse(mensajesDTO);
        
        return mensajesDTO;
    }

    /**
     * Enviar un mensaje al chat
     */
    @Transactional
    public MensajeDTO enviarMensaje(UUID partidoId, MensajeDTO mensajeDTO, Authentication auth) {
        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new RuntimeException("Partido no encontrado"));

        UUID userId = getUserIdFromAuth(auth);
        
        // Verificar que el usuario tenga acceso al chat
        validarAccesoChat(partido, userId);

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar contenido
        if (mensajeDTO.getContenido() == null || mensajeDTO.getContenido().trim().isEmpty()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }

        if (mensajeDTO.getContenido().length() > 500) {
            throw new IllegalArgumentException("El mensaje no puede exceder 500 caracteres");
        }

        // Crear mensaje
        Mensaje mensaje = new Mensaje();
        mensaje.setPartidoId(partidoId);
        mensaje.setRemitenteId(userId);
        mensaje.setDestinatarioId(null); // Es un mensaje grupal
        mensaje.setContenido(mensajeDTO.getContenido().trim());
        mensaje.setLeido(false);
        mensaje.setCreatedAt(Instant.now());

        Mensaje guardado = mensajeRepository.save(mensaje);
        
        log.info("Mensaje enviado: partidoId={}, usuarioId={}", partidoId, userId);

        // TODO: Enviar notificación push a otros participantes
        
        return convertirADTO(guardado);
    }

    /**
     * Marcar mensajes como leídos
     */
    @Transactional
    public void marcarMensajesComoLeidos(UUID partidoId, Authentication auth) {
        UUID userId = getUserIdFromAuth(auth);
        
        List<Mensaje> mensajesNoLeidos = mensajeRepository
                .findByPartidoIdAndDestinatarioIdAndLeido(partidoId, userId, false);

        mensajesNoLeidos.forEach(m -> m.setLeido(true));
        mensajeRepository.saveAll(mensajesNoLeidos);
        
        log.info("Mensajes marcados como leídos: partidoId={}, usuarioId={}, cantidad={}", 
                partidoId, userId, mensajesNoLeidos.size());
    }

    /**
     * Eliminar un mensaje
     */
    @Transactional
    public void eliminarMensaje(UUID mensajeId, Authentication auth) {
        Mensaje mensaje = mensajeRepository.findById(mensajeId)
                .orElseThrow(() -> new RuntimeException("Mensaje no encontrado"));

        UUID userId = getUserIdFromAuth(auth);

        // Verificar que sea el autor o el organizador del partido
        Partido partido = partidoRepository.findById(mensaje.getPartidoId())
                .orElseThrow(() -> new RuntimeException("Partido no encontrado"));

        boolean esAutor = mensaje.getRemitenteId().equals(userId);
        boolean esOrganizador = partido.getOrganizador().getId().equals(userId);

        if (!esAutor && !esOrganizador) {
            throw new SecurityException("No tienes permiso para eliminar este mensaje");
        }

        mensajeRepository.delete(mensaje);
        log.info("Mensaje eliminado: id={}, partidoId={}", mensajeId, mensaje.getPartidoId());
    }

    // ===== MÉTODOS AUXILIARES =====

    private void validarAccesoChat(Partido partido, UUID userId) {
        // El organizador siempre tiene acceso
        if (partido.getOrganizador().getId().equals(userId)) {
            return;
        }

        // Verificar que el usuario esté inscrito y aceptado
        List<Inscripcion> inscripciones = inscripcionRepository.findByPartidoId(partido.getId());
        boolean estaInscrito = inscripciones.stream()
                .anyMatch(i -> i.getUsuario().getId().equals(userId) && 
                              "ACEPTADO".equals(i.getEstado()));

        if (!estaInscrito) {
            throw new SecurityException("No tienes acceso al chat de este partido");
        }
    }

    private MensajeDTO convertirADTO(Mensaje mensaje) {
        MensajeDTO dto = new MensajeDTO();
        dto.setId(mensaje.getId());
        dto.setUsuarioId(mensaje.getRemitenteId());
        dto.setPartidoId(mensaje.getPartidoId());
        dto.setContenido(mensaje.getContenido());
        dto.setCreatedAt(mensaje.getCreatedAt());
        dto.setLeido(mensaje.getLeido());

        // Cargar información del usuario
        try {
            Usuario usuario = usuarioRepository.findById(mensaje.getRemitenteId()).orElse(null);
            if (usuario != null) {
                UsuarioMinDTO usuarioMin = new UsuarioMinDTO(
                    usuario.getId(),
                    usuario.getNombre(),
                    usuario.getApellido(),
                    usuario.getFotoPerfil()
                );
                dto.setUsuario(usuarioMin);
            }
        } catch (Exception e) {
            log.warn("Error cargando usuario del mensaje: {}", e.getMessage());
        }

        return dto;
    }

    private UUID getUserIdFromAuth(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("Usuario no autenticado");
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetailsService.UserPrincipal) {
            return ((CustomUserDetailsService.UserPrincipal) principal).getId();
        }
        
        throw new SecurityException("No se pudo obtener el ID del usuario");
    }
}