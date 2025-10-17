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

    /**
     * Crear solicitud de inscripción
     */
    @Transactional
    public InscripcionDTO crearInscripcion(UUID partidoId, UUID usuarioId, Authentication auth) {
        // Validar que el usuario autenticado coincida con el que se está inscribiendo
        UUID authUserId = getUserIdFromAuth(auth);
        if (!authUserId.equals(usuarioId)) {
            throw new SecurityException("No puedes inscribir a otro usuario");
        }

        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));
        
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Validaciones
        validarInscripcion(partido, usuario);

        // Verificar si ya existe una inscripción
        Optional<Inscripcion> existente = inscripcionRepository
                .findByPartidoId(partidoId)
                .stream()
                .filter(i -> i.getUsuario().getId().equals(usuarioId))
                .findFirst();

        if (existente.isPresent()) {
            Inscripcion insc = existente.get();
            if ("ACEPTADO".equals(insc.getEstado())) {
                throw new IllegalStateException("Ya estás inscrito en este partido");
            } else if ("PENDIENTE".equals(insc.getEstado())) {
                throw new IllegalStateException("Ya tienes una solicitud pendiente para este partido");
            } else if ("RECHAZADO".equals(insc.getEstado())) {
                // Permitir reintentar si fue rechazado
                insc.setEstado("PENDIENTE");
                insc.setCreatedAt(java.time.Instant.now());
                return inscripcionMapper.toDTO(inscripcionRepository.save(insc));
            }
        }

        // Crear nueva inscripción
        Inscripcion inscripcion = Inscripcion.builder()
                .partido(partido)
                .usuario(usuario)
                .estado("PENDIENTE")
                .build();

        Inscripcion guardada = inscripcionRepository.save(inscripcion);
        log.info("Inscripción creada: partidoId={}, usuarioId={}, estado=PENDIENTE", 
                partidoId, usuarioId);

        // TODO: Notificar al organizador

        return inscripcionMapper.toDTO(guardada);
    }

    /**
     * Listar inscripciones de un usuario
     */
    @Transactional(readOnly = true)
    public List<InscripcionDTO> listarPorUsuario(UUID usuarioId, String estado) {
        List<Inscripcion> inscripciones;
        
        if (estado != null && !estado.isBlank()) {
            inscripciones = inscripcionRepository.findByUsuario_IdAndEstado(usuarioId, estado);
        } else {
            inscripciones = inscripcionRepository.findByUsuarioId(usuarioId);
        }

        return inscripciones.stream()
                .map(inscripcionMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Listar inscripciones de un partido
     */
    @Transactional(readOnly = true)
    public List<InscripcionDTO> listarPorPartido(UUID partidoId, String estado) {
        List<Inscripcion> inscripciones;
        
        if (estado != null && !estado.isBlank()) {
            inscripciones = inscripcionRepository.findByPartido_IdAndEstado(partidoId, estado);
        } else {
            inscripciones = inscripcionRepository.findByPartidoId(partidoId);
        }

        return inscripciones.stream()
                .map(inscripcionMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener solicitudes pendientes (solo organizador)
     */
    @Transactional(readOnly = true)
    public List<InscripcionDTO> obtenerSolicitudesPendientes(UUID partidoId, Authentication auth) {
        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new RuntimeException("Partido no encontrado"));

        // Verificar que sea el organizador
        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            throw new SecurityException("Solo el organizador puede ver las solicitudes");
        }

        List<Inscripcion> pendientes = inscripcionRepository
                .findByPartido_IdAndEstado(partidoId, "PENDIENTE");

        return pendientes.stream()
                .map(inscripcionMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Aceptar una solicitud de inscripción
     */
    @Transactional
    public InscripcionDTO aceptarInscripcion(UUID inscripcionId, Authentication auth) {
        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new RuntimeException("Inscripción no encontrada"));

        Partido partido = inscripcion.getPartido();

        // Verificar que sea el organizador
        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            throw new SecurityException("Solo el organizador puede aceptar solicitudes");
        }

        // Verificar que esté pendiente
        if (!"PENDIENTE".equals(inscripcion.getEstado())) {
            throw new IllegalStateException("La solicitud no está pendiente");
        }

        // Verificar que haya cupo
        long jugadoresAceptados = inscripcionRepository
                .findByPartido_IdAndEstado(partido.getId(), "ACEPTADO")
                .size();

        if (jugadoresAceptados >= partido.getCantidadJugadores()) {
            throw new IllegalStateException("El partido está completo");
        }

        // Aceptar
        inscripcion.setEstado("ACEPTADO");
        Inscripcion aceptada = inscripcionRepository.save(inscripcion);
        
        log.info("Inscripción aceptada: id={}, partidoId={}, usuarioId={}", 
                inscripcionId, partido.getId(), inscripcion.getUsuario().getId());

        // TODO: Notificar al usuario aceptado
        // TODO: Si el partido se llenó, cambiar estado a CONFIRMADO

        return inscripcionMapper.toDTO(aceptada);
    }

    /**
     * Rechazar una solicitud de inscripción
     */
    @Transactional
    public void rechazarInscripcion(UUID inscripcionId, String motivo, Authentication auth) {
        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new RuntimeException("Inscripción no encontrada"));

        Partido partido = inscripcion.getPartido();

        // Verificar que sea el organizador
        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            throw new SecurityException("Solo el organizador puede rechazar solicitudes");
        }

        // Verificar que esté pendiente
        if (!"PENDIENTE".equals(inscripcion.getEstado())) {
            throw new IllegalStateException("La solicitud no está pendiente");
        }

        // Rechazar (o eliminar directamente)
        inscripcionRepository.delete(inscripcion);
        
        log.info("Inscripción rechazada: id={}, motivo={}", inscripcionId, motivo);

        // TODO: Notificar al usuario rechazado
    }

    /**
     * Cancelar inscripción (el usuario se retira)
     */
    @Transactional
    public void cancelarInscripcion(UUID inscripcionId, Authentication auth) {
        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new RuntimeException("Inscripción no encontrada"));

        // Verificar que sea el usuario inscrito
        UUID userId = getUserIdFromAuth(auth);
        if (!inscripcion.getUsuario().getId().equals(userId)) {
            throw new SecurityException("Solo puedes cancelar tu propia inscripción");
        }

        Partido partido = inscripcion.getPartido();

        // No permitir cancelar si el partido ya pasó
        LocalDateTime inicioPartido = LocalDateTime.of(partido.getFecha(), partido.getHora());
        if (inicioPartido.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("No puedes cancelar tu inscripción a un partido que ya pasó");
        }

        // No permitir cancelar si falta menos de 2 horas (política de cancelación)
        if (inicioPartido.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new IllegalStateException("No puedes cancelar con menos de 2 horas de anticipación");
        }

        inscripcionRepository.delete(inscripcion);
        
        log.info("Usuario canceló inscripción: inscripcionId={}, usuarioId={}", 
                inscripcionId, userId);

        // TODO: Notificar al organizador
        // TODO: Ofrecer el cupo a usuarios en lista de espera
    }

    /**
     * Obtener estado de inscripción de un usuario en un partido
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadoInscripcion(UUID partidoId, UUID usuarioId) {
        Map<String, Object> resultado = new HashMap<>();
        
        Optional<Inscripcion> inscripcion = inscripcionRepository
                .findByPartidoId(partidoId)
                .stream()
                .filter(i -> i.getUsuario().getId().equals(usuarioId))
                .findFirst();

        if (inscripcion.isPresent()) {
            resultado.put("inscrito", true);
            resultado.put("estado", inscripcion.get().getEstado());
            resultado.put("inscripcionId", inscripcion.get().getId());
        } else {
            resultado.put("inscrito", false);
            resultado.put("estado", null);
        }

        return resultado;
    }

    // ===== VALIDACIONES =====

    private void validarInscripcion(Partido partido, Usuario usuario) {
        // Validar que el partido sea futuro
        LocalDateTime inicioPartido = LocalDateTime.of(partido.getFecha(), partido.getHora());
        if (inicioPartido.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("No puedes inscribirte a un partido que ya pasó");
        }

        // Validar que no sea el organizador
        if (partido.getOrganizador().getId().equals(usuario.getId())) {
            throw new IllegalStateException("El organizador no puede inscribirse como jugador");
        }

        // Validar que haya cupo
        long jugadoresAceptados = inscripcionRepository
                .findByPartido_IdAndEstado(partido.getId(), "ACEPTADO")
                .size();

        if (jugadoresAceptados >= partido.getCantidadJugadores()) {
            throw new IllegalStateException("El partido está completo");
        }

        // TODO: Validar perfil completo del usuario
        // TODO: Validar que no tenga conflictos de horario
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