package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.um.faltauno.config.CustomUserDetailsService;
import uy.um.faltauno.dto.InscripcionDTO;
import uy.um.faltauno.dto.UsuarioMinDTO;
import uy.um.faltauno.entity.Inscripcion;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.SolicitudPartido;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.InscripcionRepository;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.repository.SolicitudPartidoRepository;
import uy.um.faltauno.repository.UsuarioRepository;
import uy.um.faltauno.util.InscripcionMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InscripcionService {

    private final InscripcionRepository inscripcionRepository;
    private final SolicitudPartidoRepository solicitudPartidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PartidoRepository partidoRepository;
    private final InscripcionMapper inscripcionMapper;
    private final NotificacionService notificacionService;

    /**
     * Crear solicitud para unirse a un partido.
     * El usuario queda en estado PENDIENTE hasta que el organizador lo acepte.
     */
    @Transactional
    public InscripcionDTO crearInscripcion(UUID partidoId, UUID usuarioId, Authentication auth) {
        log.info("[InscripcionService] Creando solicitud: partidoId={}, usuarioId={}", partidoId, usuarioId);
        
        UUID authUserId = getUserIdFromAuth(auth);
        if (!authUserId.equals(usuarioId)) {
            log.warn("[InscripcionService] Intento no autorizado: authUser={}, targetUser={}", 
                    authUserId, usuarioId);
            throw new SecurityException("No puedes inscribir a otro usuario");
        }

        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> {
                    log.error("[InscripcionService] Partido no encontrado: {}", partidoId);
                    return new IllegalArgumentException("Partido no encontrado");
                });
        
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> {
                    log.error("[InscripcionService] Usuario no encontrado: {}", usuarioId);
                    return new IllegalArgumentException("Usuario no encontrado");
                });

        validarInscripcion(partido, usuario);

        // Verificar si ya está inscrito (aceptado)
        if (inscripcionRepository.findByPartidoIdAndUsuarioId(partidoId, usuarioId).isPresent()) {
            log.warn("[InscripcionService] Usuario ya inscrito (aceptado)");
            throw new IllegalStateException("Ya estás inscrito en este partido");
        }

        // Verificar si ya tiene solicitud pendiente
        if (solicitudPartidoRepository.existsByPartidoIdAndUsuarioId(partidoId, usuarioId)) {
            log.warn("[InscripcionService] Ya existe solicitud pendiente");
            throw new IllegalStateException("Ya tienes una solicitud pendiente para este partido");
        }

        // Crear solicitud
        SolicitudPartido solicitud = SolicitudPartido.builder()
                .partido(partido)
                .usuario(usuario)
                .build();

        SolicitudPartido guardada = solicitudPartidoRepository.save(solicitud);
        log.info("[InscripcionService] ✅ Solicitud creada: id={}", guardada.getId());

        // Notificar al organizador
        try {
            notificacionService.notificarNuevaSolicitudInscripcion(
                    partido.getOrganizador().getId(),
                    usuarioId,
                    usuario.getNombre(),
                    partidoId,
                    partido.getNombreUbicacion()
            );
            log.info("[InscripcionService] 📧 Notificación enviada al organizador");
        } catch (Exception e) {
            log.error("[InscripcionService] ⚠️ Error enviando notificación", e);
        }

        // Convertir solicitud a DTO (usando mapper de inscripción por compatibilidad)
        InscripcionDTO dto = new InscripcionDTO();
        dto.setId(guardada.getId());
        dto.setPartidoId(partidoId);
        dto.setUsuarioId(usuarioId);
        dto.setCreatedAt(guardada.getCreatedAt());
        dto.setEstado("PENDIENTE"); // Para compatibilidad con frontend
        
        return dto;
    }

    @Transactional(readOnly = true)
    public List<InscripcionDTO> listarPorUsuario(UUID usuarioId, String estado) {
        log.debug("[InscripcionService] Listando inscripciones: usuarioId={}, estado={}", usuarioId, estado);
        
        // Con la nueva arquitectura:
        // - ACEPTADO: buscar en inscripcion
        // - PENDIENTE: buscar en solicitud_partido
        // - sin estado: buscar solo en inscripcion (inscripciones aceptadas)
        
        if (estado != null && !estado.isBlank()) {
            if ("PENDIENTE".equalsIgnoreCase(estado)) {
                // Buscar solicitudes pendientes
                List<SolicitudPartido> solicitudes = solicitudPartidoRepository.findByUsuarioId(usuarioId);
                return solicitudes.stream()
                        .map(this::convertSolicitudToDTO)
                        .collect(Collectors.toList());
            } else if ("ACEPTADO".equalsIgnoreCase(estado)) {
                // Buscar inscripciones aceptadas
                List<Inscripcion> inscripciones = inscripcionRepository.findByUsuarioId(usuarioId);
                return inscripciones.stream()
                        .map(inscripcionMapper::toDTO)
                        .collect(Collectors.toList());
            } else {
                log.warn("[InscripcionService] Estado no válido: {}", estado);
                return Collections.emptyList();
            }
        } else {
            // Sin estado: retornar solo inscripciones aceptadas
            List<Inscripcion> inscripciones = inscripcionRepository.findByUsuarioId(usuarioId);
            log.debug("[InscripcionService] Encontradas {} inscripciones", inscripciones.size());
            return inscripciones.stream()
                    .map(inscripcionMapper::toDTO)
                    .collect(Collectors.toList());
        }
    }

    @Transactional(readOnly = true)
    public List<InscripcionDTO> listarPorPartido(UUID partidoId, String estado) {
        log.debug("[InscripcionService] Listando inscripciones del partido: partidoId={}, estado={}", 
                partidoId, estado);
        
        // Con la nueva arquitectura:
        // - ACEPTADO: buscar en inscripcion
        // - PENDIENTE: buscar en solicitud_partido
        // - sin estado: buscar solo en inscripcion (inscripciones aceptadas)
        
        if (estado != null && !estado.isBlank()) {
            if ("PENDIENTE".equalsIgnoreCase(estado)) {
                // Buscar solicitudes pendientes
                List<SolicitudPartido> solicitudes = solicitudPartidoRepository.findByPartidoId(partidoId);
                return solicitudes.stream()
                        .map(this::convertSolicitudToDTO)
                        .collect(Collectors.toList());
            } else if ("ACEPTADO".equalsIgnoreCase(estado)) {
                // Buscar inscripciones aceptadas
                List<Inscripcion> inscripciones = inscripcionRepository.findByPartidoId(partidoId);
                return inscripciones.stream()
                        .map(inscripcionMapper::toDTO)
                        .collect(Collectors.toList());
            } else {
                log.warn("[InscripcionService] Estado no válido: {}", estado);
                return Collections.emptyList();
            }
        } else {
            // Sin estado: retornar solo inscripciones aceptadas
            List<Inscripcion> inscripciones = inscripcionRepository.findByPartidoId(partidoId);
            log.debug("[InscripcionService] Encontradas {} inscripciones", inscripciones.size());
            return inscripciones.stream()
                    .map(inscripcionMapper::toDTO)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Convierte SolicitudPartido a InscripcionDTO para compatibilidad con frontend
     */
    private InscripcionDTO convertSolicitudToDTO(SolicitudPartido solicitud) {
        InscripcionDTO dto = new InscripcionDTO();
        dto.setId(solicitud.getId());
        dto.setPartidoId(solicitud.getPartido().getId());
        dto.setUsuarioId(solicitud.getUsuario().getId());
        dto.setCreatedAt(solicitud.getCreatedAt());
        dto.setEstado("PENDIENTE"); // Siempre PENDIENTE en esta tabla
        
        // Agregar información del usuario si está disponible
        Usuario usuario = solicitud.getUsuario();
        if (usuario != null) {
            // Nota: Estos setters deben existir en InscripcionDTO
            // Si no existen, considerar crear un DTO específico para solicitudes
        }
        
        return dto;
    }

    @Transactional(readOnly = true)
    public List<InscripcionDTO> obtenerSolicitudesPendientes(UUID partidoId, Authentication auth) {
        log.info("[InscripcionService] Obteniendo solicitudes pendientes: partidoId={}", partidoId);
        
        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> {
                    log.error("[InscripcionService] Partido no encontrado: {}", partidoId);
                    return new IllegalArgumentException("Partido no encontrado");
                });

        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            log.warn("[InscripcionService] Acceso denegado: userId={} no es organizador", userId);
            throw new SecurityException("Solo el organizador puede ver las solicitudes");
        }

        // Obtener solicitudes desde la tabla solicitud_partido
        List<SolicitudPartido> pendientes = solicitudPartidoRepository.findByPartidoId(partidoId);

        log.info("[InscripcionService] ✅ Encontradas {} solicitudes pendientes", pendientes.size());

        // Convertir a DTO
        return pendientes.stream()
                .map(sol -> {
                    InscripcionDTO dto = new InscripcionDTO();
                    dto.setId(sol.getId());
                    dto.setPartidoId(sol.getPartido().getId());
                    dto.setUsuarioId(sol.getUsuario().getId());
                    dto.setCreatedAt(sol.getCreatedAt());
                    dto.setEstado("PENDIENTE"); // Para compatibilidad
                    
                    // Agregar info del usuario usando UsuarioMinDTO
                    Usuario usuario = sol.getUsuario();
                    UsuarioMinDTO usuarioMin = new UsuarioMinDTO(
                        usuario.getId(),
                        usuario.getNombre(),
                        usuario.getApellido(),
                        usuario.getFotoPerfil()
                    );
                    dto.setUsuario(usuarioMin);
                    
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Aceptar solicitud: elimina de solicitud_partido y crea inscripcion
     */
    @Transactional
    public InscripcionDTO aceptarInscripcion(UUID solicitudId, Authentication auth) {
        log.info("[InscripcionService] Aceptando solicitud: id={}", solicitudId);
        
        // Buscar solicitud
        SolicitudPartido solicitud = solicitudPartidoRepository.findById(solicitudId)
                .orElseThrow(() -> {
                    log.error("[InscripcionService] Solicitud no encontrada: {}", solicitudId);
                    return new IllegalArgumentException("Solicitud no encontrada");
                });

        Partido partido = solicitud.getPartido();
        Usuario usuario = solicitud.getUsuario();

        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            log.warn("[InscripcionService] Acceso denegado: userId={} no es organizador", userId);
            throw new SecurityException("Solo el organizador puede aceptar solicitudes");
        }

        // Verificar cupo
        long jugadoresActuales = inscripcionRepository.countByPartidoId(partido.getId());
        log.debug("[InscripcionService] Jugadores: {}/{}", jugadoresActuales, partido.getCantidadJugadores());

        if (jugadoresActuales >= partido.getCantidadJugadores()) {
            log.warn("[InscripcionService] Partido completo");
            throw new IllegalStateException("El partido está completo");
        }

        // Crear inscripción (usuario aceptado)
        Inscripcion inscripcion = Inscripcion.builder()
                .partido(partido)
                .usuario(usuario)
                .comentario(solicitud.getComentario())
                .build();

        Inscripcion guardada = inscripcionRepository.save(inscripcion);
        
        // Eliminar solicitud (ya fue aceptada)
        solicitudPartidoRepository.delete(solicitud);
        
        log.info("[InscripcionService] ✅ Solicitud aceptada y usuario inscrito: usuarioId={}", usuario.getId());

        // Notificar al jugador
        String nombrePartido = partido.getTipoPartido() + " - " + partido.getNombreUbicacion();
        notificacionService.notificarInscripcionAceptada(
                usuario.getId(), 
                partido.getId(), 
                nombrePartido
        );

        // Notificar si se llenó el cupo
        long nuevosJugadores = jugadoresActuales + 1;
        if (nuevosJugadores >= partido.getCantidadJugadores()) {
            log.info("[InscripcionService] ⚽ Partido completo ({}/{})", 
                    nuevosJugadores, partido.getCantidadJugadores());
            notificacionService.notificarPartidoListo(
                    partido.getOrganizador().getId(),
                    partido.getId(),
                    nombrePartido
            );
        }

        // Convertir a DTO
        InscripcionDTO dto = inscripcionMapper.toDTO(guardada);
        dto.setEstado("ACEPTADO"); // Para compatibilidad
        return dto;
    }

    /**
     * Rechazar solicitud: simplemente elimina de solicitud_partido
     */
    @Transactional
    public void rechazarInscripcion(UUID solicitudId, String motivo, Authentication auth) {
        log.info("[InscripcionService] Rechazando solicitud: id={}", solicitudId);
        
        // Buscar solicitud
        SolicitudPartido solicitud = solicitudPartidoRepository.findById(solicitudId)
                .orElseThrow(() -> {
                    log.error("[InscripcionService] Solicitud no encontrada: {}", solicitudId);
                    return new IllegalArgumentException("Solicitud no encontrada");
                });

        Partido partido = solicitud.getPartido();
        Usuario usuario = solicitud.getUsuario();

        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            log.warn("[InscripcionService] Acceso denegado: userId={} no es organizador", userId);
            throw new SecurityException("Solo el organizador puede rechazar solicitudes");
        }

        // Eliminar solicitud (= rechazo)
        solicitudPartidoRepository.delete(solicitud);
        
        log.info("[InscripcionService] ✅ Solicitud rechazada y eliminada: usuarioId={}", usuario.getId());
        
        // Notificar al usuario
        String nombrePartido = partido.getTipoPartido() + " - " + partido.getNombreUbicacion();
        notificacionService.notificarInscripcionRechazada(
                usuario.getId(), 
                partido.getId(), 
                nombrePartido,
                motivo
        );
    }

    @Transactional
    public void cancelarInscripcion(UUID inscripcionId, Authentication auth) {
        log.info("[InscripcionService] Cancelando inscripción: id={}", inscripcionId);
        
        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> {
                    log.error("[InscripcionService] Inscripción no encontrada: {}", inscripcionId);
                    return new IllegalArgumentException("Inscripción no encontrada");
                });

        UUID userId = getUserIdFromAuth(auth);
        if (!inscripcion.getUsuario().getId().equals(userId)) {
            log.warn("[InscripcionService] Acceso denegado: userId={} no es el inscrito", userId);
            throw new SecurityException("Solo puedes cancelar tu propia inscripción");
        }

        Partido partido = inscripcion.getPartido();

        // ✅ No permitir cancelar inscripciones en partidos CONFIRMADOS, COMPLETADOS o CANCELADOS
        if ("CONFIRMADO".equals(partido.getEstado())) {
            throw new IllegalStateException("No puedes cancelar tu inscripción a un partido confirmado. Contacta al organizador.");
        }
        if ("COMPLETADO".equals(partido.getEstado())) {
            throw new IllegalStateException("No puedes cancelar tu inscripción a un partido completado");
        }
        if ("CANCELADO".equals(partido.getEstado())) {
            throw new IllegalStateException("El partido fue cancelado");
        }

        LocalDateTime inicioPartido = LocalDateTime.of(partido.getFecha(), partido.getHora());
        if (inicioPartido.isBefore(LocalDateTime.now())) {
            log.warn("[InscripcionService] Partido ya pasó: fecha={} hora={}", 
                    partido.getFecha(), partido.getHora());
            throw new IllegalStateException("No puedes cancelar tu inscripción a un partido que ya pasó");
        }

        LocalDateTime limiteHoras = LocalDateTime.now().plusHours(2);
        if (inicioPartido.isBefore(limiteHoras)) {
            log.warn("[InscripcionService] Muy cerca del inicio: {}", inicioPartido);
            throw new IllegalStateException(
                "No puedes cancelar con menos de 2 horas de anticipación. " +
                "Por favor contacta al organizador."
            );
        }

        inscripcionRepository.delete(inscripcion);
        
        log.info("[InscripcionService] ✅ Usuario canceló inscripción: inscripcionId={}, usuarioId={}", 
                inscripcionId, userId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadoInscripcion(UUID partidoId, UUID usuarioId) {
        log.debug("[InscripcionService] Verificando estado: partidoId={}, usuarioId={}", 
                partidoId, usuarioId);
        
        Map<String, Object> resultado = new HashMap<>();
        
        // Verificar si está inscrito (aceptado)
        Optional<Inscripcion> inscripcion = inscripcionRepository
                .findByPartidoIdAndUsuarioId(partidoId, usuarioId);

        if (inscripcion.isPresent()) {
            // Usuario ACEPTADO (está en la tabla inscripcion)
            Inscripcion insc = inscripcion.get();
            resultado.put("inscrito", true);
            resultado.put("estado", "ACEPTADO"); // Siempre ACEPTADO si está en inscripcion
            resultado.put("inscripcionId", insc.getId());
            
            log.info("[InscripcionService] 📊 Usuario INSCRITO (aceptado) - partidoId={}, usuarioId={}, inscripcionId={}", 
                    partidoId, usuarioId, insc.getId());
        } else {
            // Verificar si tiene solicitud pendiente
            boolean tieneSolicitud = solicitudPartidoRepository
                    .existsByPartidoIdAndUsuarioId(partidoId, usuarioId);
            
            if (tieneSolicitud) {
                // Usuario tiene solicitud PENDIENTE
                resultado.put("inscrito", false);
                resultado.put("estado", "PENDIENTE");
                resultado.put("inscripcionId", null);
                log.info("[InscripcionService] 📊 Usuario con SOLICITUD PENDIENTE - partidoId={}, usuarioId={}", 
                        partidoId, usuarioId);
            } else {
                // Usuario NO inscrito ni solicitó
                resultado.put("inscrito", false);
                resultado.put("estado", null);
                resultado.put("inscripcionId", null);
                log.debug("[InscripcionService] Usuario NO inscrito ni solicitó");
            }
        }

        return resultado;
    }

    private void validarInscripcion(Partido partido, Usuario usuario) {
        LocalDateTime inicioPartido = LocalDateTime.of(partido.getFecha(), partido.getHora());
        if (inicioPartido.isBefore(LocalDateTime.now())) {
            log.warn("[InscripcionService] Partido ya pasó: {}", inicioPartido);
            throw new IllegalStateException("No puedes inscribirte a un partido que ya pasó");
        }

        if (partido.getOrganizador().getId().equals(usuario.getId())) {
            log.warn("[InscripcionService] Usuario es el organizador");
            throw new IllegalStateException("El organizador no puede inscribirse como jugador");
        }

        if ("CANCELADO".equals(partido.getEstado())) {
            log.warn("[InscripcionService] Partido cancelado");
            throw new IllegalStateException("No puedes inscribirte a un partido cancelado");
        }

        long jugadoresAceptados = inscripcionRepository.countByPartidoId(partido.getId());
        if (jugadoresAceptados >= partido.getCantidadJugadores()) {
            log.warn("[InscripcionService] Partido completo: {}/{}", 
                    jugadoresAceptados, partido.getCantidadJugadores());
            throw new IllegalStateException("El partido está completo");
        }

        if (usuario.getNombre() == null || usuario.getApellido() == null) {
            log.warn("[InscripcionService] Perfil incompleto: usuarioId={}", usuario.getId());
            throw new IllegalStateException(
                "Debes completar tu perfil antes de inscribirte a un partido"
            );
        }

        log.debug("[InscripcionService] ✅ Validaciones pasadas correctamente");
    }

    private UUID getUserIdFromAuth(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            log.error("[InscripcionService] Usuario no autenticado");
            throw new SecurityException("Usuario no autenticado");
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetailsService.UserPrincipal) {
            UUID userId = ((CustomUserDetailsService.UserPrincipal) principal).getId();
            log.debug("[InscripcionService] Usuario autenticado: {}", userId);
            return userId;
        }
        
        log.error("[InscripcionService] No se pudo obtener ID del usuario del principal");
        throw new SecurityException("No se pudo obtener el ID del usuario");
    }
}