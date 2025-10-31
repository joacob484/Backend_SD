package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.um.faltauno.config.CustomUserDetailsService;
import uy.um.faltauno.dto.InscripcionDTO;
import uy.um.faltauno.entity.Inscripcion;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.InscripcionRepository;
import uy.um.faltauno.repository.PartidoRepository;
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
    private final UsuarioRepository usuarioRepository;
    private final PartidoRepository partidoRepository;
    private final InscripcionMapper inscripcionMapper;
    private final NotificacionService notificacionService;

    @Transactional
    public InscripcionDTO crearInscripcion(UUID partidoId, UUID usuarioId, Authentication auth) {
        log.info("[InscripcionService] Creando inscripción: partidoId={}, usuarioId={}", partidoId, usuarioId);
        
        UUID authUserId = getUserIdFromAuth(auth);
        if (!authUserId.equals(usuarioId)) {
            log.warn("[InscripcionService] Intento de inscripción no autorizado: authUser={}, targetUser={}", 
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

        Optional<Inscripcion> existente = inscripcionRepository
                .findByPartidoIdAndUsuarioId(partidoId, usuarioId);

        if (existente.isPresent()) {
            Inscripcion insc = existente.get();
            Inscripcion.EstadoInscripcion estadoActual = insc.getEstado();
            
            log.info("[InscripcionService] Ya existe inscripción con estado: {}", estadoActual);
            
            if (estadoActual == Inscripcion.EstadoInscripcion.ACEPTADO) {
                throw new IllegalStateException("Ya estás inscrito en este partido");
            } else if (estadoActual == Inscripcion.EstadoInscripcion.PENDIENTE) {
                throw new IllegalStateException("Ya tienes una solicitud pendiente para este partido");
            } else if (estadoActual == Inscripcion.EstadoInscripcion.RECHAZADO) {
                log.info("[InscripcionService] Reactivando solicitud rechazada");
                insc.setEstado(Inscripcion.EstadoInscripcion.PENDIENTE);
                insc.setCreatedAt(java.time.Instant.now());
                Inscripcion reactivada = inscripcionRepository.save(insc);
                
                return inscripcionMapper.toDTO(reactivada);
            }
        }

        Inscripcion inscripcion = Inscripcion.builder()
                .partido(partido)
                .usuario(usuario)
                .estado(Inscripcion.EstadoInscripcion.PENDIENTE)
                .build();

        Inscripcion guardada = inscripcionRepository.save(inscripcion);
        log.info("[InscripcionService] ✅ Inscripción creada exitosamente: id={}, estado=PENDIENTE", 
                guardada.getId());

        // Notificar al organizador sobre la nueva solicitud
        try {
            Usuario solicitante = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario solicitante no encontrado"));
            
            notificacionService.notificarNuevaSolicitudInscripcion(
                    partido.getOrganizador().getId(),
                    usuarioId,
                    solicitante.getNombre(),
                    partidoId,
                    partido.getNombreUbicacion()
            );
            
            log.info("[InscripcionService] 📧 Notificación enviada al organizador: organizadorId={}", 
                    partido.getOrganizador().getId());
        } catch (Exception e) {
            log.error("[InscripcionService] ⚠️ Error al enviar notificación al organizador", e);
            // No fallar el flujo principal si falla la notificación
        }

        return inscripcionMapper.toDTO(guardada);
    }

    @Transactional(readOnly = true)
    public List<InscripcionDTO> listarPorUsuario(UUID usuarioId, String estado) {
        log.debug("[InscripcionService] Listando inscripciones: usuarioId={}, estado={}", usuarioId, estado);
        
        List<Inscripcion> inscripciones;
        
        if (estado != null && !estado.isBlank()) {
            // Convertir string a enum
            Inscripcion.EstadoInscripcion estadoEnum = Inscripcion.EstadoInscripcion.valueOf(estado.toUpperCase());
            inscripciones = inscripcionRepository.findByUsuarioIdAndEstado(usuarioId, estadoEnum);
        } else {
            inscripciones = inscripcionRepository.findByUsuarioId(usuarioId);
        }

        log.debug("[InscripcionService] Encontradas {} inscripciones", inscripciones.size());

        return inscripciones.stream()
                .map(inscripcionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InscripcionDTO> listarPorPartido(UUID partidoId, String estado) {
        log.debug("[InscripcionService] Listando inscripciones del partido: partidoId={}, estado={}", 
                partidoId, estado);
        
        List<Inscripcion> inscripciones;
        
        if (estado != null && !estado.isBlank()) {
            // Convertir string a enum
            Inscripcion.EstadoInscripcion estadoEnum = Inscripcion.EstadoInscripcion.valueOf(estado.toUpperCase());
            inscripciones = inscripcionRepository.findByPartidoIdAndEstado(partidoId, estadoEnum);
        } else {
            inscripciones = inscripcionRepository.findByPartidoId(partidoId);
        }

        log.debug("[InscripcionService] Encontradas {} inscripciones", inscripciones.size());

        return inscripciones.stream()
                .map(inscripcionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InscripcionDTO> obtenerSolicitudesPendientes(UUID partidoId, Authentication auth) {
        log.info("[InscripcionService] Obteniendo solicitudes pendientes: partidoId={}", partidoId);
        
        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> {
                    log.error("[InscripcionService] Partido no encontrado: {}", partidoId);
                    return new RuntimeException("Partido no encontrado");
                });

        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            log.warn("[InscripcionService] Acceso denegado: userId={} no es organizador del partido={}", 
                    userId, partidoId);
            throw new SecurityException("Solo el organizador puede ver las solicitudes");
        }

        List<Inscripcion> pendientes = inscripcionRepository
                .findSolicitudesPendientesByPartidoId(partidoId);

        log.info("[InscripcionService] ✅ Encontradas {} solicitudes pendientes", pendientes.size());

        return pendientes.stream()
                .map(inscripcionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public InscripcionDTO aceptarInscripcion(UUID inscripcionId, Authentication auth) {
        log.info("[InscripcionService] Aceptando inscripción: id={}", inscripcionId);
        
        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> {
                    log.error("[InscripcionService] Inscripción no encontrada: {}", inscripcionId);
                    return new RuntimeException("Inscripción no encontrada");
                });

        Partido partido = inscripcion.getPartido();

        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            log.warn("[InscripcionService] Acceso denegado: userId={} no es organizador", userId);
            throw new SecurityException("Solo el organizador puede aceptar solicitudes");
        }

        if (inscripcion.getEstado() != Inscripcion.EstadoInscripcion.PENDIENTE) {
            log.warn("[InscripcionService] Estado inválido: {}", inscripcion.getEstado());
            throw new IllegalStateException("La solicitud no está pendiente");
        }

        long jugadoresAceptados = inscripcionRepository
                .countInscripcionesAceptadas(partido.getId());

        log.debug("[InscripcionService] Jugadores actuales: {}/{}", 
                jugadoresAceptados, partido.getCantidadJugadores());

        if (jugadoresAceptados >= partido.getCantidadJugadores()) {
            log.warn("[InscripcionService] Partido completo: {}/{}", 
                    jugadoresAceptados, partido.getCantidadJugadores());
            throw new IllegalStateException("El partido está completo");
        }

        inscripcion.aceptar();
        Inscripcion aceptada = inscripcionRepository.save(inscripcion);
        
        log.info("[InscripcionService] ✅ Inscripción aceptada: id={}, partidoId={}, usuarioId={}", 
                inscripcionId, partido.getId(), inscripcion.getUsuario().getId());

        // Notificar al jugador
        String nombrePartido = partido.getTipoPartido() + " - " + partido.getNombreUbicacion();
        notificacionService.notificarInscripcionAceptada(
                inscripcion.getUsuario().getId(), 
                partido.getId(), 
                nombrePartido
        );

        long nuevosJugadores = jugadoresAceptados + 1;
        if (nuevosJugadores >= partido.getCantidadJugadores()) {
            log.info("[InscripcionService] Partido completo, cambiando estado a CONFIRMADO");
            partido.setEstado("CONFIRMADO");
            partidoRepository.save(partido);
        }

        return inscripcionMapper.toDTO(aceptada);
    }

    @Transactional
    public void rechazarInscripcion(UUID inscripcionId, String motivo, Authentication auth) {
        log.info("[InscripcionService] Rechazando inscripción: id={}, motivo={}", inscripcionId, motivo);
        
        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> {
                    log.error("[InscripcionService] Inscripción no encontrada: {}", inscripcionId);
                    return new RuntimeException("Inscripción no encontrada");
                });

        Partido partido = inscripcion.getPartido();

        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            log.warn("[InscripcionService] Acceso denegado: userId={} no es organizador", userId);
            throw new SecurityException("Solo el organizador puede rechazar solicitudes");
        }

        if (inscripcion.getEstado() != Inscripcion.EstadoInscripcion.PENDIENTE) {
            log.warn("[InscripcionService] Estado inválido: {}", inscripcion.getEstado());
            throw new IllegalStateException("La solicitud no está pendiente");
        }

        inscripcion.rechazar(motivo);
        inscripcionRepository.save(inscripcion);
        
        log.info("[InscripcionService] ✅ Inscripción rechazada: id={}", inscripcionId);
        
        // Notificar al jugador
        String nombrePartido = partido.getTipoPartido() + " - " + partido.getNombreUbicacion();
        notificacionService.notificarInscripcionRechazada(
                inscripcion.getUsuario().getId(), 
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
                    return new RuntimeException("Inscripción no encontrada");
                });

        UUID userId = getUserIdFromAuth(auth);
        if (!inscripcion.getUsuario().getId().equals(userId)) {
            log.warn("[InscripcionService] Acceso denegado: userId={} no es el inscrito", userId);
            throw new SecurityException("Solo puedes cancelar tu propia inscripción");
        }

        Partido partido = inscripcion.getPartido();

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
        
        Optional<Inscripcion> inscripcion = inscripcionRepository
                .findByPartidoIdAndUsuarioId(partidoId, usuarioId);

        if (inscripcion.isPresent()) {
            Inscripcion insc = inscripcion.get();
            resultado.put("inscrito", true);
            resultado.put("estado", insc.getEstado().name());
            resultado.put("inscripcionId", insc.getId());
            
            log.debug("[InscripcionService] Usuario inscrito con estado: {}", insc.getEstado());
        } else {
            resultado.put("inscrito", false);
            resultado.put("estado", null);
            
            log.debug("[InscripcionService] Usuario NO inscrito");
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

        long jugadoresAceptados = inscripcionRepository.countInscripcionesAceptadas(partido.getId());
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