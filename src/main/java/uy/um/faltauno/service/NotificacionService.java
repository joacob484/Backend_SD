package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.um.faltauno.dto.NotificacionDTO;
import uy.um.faltauno.entity.Notificacion;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.NotificacionRepository;
import uy.um.faltauno.util.NotificacionMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionMapper notificacionMapper;
    private final EmailService emailService;
    private final UsuarioService usuarioService;

    /**
     * Crear una notificación genérica
     */
    @Transactional
    public NotificacionDTO crearNotificacion(
            UUID usuarioId,
            Notificacion.TipoNotificacion tipo,
            String titulo,
            String mensaje,
            UUID entidadId,
            String entidadTipo,
            String urlAccion,
            Notificacion.Prioridad prioridad
    ) {
        log.debug("[NotificacionService] Creando notificación para usuario {}: tipo={}", usuarioId, tipo);

        // 🎯 SISTEMA INTELIGENTE: Evitar duplicados recientes (últimas 24h)
        // Ejemplo: Si ya existe "Nueva solicitud" para el mismo partido en las últimas 24h, 
        // actualizamos el mensaje en lugar de crear duplicado
        if (entidadId != null && debeValidarDuplicados(tipo)) {
            Instant hace24h = Instant.now().minus(24, ChronoUnit.HOURS);
            List<Notificacion> notificacionesRecientes = notificacionRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId)
                .stream()
                .filter(n -> n.getCreatedAt().isAfter(hace24h))
                .filter(n -> n.getTipo().equals(tipo))
                .filter(n -> entidadId.equals(n.getEntidadId()))
                .filter(n -> !n.getLeida()) // Solo actualizar si aún no fue leída
                .limit(1)
                .collect(Collectors.toList());
            
            if (!notificacionesRecientes.isEmpty()) {
                Notificacion existente = notificacionesRecientes.get(0);
                log.info("[NotificacionService] 🔄 Actualizando notificación existente en lugar de duplicar: id={}", existente.getId());
                
                // Actualizar mensaje para reflejar acumulación
                existente.setMensaje(mensaje);
                existente.setCreatedAt(Instant.now()); // Bump to top
                existente.setPrioridad(prioridad != null ? prioridad : Notificacion.Prioridad.NORMAL);
                
                Notificacion actualizada = notificacionRepository.save(existente);
                return notificacionMapper.toDTO(actualizada);
            }
        }

        Notificacion notificacion = Notificacion.builder()
                .usuarioId(usuarioId)
                .tipo(tipo)
                .titulo(titulo)
                .mensaje(mensaje)
                .entidadId(entidadId)
                .entidadTipo(entidadTipo)
                .urlAccion(urlAccion)
                .leida(false)
                .prioridad(prioridad != null ? prioridad : Notificacion.Prioridad.NORMAL)
                .build();

        Notificacion guardada = notificacionRepository.save(notificacion);
        log.info("[NotificacionService] ✅ Notificación creada: id={}, tipo={}", guardada.getId(), tipo);

        // Enviar email de forma asíncrona (best-effort, no bloquea)
        try {
            Usuario usuario = usuarioService.findUsuarioEntityById(usuarioId);
            if (usuario != null && usuario.getEmail() != null) {
                emailService.enviarNotificacionEmail(usuario, tipo, titulo, mensaje, urlAccion);
            }
        } catch (Exception e) {
            log.warn("[NotificacionService] No se pudo enviar email para notificación: {}", e.getMessage());
            // NO propagar el error - el email es secundario
        }

        return notificacionMapper.toDTO(guardada);
    }

    /**
     * Determina si un tipo de notificación debe validar duplicados
     * Solo aplicamos esta lógica a notificaciones que tienden a generarse en masa
     * o que pueden ser enviadas por múltiples instancias de la aplicación
     */
    private boolean debeValidarDuplicados(Notificacion.TipoNotificacion tipo) {
        return tipo == Notificacion.TipoNotificacion.NUEVA_SOLICITUD ||
               tipo == Notificacion.TipoNotificacion.JUGADOR_UNIDO ||
               tipo == Notificacion.TipoNotificacion.NUEVO_MENSAJE ||
               tipo == Notificacion.TipoNotificacion.INSCRIPCION_ACEPTADA || // ✅ FIX: Prevenir emails duplicados
               tipo == Notificacion.TipoNotificacion.INSCRIPCION_RECHAZADA;
    }

    /**
     * Obtener todas las notificaciones de un usuario
     */
    @Transactional(readOnly = true)
    public List<NotificacionDTO> obtenerNotificaciones(Authentication auth) {
        UUID usuarioId = getUserIdFromAuth(auth);
        log.debug("[NotificacionService] Obteniendo notificaciones de usuario {}", usuarioId);

        List<Notificacion> notificaciones = notificacionRepository.findRecientesByUsuarioId(usuarioId);
        
        return notificaciones.stream()
                .map(notificacionMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener notificaciones no leídas
     */
    @Transactional(readOnly = true)
    public List<NotificacionDTO> obtenerNoLeidas(Authentication auth) {
        UUID usuarioId = getUserIdFromAuth(auth);
        log.debug("[NotificacionService] Obteniendo notificaciones no leídas de usuario {}", usuarioId);

        List<Notificacion> notificaciones = notificacionRepository.findNoLeidasByUsuarioId(usuarioId);
        
        return notificaciones.stream()
                .map(notificacionMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Contar notificaciones no leídas
     */
    @Transactional(readOnly = true)
    public long contarNoLeidas(Authentication auth) {
        UUID usuarioId = getUserIdFromAuth(auth);
        return notificacionRepository.countNoLeidasByUsuarioId(usuarioId);
    }

    /**
     * Marcar notificación como leída
     */
    @Transactional
    public void marcarComoLeida(UUID notificacionId, Authentication auth) {
        UUID usuarioId = getUserIdFromAuth(auth);
        
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));

        if (!notificacion.getUsuarioId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso para modificar esta notificación");
        }

        notificacion.marcarComoLeida();
        notificacionRepository.save(notificacion);
        
        log.info("[NotificacionService] Notificación marcada como leída: id={}", notificacionId);
    }

    /**
     * Marcar todas las notificaciones como leídas
     */
    @Transactional
    public int marcarTodasComoLeidas(Authentication auth) {
        UUID usuarioId = getUserIdFromAuth(auth);
        int count = notificacionRepository.marcarTodasComoLeidas(usuarioId, Instant.now());
        
        log.info("[NotificacionService] {} notificaciones marcadas como leídas para usuario {}", count, usuarioId);
        return count;
    }

    /**
     * Marcar notificaciones de inscripción de un partido como leídas
     * Se llama automáticamente cuando el organizador entra a gestión del partido
     */
    @Transactional
    public int marcarNotificacionesInscripcionComoLeidas(UUID partidoId, Authentication auth) {
        UUID usuarioId = getUserIdFromAuth(auth);
        int count = notificacionRepository.marcarNotificacionesInscripcionComoLeidas(
            usuarioId, 
            partidoId, 
            Instant.now()
        );
        
        log.info("[NotificacionService] {} notificaciones de inscripción marcadas como leídas para partido {}", 
                count, partidoId);
        return count;
    }

    /**
     * Eliminar una notificación
     */
    @Transactional
    public void eliminarNotificacion(UUID notificacionId, Authentication auth) {
        UUID usuarioId = getUserIdFromAuth(auth);
        
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));

        if (!notificacion.getUsuarioId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso para eliminar esta notificación");
        }

        notificacionRepository.delete(notificacion);
        log.info("[NotificacionService] Notificación eliminada: id={}", notificacionId);
    }

    /**
     * Limpiar notificaciones antiguas (más de 30 días)
     */
    @Transactional
    public int limpiarNotificacionesAntiguas() {
        Instant hace30Dias = Instant.now().minus(30, ChronoUnit.DAYS);
        int eliminadas = notificacionRepository.eliminarAntiguasAntesDe(hace30Dias);
        
        log.info("[NotificacionService] Limpieza automática: {} notificaciones eliminadas", eliminadas);
        return eliminadas;
    }

    // ==============================================
    // MÉTODOS ESPECÍFICOS POR TIPO DE NOTIFICACIÓN
    // ==============================================

    @Transactional
    public void notificarInvitacionPartido(UUID usuarioId, UUID partidoId, String nombrePartido, String organizador) {
        crearNotificacion(
                usuarioId,
                Notificacion.TipoNotificacion.INVITACION_PARTIDO,
                "Invitación a partido",
                organizador + " te ha invitado a jugar " + nombrePartido,
                partidoId,
                "PARTIDO",
                "/matches/" + partidoId,
                Notificacion.Prioridad.ALTA
        );
    }

    @Transactional
    public void notificarSolicitudAmistad(UUID usuarioId, UUID solicitanteId, String nombreSolicitante) {
        crearNotificacion(
                usuarioId,
                Notificacion.TipoNotificacion.SOLICITUD_AMISTAD,
                "Nueva solicitud de amistad",
                nombreSolicitante + " quiere ser tu amigo",
                solicitanteId,
                "USUARIO",
                "/friend-requests",
                Notificacion.Prioridad.NORMAL
        );
    }

    @Transactional
    public void notificarAmistadAceptada(UUID usuarioId, UUID amigoId, String nombreAmigo) {
        crearNotificacion(
                usuarioId,
                Notificacion.TipoNotificacion.AMISTAD_ACEPTADA,
                "Solicitud aceptada",
                nombreAmigo + " aceptó tu solicitud de amistad",
                amigoId,
                "USUARIO",
                "/friends",
                Notificacion.Prioridad.NORMAL
        );
    }

    @Transactional
    public void notificarNuevaSolicitudInscripcion(UUID organizadorId, UUID solicitanteId, String nombreSolicitante, UUID partidoId, String nombrePartido) {
        crearNotificacion(
                organizadorId,
                Notificacion.TipoNotificacion.NUEVA_SOLICITUD,
                "Nueva solicitud de inscripción",
                nombreSolicitante + " quiere unirse a " + nombrePartido,
                partidoId,
                "PARTIDO",
                "/matches/" + partidoId + "/manage",
                Notificacion.Prioridad.ALTA
        );
    }

    /**
     * 🎯 VERSIÓN MEJORADA: Notificación inteligente que muestra cantidad de solicitudes
     */
    @Transactional
    public void notificarNuevaSolicitudInscripcionMejorada(
            UUID organizadorId, 
            UUID solicitanteId, 
            String mensajePersonalizado,
            UUID partidoId, 
            String nombrePartido,
            long totalSolicitudes) {
        
        String titulo = totalSolicitudes == 1 
            ? "Nueva solicitud de inscripción" 
            : totalSolicitudes + " solicitudes pendientes";
        
        crearNotificacion(
                organizadorId,
                Notificacion.TipoNotificacion.NUEVA_SOLICITUD,
                titulo,
                mensajePersonalizado,
                partidoId,
                "PARTIDO",
                "/matches/" + partidoId + "/manage",
                totalSolicitudes >= 3 ? Notificacion.Prioridad.URGENTE : Notificacion.Prioridad.ALTA
        );
    }

    @Transactional
    public void notificarInscripcionAceptada(UUID usuarioId, UUID partidoId, String nombrePartido) {
        crearNotificacion(
                usuarioId,
                Notificacion.TipoNotificacion.INSCRIPCION_ACEPTADA,
                "Solicitud aceptada",
                "Tu solicitud para " + nombrePartido + " fue aceptada",
                partidoId,
                "PARTIDO",
                "/matches/" + partidoId,
                Notificacion.Prioridad.ALTA
        );
    }

    @Transactional
    public void notificarInscripcionRechazada(UUID usuarioId, UUID partidoId, String nombrePartido, String motivo) {
        crearNotificacion(
                usuarioId,
                Notificacion.TipoNotificacion.INSCRIPCION_RECHAZADA,
                "Solicitud rechazada",
                "Tu solicitud para " + nombrePartido + " fue rechazada" + (motivo != null ? ": " + motivo : ""),
                partidoId,
                "PARTIDO",
                "/matches/" + partidoId,
                Notificacion.Prioridad.NORMAL
        );
    }

    @Transactional
    public void notificarPartidoCancelado(List<UUID> usuariosIds, UUID partidoId, String nombrePartido, String motivo) {
        for (UUID usuarioId : usuariosIds) {
            crearNotificacion(
                    usuarioId,
                    Notificacion.TipoNotificacion.PARTIDO_CANCELADO,
                    "Partido cancelado",
                    "El partido " + nombrePartido + " ha sido cancelado" + (motivo != null ? ": " + motivo : ""),
                    partidoId,
                    "PARTIDO",
                    "/my-matches",
                    Notificacion.Prioridad.ALTA
            );
        }
    }

    @Transactional
    public void notificarPartidoCompletado(List<UUID> usuariosIds, UUID partidoId, String nombrePartido) {
        for (UUID usuarioId : usuariosIds) {
            crearNotificacion(
                    usuarioId,
                    Notificacion.TipoNotificacion.REVIEW_PENDIENTE,
                    "Partido completado",
                    "Califica a los jugadores de " + nombrePartido,
                    partidoId,
                    "PARTIDO",
                    "/matches/" + partidoId + "/review",
                    Notificacion.Prioridad.NORMAL
            );
        }
    }

    @Transactional
    public void notificarPartidoConfirmado(List<UUID> usuariosIds, UUID partidoId, String nombrePartido) {
        for (UUID usuarioId : usuariosIds) {
            crearNotificacion(
                    usuarioId,
                    Notificacion.TipoNotificacion.PARTIDO_CONFIRMADO,
                    "Partido confirmado",
                    "El partido " + nombrePartido + " ha sido confirmado por el organizador",
                    partidoId,
                    "PARTIDO",
                    "/matches/" + partidoId,
                    Notificacion.Prioridad.ALTA
            );
        }
    }

    @Transactional
    public void notificarPartidoActualizado(List<UUID> usuariosIds, UUID partidoId, String nombrePartido) {
        for (UUID usuarioId : usuariosIds) {
            crearNotificacion(
                    usuarioId,
                    Notificacion.TipoNotificacion.PARTIDO_ACTUALIZADO,
                    "Partido actualizado",
                    "El partido " + nombrePartido + " ha sido modificado. Revisa los cambios",
                    partidoId,
                    "PARTIDO",
                    "/matches/" + partidoId,
                    Notificacion.Prioridad.NORMAL
            );
        }
    }

    @Transactional
    public void notificarPartidoListo(UUID organizadorId, UUID partidoId, String nombrePartido) {
        crearNotificacion(
                organizadorId,
                Notificacion.TipoNotificacion.PARTIDO_LISTO,
                "Partido listo para confirmar",
                "El partido " + nombrePartido + " completó todos los cupos. ¡Confírmalo para que se concrete!",
                partidoId,
                "PARTIDO",
                "/my-matches/" + partidoId,
                Notificacion.Prioridad.ALTA
        );
    }

    @Transactional
    public void notificarJugadorEliminado(UUID usuarioId, UUID partidoId, String nombrePartido) {
        crearNotificacion(
                usuarioId,
                Notificacion.TipoNotificacion.JUGADOR_SALIO,
                "Removido del partido",
                "Has sido removido del partido " + nombrePartido,
                partidoId,
                "PARTIDO",
                "/matches",
                Notificacion.Prioridad.ALTA
        );
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void notificarNuevoMensaje(UUID usuarioId, UUID partidoId, String nombrePartido, String remitente) {
        try {
            // Evitar spam: no notificar si ya existe una notificación reciente de mensaje
            boolean existe = notificacionRepository.existeNotificacion(
                    usuarioId, 
                    partidoId, 
                    Notificacion.TipoNotificacion.NUEVO_MENSAJE.name()
            );
            
            if (!existe) {
                crearNotificacion(
                        usuarioId,
                        Notificacion.TipoNotificacion.NUEVO_MENSAJE,
                        "Nuevo mensaje",
                        remitente + " escribió en " + nombrePartido,
                        partidoId,
                        "PARTIDO",
                        "/matches/" + partidoId,
                        Notificacion.Prioridad.BAJA
                );
            }
        } catch (Exception e) {
            log.error("[NotificacionService] Error creando notificación de mensaje: {}", e.getMessage());
            // No propagar la excepción para evitar rollback de la transacción principal
        }
    }

    // ==============================================
    // MÉTODOS AUXILIARES
    // ==============================================

    private UUID getUserIdFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new SecurityException("Usuario no autenticado");
        }
        
        Object principal = auth.getPrincipal();
        
        // ✅ NUEVO: Soporte para Usuario como principal (JWT auth)
        if (principal instanceof Usuario) {
            return ((Usuario) principal).getId();
        }
        
        // Fallback: intentar obtener desde getName() (puede ser email o UUID)
        String principalName = auth.getName();
        
        // Intentar parsear como UUID primero
        try {
            return UUID.fromString(principalName);
        } catch (IllegalArgumentException e) {
            // Si no es un UUID, debe ser un email - buscar usuario por email
            log.debug("[NotificacionService] Principal no es UUID, buscando por email: {}", principalName);
            Usuario usuario = usuarioService.findByEmail(principalName);
            if (usuario == null) {
                throw new IllegalArgumentException("Usuario no encontrado con email: " + principalName);
            }
            return usuario.getId();
        }
    }
}
