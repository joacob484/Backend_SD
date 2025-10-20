package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartidoService {

    private final PartidoRepository partidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PartidoMapper partidoMapper;

    /**
     * Crear un nuevo partido
     */
    @Transactional
    public PartidoDTO crearPartido(PartidoDTO dto) {
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
        partido.setEstado("PENDIENTE");

        Partido guardado = partidoRepository.save(partido);
        log.info("Partido creado: id={}, tipo={}, fecha={}", 
                guardado.getId(), guardado.getTipoPartido(), guardado.getFecha());

        return entityToDtoCompleto(guardado);
    }

    /**
     * Obtener partido completo con jugadores
     */
    @Transactional(readOnly = true)
    public PartidoDTO obtenerPartidoCompleto(UUID id) {
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partido no encontrado"));

        PartidoDTO dto = entityToDtoCompleto(partido);

        // Obtener jugadores aceptados
        List<Inscripcion> inscripciones = inscripcionRepository
                .findByPartido_IdAndEstado(id, "ACEPTADO");
        
        List<UsuarioMinDTO> jugadores = inscripciones.stream()
                .map(i -> {
                    Usuario u = i.getUsuario();
                    return new UsuarioMinDTO(
                        u.getId(),
                        u.getNombre(),
                        u.getApellido(),
                        u.getFotoPerfil()
                    );
                })
                .collect(Collectors.toList());
        
        dto.setJugadores(jugadores);

        return dto;
    }

    /**
     * Listar partidos con filtros
     */
    @Transactional(readOnly = true)
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

        Specification<Partido> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

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
                // Por defecto, solo partidos activos
                predicates.add(cb.equal(root.get("estado"), "PENDIENTE"));
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
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate nombreUbicacion = cb.like(cb.lower(root.get("nombreUbicacion")), pattern);
                Predicate direccion = cb.like(cb.lower(root.get("direccionUbicacion")), pattern);
                predicates.add(cb.or(nombreUbicacion, direccion));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Partido> partidos = partidoRepository.findAll(spec);
        
        return partidos.stream()
                .map(this::entityToDtoCompleto)
                .collect(Collectors.toList());
    }

    /**
     * Listar partidos de un usuario (creados e inscritos)
     */
    @Transactional(readOnly = true)
    public List<PartidoDTO> listarPartidosPorUsuario(UUID usuarioId) {
        // Partidos creados
        List<Partido> creados = partidoRepository.findByOrganizador_Id(usuarioId);

        // Partidos inscritos (ACEPTADO)
        List<Inscripcion> inscripciones = inscripcionRepository
                .findByUsuario_IdAndEstado(usuarioId, "ACEPTADO");
        List<Partido> inscritos = inscripciones.stream()
                .map(Inscripcion::getPartido)
                .collect(Collectors.toList());

        // Combinar y eliminar duplicados
        Set<Partido> todosPartidos = new HashSet<>();
        todosPartidos.addAll(creados);
        todosPartidos.addAll(inscritos);

        return todosPartidos.stream()
                .map(this::entityToDtoCompleto)
                .sorted((a, b) -> {
                    // Ordenar por fecha descendente
                    LocalDateTime dateA = LocalDateTime.of(a.getFecha(), a.getHora());
                    LocalDateTime dateB = LocalDateTime.of(b.getFecha(), b.getHora());
                    return dateB.compareTo(dateA);
                })
                .collect(Collectors.toList());
    }

    /**
     * Actualizar un partido (solo organizador)
     */
   @Transactional
    public PartidoDTO actualizarPartido(UUID id, PartidoDTO dto, Authentication auth) {
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partido no encontrado"));

        // Verificar que sea el organizador
        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            throw new SecurityException("Solo el organizador puede modificar el partido");
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
            // Solo permitir aumentar, no reducir por debajo de jugadores actuales
            long jugadoresActuales = inscripcionRepository
                    .findByPartido_IdAndEstado(id, "ACEPTADO").size();
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

        return entityToDtoCompleto(actualizado);
    }

    /**
     * Cancelar un partido
     */
    @Transactional
    public void cancelarPartido(UUID id, String motivo, Authentication auth) {
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partido no encontrado"));

        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            throw new SecurityException("Solo el organizador puede cancelar el partido");
        }

        // TODO: Notificar a todos los jugadores inscritos
        // TODO: Procesar reembolsos si corresponde

        log.info("Partido cancelado: id={}, motivo={}", id, motivo);
    }

    /**
     * Completar un partido manualmente
     */
    @Transactional
    public void completarPartido(UUID id, Authentication auth) {
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partido no encontrado"));

        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            throw new SecurityException("Solo el organizador puede completar el partido");
        }

        log.info("Partido completado manualmente: id={}", id);
        
        // TODO: Cambiar estado a COMPLETADO
        // TODO: Notificar jugadores para que califiquen
    }

    /**
     * Obtener jugadores de un partido
     */
    @Transactional(readOnly = true)
    public List<UsuarioMinDTO> obtenerJugadores(UUID partidoId) {
        List<Inscripcion> inscripciones = inscripcionRepository
                .findByPartido_IdAndEstado(partidoId, "ACEPTADO");

        return inscripciones.stream()
                .map(i -> {
                    Usuario u = i.getUsuario();
                    return new UsuarioMinDTO(
                        u.getId(),
                        u.getNombre(),
                        u.getApellido(),
                        u.getFotoPerfil()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Remover un jugador del partido
     */
    @Transactional
    public void removerJugador(UUID partidoId, UUID jugadorId, Authentication auth) {
        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new RuntimeException("Partido no encontrado"));

        UUID userId = getUserIdFromAuth(auth);
        if (!partido.getOrganizador().getId().equals(userId)) {
            throw new SecurityException("Solo el organizador puede remover jugadores");
        }

        // No permitir si el partido ya pasó
        if (LocalDateTime.of(partido.getFecha(), partido.getHora()).isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("No se pueden remover jugadores de un partido que ya pasó");
        }

        // Buscar inscripción
        List<Inscripcion> inscripciones = inscripcionRepository.findByPartidoId(partidoId);
        Inscripcion inscripcion = inscripciones.stream()
                .filter(i -> i.getUsuario().getId().equals(jugadorId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("El jugador no está inscrito en este partido"));

        inscripcionRepository.delete(inscripcion);
        log.info("Jugador removido del partido: partidoId={}, jugadorId={}", partidoId, jugadorId);

        // TODO: Notificar al jugador
    }

    /**
     * Eliminar un partido
     */
    @Transactional
    public void eliminarPartido(UUID id, Authentication auth) {
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partido no encontrado"));

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
     * Proceso automático: cancelar partidos que no se llenaron
     * Este método debe ser llamado por un scheduler antes del inicio del partido
     */
    @Transactional
    public void procesarCancelacionesAutomaticas() {
        LocalDateTime ahora = LocalDateTime.now();
        
        // Buscar partidos que empiezan en las próximas 2 horas y no están completos
        List<Partido> partidosPorEmpezar = partidoRepository.findAll().stream()
                .filter(p -> {
                    LocalDateTime inicio = LocalDateTime.of(p.getFecha(), p.getHora());
                    return inicio.isAfter(ahora) && 
                           inicio.isBefore(ahora.plusHours(2));
                })
                .collect(Collectors.toList());

        for (Partido partido : partidosPorEmpezar) {
            long jugadores = inscripcionRepository
                    .findByPartido_IdAndEstado(partido.getId(), "ACEPTADO").size();
            
            // Si no alcanzó el mínimo, cancelar
            int minimo = calcularMinimoJugadores(partido.getCantidadJugadores());
            if (jugadores < minimo) {
                log.warn("Cancelando partido {} por falta de jugadores: {}/{}", 
                        partido.getId(), jugadores, minimo);
                // TODO: Cambiar estado a CANCELADO
                // TODO: Notificar jugadores
            }
        }
    }

    // ===== MÉTODOS AUXILIARES =====

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
        if (dto.getPrecioTotal() == null || dto.getPrecioTotal().doubleValue() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        if (dto.getOrganizadorId() == null) {
            throw new IllegalArgumentException("El organizador es requerido");
        }
    }

    private void validarFechaFutura(LocalDate fecha, LocalTime hora) {
        LocalDateTime fechaHora = LocalDateTime.of(fecha, hora);
        if (fechaHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha y hora deben ser futuras");
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
        if (principal instanceof CustomUserDetailsService.UserPrincipal) {
            return ((CustomUserDetailsService.UserPrincipal) principal).getId();
        }
        
        throw new SecurityException("No se pudo obtener el ID del usuario");
    }

    private PartidoDTO entityToDtoCompleto(Partido partido) {
        PartidoDTO dto = partidoMapper.toDto(partido);
        
        // Calcular jugadores actuales
        long jugadoresActuales = inscripcionRepository
            .findByPartido_IdAndEstado(partido.getId(), "ACEPTADO")
            .size();
        dto.setJugadoresActuales((int) jugadoresActuales);
        
        // Setear información del organizador
        if (partido.getOrganizador() != null) {
            Usuario org = partido.getOrganizador();
            UsuarioMinDTO orgMin = new UsuarioMinDTO(
                org.getId(),
                org.getNombre(),
                org.getApellido(),
                org.getFotoPerfil()
            );
            dto.setOrganizador(orgMin);
        }
        
        // Calcular precio por jugador si no viene
        if (dto.getPrecioPorJugador() == null) {
            dto.setPrecioPorJugador(dto.getPrecioPorJugador()); // Usa el getter que calcula
        }
        
        return dto;
    }

    public Partido findById(UUID id){
        return partidoRepository.findById(id).orElse(null);
    }
}