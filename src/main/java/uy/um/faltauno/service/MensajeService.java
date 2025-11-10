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
import uy.um.faltauno.entity.ChatVisit;
import uy.um.faltauno.repository.InscripcionRepository;
import uy.um.faltauno.repository.MensajeRepository;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.repository.UsuarioRepository;
import uy.um.faltauno.repository.ChatVisitRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
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
    private final NotificacionService notificacionService;
    private final ChatVisitRepository chatVisitRepository;

    /**
     * Obtener mensajes del chat de un partido
     */
    @Transactional(readOnly = true)
    public List<MensajeDTO> obtenerMensajesPartido(UUID partidoId, int limit, Authentication auth) {
        log.debug("[MensajeService] Obteniendo mensajes: partidoId={}, limit={}", partidoId, limit);
        
        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> {
                    log.error("[MensajeService] Partido no encontrado: {}", partidoId);
                    return new IllegalArgumentException("Partido no encontrado");
                });

        UUID userId = getUserIdFromAuth(auth);
        validarAccesoChat(partido, userId);

        // Obtener mensajes con paginación
        PageRequest pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Mensaje> mensajes = mensajeRepository.findByPartidoIdOrderByCreatedAtDesc(partidoId, pageable);

        log.debug("[MensajeService] ✅ Encontrados {} mensajes", mensajes.size());

        // Convertir a DTO e invertir orden
        List<MensajeDTO> mensajesDTO = mensajes.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
        
        Collections.reverse(mensajesDTO); // Más antiguos primero
        
        return mensajesDTO;
    }

    /**
     * Enviar un mensaje al chat del partido
     */
    @Transactional
    public MensajeDTO enviarMensaje(UUID partidoId, MensajeDTO mensajeDTO, Authentication auth) {
        log.info("[MensajeService] Enviando mensaje a partido: {}", partidoId);
        
        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> {
                    log.error("[MensajeService] Partido no encontrado: {}", partidoId);
                    return new IllegalArgumentException("Partido no encontrado");
                });

        UUID userId = getUserIdFromAuth(auth);
        validarAccesoChat(partido, userId);

        // Validar que el usuario existe
        if (!usuarioRepository.existsById(userId)) {
            log.error("[MensajeService] Usuario no encontrado: {}", userId);
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        // Validar contenido
        if (mensajeDTO.getContenido() == null || mensajeDTO.getContenido().trim().isEmpty()) {
            log.warn("[MensajeService] Intento de enviar mensaje vacío");
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }

        if (mensajeDTO.getContenido().length() > 500) {
            log.warn("[MensajeService] Mensaje excede límite de caracteres");
            throw new IllegalArgumentException("El mensaje no puede exceder 500 caracteres");
        }

        // ✅ USAR Builder con UUID directamente
        Mensaje mensaje = Mensaje.builder()
                .partidoId(partidoId)
                .remitenteId(userId)
                .destinatarioId(null) // Mensaje grupal
                .contenido(mensajeDTO.getContenido().trim())
                .leido(false)
                .createdAt(Instant.now())
                .build();

        Mensaje guardado = mensajeRepository.save(mensaje);
        
        log.info("[MensajeService] ✅ Mensaje enviado: id={}, partidoId={}, usuarioId={}", 
                guardado.getId(), partidoId, userId);

        // Notificar a los participantes del partido (excepto al remitente)
        notificarParticipantes(partido, userId, guardado);
        
        return convertirADTO(guardado);
    }

    /**
     * Marcar mensajes como leídos
     */
    @Transactional
    public void marcarMensajesComoLeidos(UUID partidoId, Authentication auth) {
        log.debug("[MensajeService] Marcando mensajes como leídos: partidoId={}", partidoId);
        
        UUID userId = getUserIdFromAuth(auth);
        
        int actualizados = mensajeRepository.marcarMensajesComoLeidos(partidoId, userId);
        
        log.info("[MensajeService] ✅ Mensajes marcados como leídos: {}", actualizados);
    }

    /**
     * Eliminar un mensaje (solo autor o organizador)
     */
    @Transactional
    public void eliminarMensaje(UUID mensajeId, Authentication auth) {
        log.info("[MensajeService] Eliminando mensaje: id={}", mensajeId);
        
        Mensaje mensaje = mensajeRepository.findById(mensajeId)
                .orElseThrow(() -> {
                    log.error("[MensajeService] Mensaje no encontrado: {}", mensajeId);
                    return new IllegalArgumentException("Mensaje no encontrado");
                });

        UUID userId = getUserIdFromAuth(auth);

        Partido partido = partidoRepository.findById(mensaje.getPartidoId())
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));

        boolean esAutor = mensaje.getRemitenteId().equals(userId);
        boolean esOrganizador = partido.getOrganizador().getId().equals(userId);

        if (!esAutor && !esOrganizador) {
            log.warn("[MensajeService] Acceso denegado: userId={}", userId);
            throw new SecurityException("No tienes permiso para eliminar este mensaje");
        }

        mensajeRepository.delete(mensaje);
        log.info("[MensajeService] ✅ Mensaje eliminado: id={}", mensajeId);
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Notificar a todos los participantes del partido (excepto al remitente)
     */
    private void notificarParticipantes(Partido partido, UUID remitenteId, Mensaje mensaje) {
        try {
            Usuario remitente = usuarioRepository.findById(remitenteId).orElse(null);
            if (remitente == null) {
                log.warn("[MensajeService] No se pudo cargar remitente para notificaciones");
                return;
            }

            String nombreRemitente = remitente.getNombre() + " " + remitente.getApellido();
            String nombrePartido = partido.getTipoPartido() + " en " + partido.getNombreUbicacion();

            // Notificar al organizador (si no es el remitente)
            if (!partido.getOrganizador().getId().equals(remitenteId)) {
                notificacionService.notificarNuevoMensaje(
                    partido.getOrganizador().getId(),
                    partido.getId(),
                    nombrePartido,
                    nombreRemitente
                );
            }

            // Notificar a todos los jugadores inscritos (excepto remitente)
            // Con la nueva arquitectura, todos en la tabla inscripcion están aceptados
            List<Inscripcion> inscripciones = inscripcionRepository.findByPartidoId(partido.getId());
            inscripciones.stream()
                    .filter(i -> !i.getUsuario().getId().equals(remitenteId))
                    .forEach(i -> {
                        notificacionService.notificarNuevoMensaje(
                            i.getUsuario().getId(),
                            partido.getId(),
                            nombrePartido,
                            nombreRemitente
                        );
                    });

            log.debug("[MensajeService] ✅ Notificaciones de mensaje enviadas");
        } catch (Exception e) {
            log.error("[MensajeService] Error enviando notificaciones de mensaje: {}", e.getMessage());
        }
    }

    /**
     * Validar que el usuario tenga acceso al chat del partido
     */
    private void validarAccesoChat(Partido partido, UUID userId) {
        log.debug("[MensajeService] Validando acceso: partidoId={}, userId={}, organizadorId={}", 
                partido.getId(), userId, partido.getOrganizador().getId());
        
        // Organizador siempre tiene acceso
        if (partido.getOrganizador().getId().equals(userId)) {
            log.debug("[MensajeService] ✅ Acceso concedido: usuario es organizador");
            return;
        }

        // Verificar inscripción (con la nueva arquitectura, todos en inscripcion están aceptados)
        List<Inscripcion> inscripciones = inscripcionRepository.findByPartidoId(partido.getId());
        log.debug("[MensajeService] Total inscripciones encontradas: {}", inscripciones.size());
        
        // Log de cada inscripción para debug
        inscripciones.forEach(i -> 
            log.debug("[MensajeService] Inscripción: usuarioId={}", 
                    i.getUsuario().getId())
        );
        
        boolean estaInscrito = inscripciones.stream()
                .anyMatch(i -> i.getUsuario().getId().equals(userId));

        if (!estaInscrito) {
            log.warn("[MensajeService] ❌ Acceso denegado: usuario no inscrito o no aceptado. UserId buscado: {}", userId);
            throw new SecurityException("No tienes acceso al chat de este partido");
        }
        
        log.debug("[MensajeService] ✅ Acceso concedido: usuario inscrito y aceptado");
    }

    /**
     * Convertir Mensaje a MensajeDTO
     */
    private MensajeDTO convertirADTO(Mensaje mensaje) {
        MensajeDTO dto = MensajeDTO.builder()
                .id(mensaje.getId())
                .usuarioId(mensaje.getRemitenteId())
                .partidoId(mensaje.getPartidoId())
                .contenido(mensaje.getContenido())
                .createdAt(mensaje.getCreatedAt())
                .leido(mensaje.getLeido())
                .build();

        // Cargar información del remitente
        try {
            Usuario usuario = usuarioRepository.findById(mensaje.getRemitenteId()).orElse(null);
            if (usuario != null) {
                String fotoPerfil = null;
                if (usuario.getFotoPerfil() != null) {
                    try {
                        fotoPerfil = java.util.Base64.getEncoder().encodeToString(usuario.getFotoPerfil());
                    } catch (Exception ex) {
                        log.warn("[MensajeService] Error encoding foto: {}", ex.getMessage());
                    }
                }
                UsuarioMinDTO usuarioMin = new UsuarioMinDTO(
                    usuario.getId(),
                    usuario.getNombre(),
                    usuario.getApellido(),
                    fotoPerfil
                );
                dto.setUsuario(usuarioMin);
            }
        } catch (Exception e) {
            log.warn("[MensajeService] Error cargando usuario del mensaje: {}", e.getMessage());
        }

        return dto;
    }

    /**
     * Obtener ID del usuario autenticado
     */
    private UUID getUserIdFromAuth(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            log.error("[MensajeService] Usuario no autenticado");
            throw new SecurityException("Usuario no autenticado");
        }
        
        Object principal = auth.getPrincipal();
        
        // ✅ NUEVO: Soporte para Usuario como principal (JWT auth)
        if (principal instanceof uy.um.faltauno.entity.Usuario) {
            UUID userId = ((uy.um.faltauno.entity.Usuario) principal).getId();
            log.debug("[MensajeService] Usuario autenticado (JWT): {}", userId);
            return userId;
        }
        
        if (principal instanceof CustomUserDetailsService.UserPrincipal) {
            UUID userId = ((CustomUserDetailsService.UserPrincipal) principal).getId();
            log.debug("[MensajeService] Usuario autenticado: {}", userId);
            return userId;
        }
        
        log.error("[MensajeService] No se pudo obtener ID del usuario");
        throw new SecurityException("No se pudo obtener el ID del usuario");
    }
    
    /**
     * Registrar que un usuario visitó el chat de un partido
     * Esto actualiza la última visita para calcular mensajes no leídos
     */
    @Transactional
    public void registrarVisitaChat(UUID partidoId, UUID usuarioId) {
        log.debug("[MensajeService] Registrando visita al chat: partido={}, usuario={}", partidoId, usuarioId);
        
        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));
        
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        // Buscar visita existente o crear nueva
        ChatVisit visit = chatVisitRepository.findByUsuarioAndPartido(usuario, partido)
                .orElseGet(() -> {
                    ChatVisit newVisit = new ChatVisit();
                    newVisit.setUsuario(usuario);
                    newVisit.setPartido(partido);
                    return newVisit;
                });
        
        // Actualizar timestamp
        visit.setLastVisitAt(LocalDateTime.now());
        chatVisitRepository.save(visit);
        
        log.debug("[MensajeService] ✅ Visita registrada correctamente");
    }
    
    /**
     * Contar mensajes no leídos en un chat
     * Cuenta los mensajes posteriores a la última visita del usuario
     */
    @Transactional(readOnly = true)
    public long contarMensajesNoLeidos(UUID partidoId, UUID usuarioId) {
        log.debug("[MensajeService] Contando mensajes no leídos: partido={}, usuario={}", partidoId, usuarioId);
        
        // Buscar última visita
        var visitOpt = chatVisitRepository.findByUsuarioIdAndPartidoId(usuarioId, partidoId);
        
        Instant cutoffTime;
        if (visitOpt.isEmpty()) {
            // Si nunca visitó, usar fecha muy antigua para contar todos los mensajes
            cutoffTime = Instant.ofEpochMilli(0); // 1970-01-01
        } else {
            // Convertir LocalDateTime a Instant
            cutoffTime = visitOpt.get().getLastVisitAt()
                .atZone(ZoneId.systemDefault())
                .toInstant();
        }
        
        // Contar mensajes después del cutoff, excluyendo los del propio usuario
        long unread = mensajeRepository.countByPartidoIdAndCreatedAtAfterAndRemitenteIdNot(
            partidoId, 
            cutoffTime,
            usuarioId
        );
        
        log.debug("[MensajeService] ✅ {} mensajes no leídos", unread);
        return unread;
    }
}