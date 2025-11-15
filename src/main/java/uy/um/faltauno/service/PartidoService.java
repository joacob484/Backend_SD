package uy.um.faltauno.service;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.um.faltauno.config.CacheNames;
import uy.um.faltauno.config.CustomUserDetailsService;
import uy.um.faltauno.dto.PartidoDTO;
import uy.um.faltauno.dto.UsuarioMinDTO;
import uy.um.faltauno.entity.Inscripcion;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.InscripcionRepository;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.repository.UsuarioRepository;
import uy.um.faltauno.util.PartidoMapper;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartidoService {

    private final PartidoRepository partidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PartidoMapper partidoMapper;
    private final NotificacionService notificacionService;
    private final ReviewService reviewService;
    private final uy.um.faltauno.websocket.WebSocketEventPublisher webSocketEventPublisher;
    // Pub/Sub publisher is optional in environments where Pub/Sub isn't configured.
    // Make it non-final so it's not required by Lombok's generated constructor.
    private Publisher pubSubPublisher;
    
    private final MeterRegistry meterRegistry;
    /**
     * Crear un nuevo partido
     */
    @Transactional
    @CacheEvict(value = CacheNames.PARTIDOS_DISPONIBLES, allEntries = true)
    public PartidoDTO crearPartido(PartidoDTO dto) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        // ✅ Validar que el organizador no tenga reviews pendientes
        if (reviewService.tienePendingReviews(dto.getOrganizadorId())) {
            throw new IllegalStateException("No puedes crear un partido hasta que califiques a todos los jugadores pendientes");
        }
        
        // Validaciones
        validarDatosPartido(dto);

        // Validar que el organizador existe
        Usuario organizador = usuarioRepository.findById(dto.getOrganizadorId())
                .orElseThrow(() -> new IllegalArgumentException("Organizador no encontrado"));

        // Crear partido
        Partido partido = new Partido();
        partido.setTipoPartido(dto.getTipoPartido());
        partido.setGenero(dto.getGenero());
        partido.setNivel(dto.getNivel() != null ? dto.getNivel() : "INTERMEDIO");
        partido.setFecha(dto.getFecha());
        partido.setHora(dto.getHora());
        partido.setDuracionMinutos(dto.getDuracionMinutos() != null ? dto.getDuracionMinutos() : 90);
        partido.setNombreUbicacion(dto.getNombreUbicacion());
        partido.setDireccionUbicacion(dto.getDireccionUbicacion());
        partido.setLatitud(dto.getLatitud());
        partido.setLongitud(dto.getLongitud());
        partido.setCantidadJugadores(dto.getCantidadJugadores());
        partido.setPrecioTotal(dto.getPrecioTotal());
        partido.setDescripcion(dto.getDescripcion());
        partido.setOrganizador(organizador);
        partido.setEstado("DISPONIBLE");

        Partido guardado = partidoRepository.save(partido);
        log.info("Partido creado: id={}, tipo={}, fecha={}", 
                guardado.getId(), guardado.getTipoPartido(), guardado.getFecha());

        // ✅ Crear inscripción automática para el organizador
        Inscripcion inscripcionOrganizador = Inscripcion.builder()
                .partido(guardado)
                .usuario(organizador)
                .build();
        inscripcionRepository.save(inscripcionOrganizador);
        log.info("Inscripción automática creada para organizador: partidoId={}, userId={}", 
                guardado.getId(), organizador.getId());

        // 🔥 Publicar evento asíncrono
        publicarEvento("partidos.created", Map.of(
            "event", "PARTIDO_CREADO",
            "partidoId", guardado.getId().toString(),
            "organizadorId", organizador.getId().toString(),
            "tipoPartido", guardado.getTipoPartido(),
            "fecha", guardado.getFecha().toString(),
            "ubicacion", guardado.getNombreUbicacion()
        ));

        PartidoDTO result = entityToDtoCompleto(guardado);
        meterRegistry.counter("faltauno_partidos_created_total").increment();
        sample.stop(meterRegistry.timer("faltauno_partido_create_duration_seconds"));
        return result;
    }

    /**
     * Obtener partido completo con jugadores
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.PARTIDOS_V2, key = "#id")
    public PartidoDTO obtenerPartidoCompleto(UUID id) {
        log.debug("[PartidoService] Obteniendo partido: {}", id);
        
        Partido partido = partidoRepository.findByIdWithOrganizador(id)
            .orElseThrow(() -> new NoSuchElementException("Partido no encontrado: " + id));
        
        log.debug("[PartidoService] Partido encontrado, organizador: {}", 
            partido.getOrganizador() != null ? partido.getOrganizador().getId() : "null");

        PartidoDTO dto = entityToDtoCompleto(partido);
        log.debug("[PartidoService] DTO creado, buscando jugadores");

        // ✅ PERFORMANCE: Usar query optimizada con JOIN FETCH para evitar N+1
        List<Inscripcion> inscripciones = inscripcionRepository
            .findByPartidoId(id);
        
        log.debug("[PartidoService] Inscripciones encontradas: {}", inscripciones.size());

        List<UsuarioMinDTO> jugadores = inscripciones.stream()
            .map(i -> {
                Usuario u = i.getUsuario();
                return new UsuarioMinDTO(u.getId(), u.getNombre(), u.getApellido(), encodeFotoPerfil(u.getFotoPerfil()));
            })
            .toList();

        dto.setJugadores(jugadores);
        log.debug("[PartidoService] DTO completo, jugadores: {}", jugadores.size());
        return dto;
    }


    /**
     * Listar partidos con filtros
     * ✅ OPTIMIZACIÓN: Cache con clave compuesta por parámetros de búsqueda
     * - Reduce queries repetitivas (usuarios buscando mismos filtros)
     * - TTL de 10 minutos (configurado en application.yaml)
     * - Se invalida automáticamente cuando se crea/modifica un partido
     */
    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = CacheNames.PARTIDOS_DISPONIBLES,
        key = "#tipoPartido + '_' + #nivel + '_' + #genero + '_' + #fecha + '_' + #estado + '_' + #search",
        unless = "#result.isEmpty()"
    )
    public List<PartidoDTO> listarPartidos(
            String tipoPartido,
            String nivel,
            String genero,
            LocalDate fecha,
            String estado,
            String search,
            Double latitud,
            Double longitud,
            Double radioKm,
            Pageable pageable) {

        log.debug("[PartidoService] Listando partidos: tipo={}, nivel={}, genero={}, fecha={}, estado={}, search={}", 
                tipoPartido, nivel, genero, fecha, estado, search);

        Specification<Partido> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // ✅ FIX: JOIN FETCH del organizador para evitar LazyInitializationException
            if (query != null) {
                query.distinct(true);
                root.fetch("organizador", JoinType.LEFT);
            }

            // Filtro por tipo
            if (tipoPartido != null && !tipoPartido.isBlank()) {
                predicates.add(cb.equal(root.get("tipoPartido"), tipoPartido));
            }

            // Filtro por nivel
            if (nivel != null && !nivel.isBlank()) {
                predicates.add(cb.equal(root.get("nivel"), nivel));
            }

            // Filtro por género
            if (genero != null && !genero.isBlank()) {
                predicates.add(cb.equal(root.get("genero"), genero));
            }

            // Filtro por estado
            if (estado != null && !estado.isBlank()) {
                predicates.add(cb.equal(root.get("estado"), estado));
            } else {
                // Por defecto, solo partidos disponibles
                predicates.add(cb.equal(root.get("estado"), "DISPONIBLE"));
            }

            // Filtro por fecha
            if (fecha != null) {
                predicates.add(cb.equal(root.get("fecha"), fecha));
            } else {
                // Por defecto, solo partidos futuros
                predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), LocalDate.now()));
            }

            // Filtro por búsqueda en ubicación
            if (search != null && !search.isBlank()) {
                // ✅ SEGURIDAD: Sanitizar input para prevenir SQL injection
                String sanitized = sanitizeSearchInput(search);
                if (sanitized != null && !sanitized.isEmpty()) {
                    String pattern = "%" + sanitized.toLowerCase() + "%";
                    Predicate nombreUbicacion = cb.like(cb.lower(root.get("nombreUbicacion")), pattern);
                    Predicate direccion = cb.like(cb.lower(root.get("direccionUbicacion")), pattern);
                    predicates.add(cb.or(nombreUbicacion, direccion));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Partido> partidos = partidoRepository.findAll(spec);
        
        log.debug("[PartidoService.listarPartidos] Partidos encontrados: {}", partidos.size());
        
        // ✅ FIX: Forzar inicialización del organizador dentro de la transacción
        for (Partido p : partidos) {
            if (p.getOrganizador() != null) {
                p.getOrganizador().getNombre(); // Touch lazy field para forzar carga
            }
        }
        
        List<PartidoDTO> resultado = partidos.stream()
                .map(this::entityToDtoCompleto)
                .collect(Collectors.toList());
        
        log.debug("[PartidoService.listarPartidos] DTOs generados: {}", resultado.size());
        return resultado;
    }

    /**
     * Listar partidos de un usuario (creados e inscritos)
     */
    @Transactional(readOnly = true)
    public List<PartidoDTO> listarPartidosPorUsuario(UUID usuarioId) {
        log.debug("[PartidoService.listarPartidosPorUsuario] Buscando partidos para usuario: {}", usuarioId);
        
        try {
            // ✅ FIX: Usar método con JOIN FETCH para evitar LazyInitializationException
            List<Partido> creados = partidoRepository.findByOrganizadorIdWithOrganizador(usuarioId);
            log.debug("[PartidoService.listarPartidosPorUsuario] Partidos creados: {}", creados.size());

            // ✅ FIX: Usar query con JOIN FETCH explícito en vez del método derivado
            List<Inscripcion> inscripciones = inscripcionRepository
                    .findByUsuarioId(usuarioId);
            log.debug("[PartidoService.listarPartidosPorUsuario] Inscripciones encontradas: {}", inscripciones.size());
            
            // ✅ FIX: Extraer partidos dentro de la transacción y forzar inicialización del organizador
            List<Partido> inscritos = inscripciones.stream()
                    .map(i -> {
                        Partido p = i.getPartido();
                        // Forzar inicialización del organizador dentro de la transacción
                        if (p.getOrganizador() != null) {
                            p.getOrganizador().getNombre(); // Touch lazy field
                        }
                        return p;
                    })
                    .collect(Collectors.toList());

            log.debug("[PartidoService.listarPartidosPorUsuario] Partidos inscritos: {}", inscritos.size());

            // Combinar y eliminar duplicados
            Set<Partido> todosPartidos = new HashSet<>();
            todosPartidos.addAll(creados);
            todosPartidos.addAll(inscritos);

            log.debug("[PartidoService.listarPartidosPorUsuario] Total partidos únicos: {}", todosPartidos.size());

            List<PartidoDTO> resultado = todosPartidos.stream()
                    .map(this::entityToDtoCompleto)
                    .sorted((a, b) -> {
                        // Ordenar por fecha descendente
                        LocalDateTime dateA = LocalDateTime.of(a.getFecha(), a.getHora());
                        LocalDateTime dateB = LocalDateTime.of(b.getFecha(), b.getHora());
                        return dateB.compareTo(dateA);
                    })
                    .collect(Collectors.toList());

            log.debug("[PartidoService.listarPartidosPorUsuario] DTOs generados: {}", resultado.size());
            return resultado;
            
        } catch (Exception e) {
            log.error("[PartidoService.listarPartidosPorUsuario] Error procesando partidos para usuario {}: {}", 
                    usuarioId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Actualizar un partido (solo organizador)
     */
   @Transactional
   @CacheEvict(value = {CacheNames.PARTIDOS_V2, CacheNames.PARTIDOS_DISPONIBLES}, allEntries = true)
    public PartidoDTO actualizarPartido(UUID id, PartidoDTO dto, Authentication auth) {
         Timer.Sample sample = Timer.start(meterRegistry);
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));

        // Verificar que sea el organizador
        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            throw new SecurityException("Solo el organizador puede modificar el partido");
        }

        // ✅ No permitir edición de partidos CONFIRMADOS, COMPLETADOS o CANCELADOS
        if ("CONFIRMADO".equals(partido.getEstado())) {
            throw new IllegalStateException("No se puede modificar un partido confirmado");
        }
        if ("COMPLETADO".equals(partido.getEstado())) {
            throw new IllegalStateException("No se puede modificar un partido completado");
        }
        if ("CANCELADO".equals(partido.getEstado())) {
            throw new IllegalStateException("No se puede modificar un partido cancelado");
        }

        // No permitir edición si el partido ya pasó
        if (LocalDateTime.of(partido.getFecha(), partido.getHora()).isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("No se puede modificar un partido que ya pasó");
        }

        // Actualizar campos permitidos
        if (dto.getFecha() != null) {
            validarFechaFutura(dto.getFecha(), dto.getHora() != null ? dto.getHora() : partido.getHora());
            partido.setFecha(dto.getFecha());
        }
        if (dto.getHora() != null) {
            partido.setHora(dto.getHora());
        }
        if (dto.getNombreUbicacion() != null) {
            partido.setNombreUbicacion(dto.getNombreUbicacion());
        }
        if (dto.getDireccionUbicacion() != null) {
            partido.setDireccionUbicacion(dto.getDireccionUbicacion());
        }
        if (dto.getLatitud() != null) {
            partido.setLatitud(dto.getLatitud());
        }
        if (dto.getLongitud() != null) {
            partido.setLongitud(dto.getLongitud());
        }
        if (dto.getCantidadJugadores() != null) {
            // ✅ PERFORMANCE: Usar COUNT query en lugar de .size()
            long jugadoresActuales = inscripcionRepository
                    .countByPartidoId(id);
            if (dto.getCantidadJugadores() < jugadoresActuales) {
                throw new IllegalStateException(
                    "No se puede reducir la cantidad de jugadores por debajo de " + jugadoresActuales);
            }
            partido.setCantidadJugadores(dto.getCantidadJugadores());
        }
        if (dto.getPrecioTotal() != null) {
            partido.setPrecioTotal(dto.getPrecioTotal());
        }
        if (dto.getDescripcion() != null) {
            partido.setDescripcion(dto.getDescripcion());
        }
        if (dto.getDuracionMinutos() != null) {
            partido.setDuracionMinutos(dto.getDuracionMinutos());
        }

        Partido actualizado = partidoRepository.save(partido);
        log.info("Partido actualizado: id={}", id);

         PartidoDTO result = entityToDtoCompleto(actualizado);
         
         // 🔥 WebSocket: Notificar actualización en tiempo real
        try {
            webSocketEventPublisher.notifyPartidoUpdated(id.toString(), result);
            log.info("[PartidoService] 📡 WebSocket: Actualización de partido notificada");
        } catch (Exception e) {
            log.error("[PartidoService] ⚠️ Error notificando WebSocket", e);
        }

         meterRegistry.counter("faltauno_partidos_updated_total").increment();
         sample.stop(meterRegistry.timer("faltauno_partido_update_duration_seconds"));
         return result;
    }

    /**
     * Cancelar un partido
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.PARTIDOS_V2, CacheNames.PARTIDOS_DISPONIBLES}, allEntries = true)
    public void cancelarPartido(UUID id, String motivo, Authentication auth) {
        Timer.Sample sample = Timer.start(meterRegistry);
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));

        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            throw new SecurityException("Solo el organizador puede cancelar el partido");
        }

        // ✅ No permitir cancelar partidos CONFIRMADOS, COMPLETADOS o ya CANCELADOS
        if ("CONFIRMADO".equals(partido.getEstado())) {
            throw new IllegalStateException("No se puede cancelar un partido confirmado");
        }
        if ("COMPLETADO".equals(partido.getEstado())) {
            throw new IllegalStateException("No se puede cancelar un partido completado");
        }
        if ("CANCELADO".equals(partido.getEstado())) {
            throw new IllegalStateException("El partido ya está cancelado");
        }

        // Cambiar estado a CANCELADO
        partido.setEstado("CANCELADO");
        partidoRepository.save(partido);

        log.info("Partido cancelado: id={}, motivo={}", id, motivo);
        
        // Notificar a todos los jugadores inscritos
        List<Inscripcion> inscripciones = inscripcionRepository.findByPartidoId(id);
        List<UUID> usuariosIds = inscripciones.stream()
                .map(i -> i.getUsuario().getId())
                .filter(uid -> !uid.equals(userId)) // No notificar al organizador
                .collect(Collectors.toList());
        
        String nombrePartido = partido.getTipoPartido() + " - " + partido.getNombreUbicacion();
        notificacionService.notificarPartidoCancelado(usuariosIds, id, nombrePartido, motivo);
        
        log.info("Notificaciones enviadas a {} jugadores sobre cancelación", usuariosIds.size());

        // 🔥 WebSocket: Notificar cancelación en tiempo real
        try {
            webSocketEventPublisher.notifyPartidoCancelled(id.toString(), motivo);
            log.info("[PartidoService] 📡 WebSocket: Cancelación de partido notificada");
        } catch (Exception e) {
            log.error("[PartidoService] ⚠️ Error notificando WebSocket", e);
        }

        // 🔥 Publicar evento asíncrono
        publicarEvento("partidos.cancelado", Map.of(
            "event", "PARTIDO_CANCELADO",
            "partidoId", id.toString(),
            "organizadorId", userId.toString(),
            "motivo", motivo != null ? motivo : "Sin motivo especificado",
            "jugadoresAfectados", String.valueOf(usuariosIds.size())
        ));
        meterRegistry.counter("faltauno_partidos_cancelled_total").increment();
        sample.stop(meterRegistry.timer("faltauno_partido_cancel_duration_seconds"));
    }

    /**
     * Completar un partido manualmente
     */
    @Transactional
    public void completarPartido(UUID id, Authentication auth) {
        Timer.Sample sample = Timer.start(meterRegistry);
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));

        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            throw new SecurityException("Solo el organizador puede completar el partido");
        }

        // Cambiar estado a COMPLETADO
        partido.setEstado("COMPLETADO");
        partidoRepository.save(partido);

        log.info("Partido completado manualmente: id={}", id);
        
        // Notificar jugadores para que califiquen
        List<Inscripcion> inscripciones = inscripcionRepository.findByPartidoId(id);
        List<UUID> usuariosIds = inscripciones.stream()
                .map(i -> i.getUsuario().getId())
                .collect(Collectors.toList());
        
        String nombrePartido = partido.getTipoPartido() + " - " + partido.getNombreUbicacion();
        notificacionService.notificarPartidoCompletado(usuariosIds, id, nombrePartido);
        
        log.info("Notificaciones de review enviadas a {} jugadores", usuariosIds.size());

        // 🔥 WebSocket: Notificar partido completado en tiempo real
        try {
            webSocketEventPublisher.notifyPartidoCompleted(id.toString());
            log.info("[PartidoService] 📡 WebSocket: Partido completado notificado");
        } catch (Exception e) {
            log.error("[PartidoService] ⚠️ Error notificando WebSocket", e);
        }

        // 🔥 Publicar evento asíncrono
        publicarEvento("partidos.completado", Map.of(
            "event", "PARTIDO_COMPLETADO",
            "partidoId", id.toString(),
            "organizadorId", userId.toString(),
            "jugadoresParticipantes", String.valueOf(usuariosIds.size())
        ));
        meterRegistry.counter("faltauno_partidos_completed_total").increment();
        sample.stop(meterRegistry.timer("faltauno_partido_complete_duration_seconds"));
    }

    /**
     * Confirmar un partido manualmente (solo organizador)
     * Permite al organizador confirmar el partido antes de que se llenen todos los cupos
     */
    @Transactional
    public void confirmarPartido(UUID id, Authentication auth) {
        Timer.Sample sample = Timer.start(meterRegistry);
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));

        UUID userId = getUserIdFromAuth(auth);
        
        // Verificar que es el organizador
        if (!partido.getOrganizador().getId().equals(userId)) {
            throw new SecurityException("Solo el organizador puede confirmar el partido");
        }

        // Verificar que el partido está en estado DISPONIBLE
        if (!"DISPONIBLE".equals(partido.getEstado())) {
            throw new IllegalStateException("Solo se pueden confirmar partidos disponibles");
        }

        // Cambiar estado a CONFIRMADO
        partido.setEstado("CONFIRMADO");
        partidoRepository.save(partido);
        
        log.info("Partido {} confirmado manualmente por organizador {}", id, userId);

        // Notificar a todos los inscritos
        List<Inscripcion> inscripciones = inscripcionRepository
                .findByPartidoId(id);
        
        List<UUID> usuariosIds = inscripciones.stream()
                .map(i -> i.getUsuario().getId())
                .collect(Collectors.toList());
        
        if (!usuariosIds.isEmpty()) {
            String nombrePartido = partido.getTipoPartido() + " - " + partido.getNombreUbicacion();
            notificacionService.notificarPartidoConfirmado(usuariosIds, id, nombrePartido);
            log.info("Notificaciones de confirmación enviadas a {} jugadores", usuariosIds.size());
        }

        // Publicar evento
        publicarEvento("partidos.confirmado", Map.of(
            "event", "PARTIDO_CONFIRMADO",
            "partidoId", id.toString(),
            "organizadorId", userId.toString(),
            "jugadoresInscritos", String.valueOf(usuariosIds.size())
        ));
        meterRegistry.counter("faltauno_partidos_confirmed_total").increment();
        sample.stop(meterRegistry.timer("faltauno_partido_confirm_duration_seconds"));
    }

    /**
     * Obtener jugadores de un partido
     */
    @Transactional(readOnly = true)
    public List<UsuarioMinDTO> obtenerJugadores(UUID partidoId) {
        // ✅ PERFORMANCE: Usar query optimizada con JOIN FETCH
        List<Inscripcion> inscripciones = inscripcionRepository
                .findByPartidoId(partidoId);

        return inscripciones.stream()
                .map(i -> {
                    Usuario u = i.getUsuario();
                    return new UsuarioMinDTO(
                        u.getId(),
                        u.getNombre(),
                        u.getApellido(),
                        encodeFotoPerfil(u.getFotoPerfil())
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Remover un jugador del partido
     */
    @Transactional
    public void removerJugador(UUID partidoId, UUID jugadorId, Authentication auth) {
        Timer.Sample sample = Timer.start(meterRegistry);
        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));

        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            throw new SecurityException("Solo el organizador puede remover jugadores");
        }

        // No permitir si el partido ya pasó
        if (LocalDateTime.of(partido.getFecha(), partido.getHora()).isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("No se pueden remover jugadores de un partido que ya pasó");
        }

        // Buscar inscripción usando el método directo del repository
        Inscripcion inscripcion = inscripcionRepository.findByPartidoIdAndUsuarioId(partidoId, jugadorId)
                .orElseThrow(() -> {
                    log.warn("[PartidoService] No se encontró inscripción: partidoId={}, jugadorId={}", partidoId, jugadorId);
                    return new IllegalArgumentException("El jugador no está inscrito en este partido. Puede que ya haya sido removido.");
                });

        inscripcionRepository.delete(inscripcion);
        log.info("[PartidoService] Jugador removido del partido: partidoId={}, jugadorId={}", partidoId, jugadorId);
        meterRegistry.counter("faltauno_partidos_player_removed_total").increment();
        sample.stop(meterRegistry.timer("faltauno_partido_remove_player_duration_seconds"));

        // Notificar al jugador
        String nombrePartido = partido.getTipoPartido() + " - " + partido.getNombreUbicacion();
        notificacionService.notificarJugadorEliminado(jugadorId, partidoId, nombrePartido);
    }

    /**
     * Eliminar un partido
     */
    @Transactional
    public void eliminarPartido(UUID id, Authentication auth) {
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));

        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            throw new SecurityException("Solo el organizador puede eliminar el partido");
        }

        // Verificar que no tenga jugadores inscritos
        long jugadores = inscripcionRepository.findByPartidoId(id).size();
        if (jugadores > 0) {
            throw new IllegalStateException("No se puede eliminar un partido con jugadores inscritos");
        }

        partidoRepository.delete(partido);
        log.info("Partido eliminado: id={}", id);
    }

    /**
     * Invitar un jugador al partido (crea una inscripción automática pendiente)
     */
    @Transactional
    public void invitarJugador(UUID partidoId, UUID usuarioId, Authentication auth) {
        log.info("Invitando jugador al partido: partidoId={}, usuarioId={}", partidoId, usuarioId);
        
        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));

        UUID organizadorId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(organizadorId)) {
            throw new SecurityException("Solo el organizador puede invitar jugadores");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // ✅ PERFORMANCE: Usar COUNT query optimizada
        long jugadoresActuales = inscripcionRepository
                .countByPartidoId(partidoId);
        
        if (jugadoresActuales >= partido.getCantidadJugadores()) {
            throw new IllegalStateException("El partido está completo");
        }

        // Verificar que no esté ya inscrito o tenga solicitud pendiente
        boolean yaInscrito = inscripcionRepository.existeInscripcion(partidoId, usuarioId);
        if (yaInscrito) {
            throw new IllegalStateException("El usuario ya está inscrito en este partido");
        }
        
        // ✅ FIX: Solo enviar notificación, NO crear inscripción automática
        // El usuario invitado deberá aceptar la invitación manualmente
        
        log.info("Invitación preparada para enviar: partidoId={}, usuarioId={}", partidoId, usuarioId);
        
        // Enviar notificación al usuario invitado
        String nombrePartido = partido.getTipoPartido() + " - " + partido.getNombreUbicacion();
        Usuario organizador = partido.getOrganizador();
        String nombreOrganizador = organizador.getNombre() + " " + organizador.getApellido();
        notificacionService.notificarInvitacionPartido(usuarioId, partidoId, nombrePartido, nombreOrganizador);
    }

    /**
     * Proceso automático: cancelar partidos que no se llenaron
     * Este método debe ser llamado por un scheduler antes del inicio del partido
     */
    @Transactional
    public void procesarCancelacionesAutomaticas() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime dentroDeHoras = ahora.plusHours(2);
        LocalDate hoy = ahora.toLocalDate();
        LocalTime ahoraHora = ahora.toLocalTime();
        LocalTime dentroDeHorasTime = dentroDeHoras.toLocalTime();
        
        // Traemos partidos DISPONIBLES de hoy y filtramos por hora en memoria
        List<Partido> partidosPorEmpezar = partidoRepository
                .findByEstadoAndFecha("DISPONIBLE", hoy)
                .stream()
                .filter(p -> {
                    LocalTime hora = p.getHora();
                    // Debe estar entre ahora y dentro de 2 horas
                    return hora.isAfter(ahoraHora) && hora.isBefore(dentroDeHorasTime);
                })
                .toList();

        for (Partido partido : partidosPorEmpezar) {
            // ✅ PERFORMANCE: Usar COUNT query optimizada
            long jugadores = inscripcionRepository
                    .countByPartidoId(partido.getId());
            
            // Si no alcanzó el mínimo, cancelar
            int minimo = calcularMinimoJugadores(partido.getCantidadJugadores());
            if (jugadores < minimo) {
                log.warn("Cancelando partido {} por falta de jugadores: {}/{}", 
                        partido.getId(), jugadores, minimo);
                
                // Cambiar estado a CANCELADO
                partido.setEstado("CANCELADO");
                partidoRepository.save(partido);
                
                // Notificar jugadores de la cancelación automática
                List<Inscripcion> inscripciones = inscripcionRepository.findByPartidoId(partido.getId());
                List<UUID> usuariosIds = inscripciones.stream()
                        .map(i -> i.getUsuario().getId())
                        .collect(Collectors.toList());
                
                String nombrePartido = partido.getTipoPartido() + " - " + partido.getNombreUbicacion();
                String motivo = "No se alcanzó el mínimo de jugadores requerido";
                notificacionService.notificarPartidoCancelado(usuariosIds, partido.getId(), nombrePartido, motivo);
                
                log.info("Notificaciones de cancelación automática enviadas a {} jugadores", usuariosIds.size());
            }
        }
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Sanitiza el input de búsqueda para prevenir SQL injection.
     * Permite solo caracteres alfanuméricos, espacios, tildes y caracteres especiales comunes.
     * 
     * @param input String a sanitizar
     * @return String sanitizado o null si el input es inválido
     */
    private String sanitizeSearchInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        
        // Limitar longitud máxima a 100 caracteres
        String trimmed = input.trim();
        if (trimmed.length() > 100) {
            log.warn("Input de búsqueda excede 100 caracteres, truncando");
            trimmed = trimmed.substring(0, 100);
        }
        
        // Remover caracteres peligrosos pero permitir tildes y ñ
        // Permite: letras (a-z, A-Z), números (0-9), espacios, tildes (áéíóúÁÉÍÓÚ), ñÑ, guiones, comas
        String sanitized = trimmed.replaceAll("[^a-zA-Z0-9\\sáéíóúÁÉÍÓÚñÑ,\\-]", "");
        
        // Prevenir strings vacías después de sanitización
        if (sanitized.isBlank()) {
            log.warn("Input de búsqueda quedó vacío después de sanitización: {}", input);
            return null;
        }
        
        return sanitized;
    }

    private void publicarEvento(String topicId, Map<String, String> payload) {
        try {
            PubsubMessage message = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(payload.toString()))
                .putAllAttributes(payload)
                .build();

            if (pubSubPublisher != null) {
                pubSubPublisher.publish(message);
                log.info("Evento publicado en Pub/Sub: {} -> {}", topicId, payload.get("event"));
            } else {
                log.info("Pub/Sub publisher not available, skipping publish for topic {}", topicId);
            }
        } catch (Exception e) {
            log.error("Error publicando evento en Pub/Sub {}: {}", topicId, e.getMessage());
        }
    }

    private void validarDatosPartido(PartidoDTO dto) {
        if (dto.getTipoPartido() == null || dto.getTipoPartido().isBlank()) {
            throw new IllegalArgumentException("El tipo de partido es requerido");
        }
        if (dto.getFecha() == null) {
            throw new IllegalArgumentException("La fecha es requerida");
        }
        if (dto.getHora() == null) {
            throw new IllegalArgumentException("La hora es requerida");
        }
        validarFechaFutura(dto.getFecha(), dto.getHora());
        
        if (dto.getNombreUbicacion() == null || dto.getNombreUbicacion().isBlank()) {
            throw new IllegalArgumentException("La ubicación es requerida");
        }
        if (dto.getCantidadJugadores() == null || dto.getCantidadJugadores() < 6 || dto.getCantidadJugadores() > 22) {
            throw new IllegalArgumentException("La cantidad de jugadores debe estar entre 6 y 22");
        }
        // ✅ Validar precio (no negativo y máx 100,000 para prevenir overflow/errores)
        if (dto.getPrecioTotal() == null || dto.getPrecioTotal().doubleValue() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        if (dto.getPrecioTotal().doubleValue() > 100000) {
            throw new IllegalArgumentException("El precio no puede superar $100,000");
        }
        if (dto.getOrganizadorId() == null) {
            throw new IllegalArgumentException("El organizador es requerido");
        }
    }

    private void validarFechaFutura(LocalDate fecha, LocalTime hora) {
        LocalDateTime fechaHora = LocalDateTime.of(fecha, hora);
        // Permitir partidos con al menos 5 minutos de anticipación
        LocalDateTime minimoPermitido = LocalDateTime.now().plusMinutes(5);
        if (fechaHora.isBefore(minimoPermitido)) {
            throw new IllegalArgumentException("La fecha y hora deben ser futuras (al menos 5 minutos de anticipación)");
        }
    }

    private int calcularMinimoJugadores(int capacidad) {
        // Mínimo: 70% de la capacidad
        return (int) Math.ceil(capacidad * 0.7);
    }

    private UUID getUserIdFromAuth(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("Usuario no autenticado");
        }
        
        Object principal = auth.getPrincipal();
        
        // ✅ NUEVO: Soporte para Usuario como principal (JWT auth)
        if (principal instanceof uy.um.faltauno.entity.Usuario) {
            return ((uy.um.faltauno.entity.Usuario) principal).getId();
        }
        
        if (principal instanceof CustomUserDetailsService.UserPrincipal) {
            return ((CustomUserDetailsService.UserPrincipal) principal).getId();
        }
        
        throw new SecurityException("No se pudo obtener el ID del usuario");
    }

    private PartidoDTO entityToDtoCompleto(Partido partido) {
        log.debug("[PartidoService.entityToDtoCompleto] Iniciando mapeo para partido: {}", partido.getId());
        
        PartidoDTO dto = partidoMapper.toDto(partido);
        log.debug("[PartidoService.entityToDtoCompleto] Mapper completado");
        
        // ✅ PERFORMANCE: Usar COUNT query optimizada
        long jugadoresActuales = inscripcionRepository
            .countByPartidoId(partido.getId());
        dto.setJugadoresActuales((int) jugadoresActuales);
        log.debug("[PartidoService.entityToDtoCompleto] Jugadores actuales: {}", jugadoresActuales);
        
        // Setear información del organizador
        if (partido.getOrganizador() != null) {
            Usuario org = partido.getOrganizador();
            log.debug("[PartidoService.entityToDtoCompleto] Procesando organizador: {}", org.getId());
            // ✅ FIX: Ahora el organizador SE CARGA con JOIN FETCH en los repository methods
            // Por lo tanto podemos acceder a nombre/apellido sin LazyInitializationException
            // ✅ NUEVO: Incluir foto_perfil del organizador para mostrar en frontend
            String fotoPerfil = null;
            if (org.getFotoPerfil() != null) {
                try {
                    fotoPerfil = java.util.Base64.getEncoder().encodeToString(org.getFotoPerfil());
                    log.debug("[PartidoService] Foto de organizador incluida en DTO");
                } catch (Exception e) {
                    log.warn("[PartidoService] Error codificando foto del organizador: {}", e.getMessage());
                }
            }
            
            UsuarioMinDTO orgMin = new UsuarioMinDTO(
                org.getId(),
                org.getNombre(),
                org.getApellido(),
                fotoPerfil
            );
            dto.setOrganizador(orgMin);
        } else {
            log.warn("[PartidoService.entityToDtoCompleto] Partido sin organizador: {}", partido.getId());
        }
        
        // Calcular precio por jugador si no viene
        if (dto.getPrecioPorJugador() == null && partido.getPrecioTotal() != null && partido.getCantidadJugadores() != null && partido.getCantidadJugadores() > 0) {
            dto.setPrecioPorJugador(
                partido.getPrecioTotal().divide(BigDecimal.valueOf(partido.getCantidadJugadores()), 2, RoundingMode.HALF_UP)
            );
        }
        
        log.debug("[PartidoService.entityToDtoCompleto] DTO completo");        
        return dto;
    }

    /**
     * Helper method to safely encode foto_perfil to Base64
     */
    private String encodeFotoPerfil(byte[] fotoPerfil) {
        if (fotoPerfil == null) {
            return null;
        }
        try {
            return java.util.Base64.getEncoder().encodeToString(fotoPerfil);
        } catch (Exception e) {
            log.warn("[PartidoService] Error encoding foto_perfil: {}", e.getMessage());
            return null;
        }
    }

    public Partido findById(UUID id){
        return partidoRepository.findById(id).orElse(null);
    }
    
    // ==================== MÉTODOS DE ADMINISTRADOR ====================
    
    /**
     * Listar todos los partidos para admin (sin filtros)
     */
    @Transactional(readOnly = true)
    public List<PartidoDTO> listarTodosParaAdmin() {
        log.info("[ADMIN] Listando todos los partidos");
        
        List<Partido> partidos = partidoRepository.findAll();
        
        return partidos.stream()
                .map(partidoMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Contar todos los partidos
     */
    @Transactional(readOnly = true)
    public long contarPartidos() {
        return partidoRepository.count();
    }
    
    /**
     * Contar partidos de hoy
     */
    @Transactional(readOnly = true)
    public long contarPartidosHoy() {
        LocalDate today = LocalDate.now();
        
        return partidoRepository.findAll().stream()
                .filter(p -> p.getFecha().equals(today))
                .count();
    }
    
    /**
     * Contar partidos de esta semana
     */
    @Transactional(readOnly = true)
    public long contarPartidosEstaSemana() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(java.time.DayOfWeek.SUNDAY);
        
        return partidoRepository.findAll().stream()
                .filter(p -> !p.getFecha().isBefore(startOfWeek) && !p.getFecha().isAfter(endOfWeek))
                .count();
    }
    
    /**
     * Contar partidos de este mes
     */
    @Transactional(readOnly = true)
    public long contarPartidosEsteMes() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        
        return partidoRepository.findAll().stream()
                .filter(p -> !p.getFecha().isBefore(startOfMonth) && !p.getFecha().isAfter(endOfMonth))
                .count();
    }
    
    /**
     * Eliminar partido (solo admin)
     */
    @Transactional
    public void eliminarPartidoAdmin(UUID id) {
        log.warn("[ADMIN] Eliminando partido {}", id);
        
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));
        
        partidoRepository.delete(partido);
    }
}
