package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.um.faltauno.config.CustomUserDetailsService;
import uy.um.faltauno.dto.ReviewDTO;
import uy.um.faltauno.dto.UsuarioMinDTO;
import uy.um.faltauno.entity.Inscripcion;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Review;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.InscripcionRepository;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.repository.ReviewRepository;
import uy.um.faltauno.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PartidoRepository partidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InscripcionRepository inscripcionRepository;

    /**
     * Crear una review
     */
    @Transactional
    public ReviewDTO crearReview(ReviewDTO dto, Authentication auth) {
        UUID userId = getUserIdFromAuth(auth);

        // Validaciones
        Partido partido = partidoRepository.findById(dto.getPartidoId())
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));

        Usuario calificador = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Usuario calificado = usuarioRepository.findById(dto.getUsuarioCalificadoId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario calificado no encontrado"));

        // Validar que el partido ya haya pasado
        LocalDateTime fechaPartido = LocalDateTime.of(partido.getFecha(), partido.getHora());
        if (fechaPartido.isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("No puedes calificar un partido que aún no ha sucedido");
        }

        // ✅ PERFORMANCE: Usar query optimizada con JOIN FETCH
        List<Inscripcion> inscripciones = inscripcionRepository.findByPartidoId(dto.getPartidoId());

        boolean calificadorParticipo = inscripciones.stream()
                .anyMatch(i -> i.getUsuario().getId().equals(userId));
        boolean calificadoParticipo = inscripciones.stream()
                .anyMatch(i -> i.getUsuario().getId().equals(dto.getUsuarioCalificadoId()));

        if (!calificadorParticipo) {
            throw new SecurityException("No participaste en este partido");
        }
        if (!calificadoParticipo) {
            throw new IllegalStateException("El usuario calificado no participó en este partido");
        }

        // Validar que no se califique a sí mismo
        if (userId.equals(dto.getUsuarioCalificadoId())) {
            throw new IllegalStateException("No puedes calificarte a ti mismo");
        }

        // Verificar que no exista ya una review
        boolean yaExiste = reviewRepository.existsByPartido_IdAndUsuarioQueCalifica_IdAndUsuarioCalificado_Id(
                dto.getPartidoId(), userId, dto.getUsuarioCalificadoId());

        if (yaExiste) {
            throw new IllegalStateException("Ya calificaste a este usuario en este partido");
        }

        // Crear review (createdAt se setea automáticamente por @Builder.Default)
        Review review = Review.builder()
                .partido(partido)
                .usuarioQueCalifica(calificador)
                .usuarioCalificado(calificado)
                .nivel(dto.getNivel())
                .deportividad(dto.getDeportividad())
                .companerismo(dto.getCompanerismo())
                .comentario(dto.getComentario())
                .build();

        Review guardada = reviewRepository.save(review);
        log.info("Review creada: partidoId={}, calificadorId={}, calificadoId={}", 
                dto.getPartidoId(), userId, dto.getUsuarioCalificadoId());

        return convertirADTO(guardada);
    }

    /**
     * Obtener reviews recibidas por un usuario
     */
    @Transactional(readOnly = true)
    public List<ReviewDTO> obtenerReviewsDeUsuario(UUID usuarioId) {
        List<Review> reviews = reviewRepository.findByUsuarioCalificado_Id(usuarioId);
        return reviews.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener reviews de un partido específico
     */
    @Transactional(readOnly = true)
    public List<ReviewDTO> obtenerReviewsDePartido(UUID partidoId) {
        List<Review> reviews = reviewRepository.findByPartido_Id(partidoId);
        return reviews.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener estadísticas de un usuario
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas(UUID usuarioId) {
        List<Review> reviews = reviewRepository.findByUsuarioCalificado_Id(usuarioId);

        if (reviews.isEmpty()) {
            return Map.of(
                "total_reviews", 0,
                "promedio_nivel", 0.0,
                "promedio_deportividad", 0.0,
                "promedio_companerismo", 0.0,
                "promedio_general", 0.0
            );
        }

        double promedioNivel = reviews.stream()
                .mapToInt(Review::getNivel)
                .average()
                .orElse(0.0);

        double promedioDeportividad = reviews.stream()
                .mapToInt(Review::getDeportividad)
                .average()
                .orElse(0.0);

        double promedioCompanerismo = reviews.stream()
                .mapToInt(Review::getCompanerismo)
                .average()
                .orElse(0.0);

        double promedioGeneral = (promedioNivel + promedioDeportividad + promedioCompanerismo) / 3.0;

        return Map.of(
            "total_reviews", reviews.size(),
            "promedio_nivel", Math.round(promedioNivel * 10.0) / 10.0,
            "promedio_deportividad", Math.round(promedioDeportividad * 10.0) / 10.0,
            "promedio_companerismo", Math.round(promedioCompanerismo * 10.0) / 10.0,
            "promedio_general", Math.round(promedioGeneral * 10.0) / 10.0
        );
    }

    /**
     * Obtener reviews pendientes (jugadores que debe calificar)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerReviewsPendientes(Authentication auth) {
        UUID userId = getUserIdFromAuth(auth);

        // Obtener partidos donde participó (todos en inscripcion están aceptados)
        List<Inscripcion> misInscripciones = inscripcionRepository
                .findByUsuarioId(userId);

        List<Map<String, Object>> pendientes = new ArrayList<>();

        for (Inscripcion miInsc : misInscripciones) {
            Partido partido = miInsc.getPartido();

            // Solo partidos pasados
            LocalDateTime fechaPartido = LocalDateTime.of(partido.getFecha(), partido.getHora());
            if (fechaPartido.isAfter(LocalDateTime.now())) {
                continue;
            }

            // ✅ PERFORMANCE: Usar query optimizada con JOIN FETCH
            List<Inscripcion> otrosJugadores = inscripcionRepository
                    .findByPartidoId(partido.getId())
                    .stream()
                    .filter(i -> !i.getUsuario().getId().equals(userId))
                    .collect(Collectors.toList());

            // Verificar cuáles no ha calificado
            for (Inscripcion otraInsc : otrosJugadores) {
                UUID otroUsuarioId = otraInsc.getUsuario().getId();
                
                boolean yaCalificado = reviewRepository
                        .existsByPartido_IdAndUsuarioQueCalifica_IdAndUsuarioCalificado_Id(
                                partido.getId(), userId, otroUsuarioId);

                if (!yaCalificado) {
                    Usuario otroUsuario = otraInsc.getUsuario();
                    String fotoPerfil = null;
                    if (otroUsuario.getFotoPerfil() != null) {
                        try {
                            fotoPerfil = java.util.Base64.getEncoder().encodeToString(otroUsuario.getFotoPerfil());
                        } catch (Exception ex) {
                            log.warn("[ReviewService] Error encoding foto: {}", ex.getMessage());
                        }
                    }
                    Map<String, Object> pendiente = new HashMap<>();
                    pendiente.put("partido_id", partido.getId());
                    pendiente.put("tipo_partido", partido.getTipoPartido());
                    pendiente.put("fecha", partido.getFecha().toString());
                    pendiente.put("usuario", new UsuarioMinDTO(
                            otroUsuario.getId(),
                            otroUsuario.getNombre(),
                            otroUsuario.getApellido(),
                            fotoPerfil
                    ));
                    pendientes.add(pendiente);
                }
            }
        }

        return pendientes;
    }

    /**
     * Verificar si ya existe una review
     */
    @Transactional(readOnly = true)
    public boolean verificarReviewExistente(UUID partidoId, UUID usuarioCalificadoId, Authentication auth) {
        UUID userId = getUserIdFromAuth(auth);
        return reviewRepository.existsByPartido_IdAndUsuarioQueCalifica_IdAndUsuarioCalificado_Id(
                partidoId, userId, usuarioCalificadoId);
    }

    /**
     * Eliminar una review (solo el autor)
     */
    @Transactional
    public void eliminarReview(UUID reviewId, Authentication auth) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review no encontrada"));

        UUID userId = getUserIdFromAuth(auth);

        if (!review.getUsuarioQueCalifica().getId().equals(userId)) {
            throw new SecurityException("Solo puedes eliminar tus propias reviews");
        }

        reviewRepository.delete(review);
        log.info("Review eliminada: id={}", reviewId);
    }

    // ===== MÉTODOS AUXILIARES =====

    private ReviewDTO convertirADTO(Review review) {
        // Crear UsuarioMinDTO para el usuario calificado
        Usuario calificado = review.getUsuarioCalificado();
        String fotoPerfil = null;
        if (calificado.getFotoPerfil() != null) {
            try {
                fotoPerfil = java.util.Base64.getEncoder().encodeToString(calificado.getFotoPerfil());
            } catch (Exception ex) {
                log.warn("[ReviewService] Error encoding foto: {}", ex.getMessage());
            }
        }
        UsuarioMinDTO usuarioCalificadoDTO = new UsuarioMinDTO(
                calificado.getId(),
                calificado.getNombre(),
                calificado.getApellido(),
                fotoPerfil
        );

        // Usar el builder para crear el DTO
        return ReviewDTO.builder()
                .id(review.getId())
                .partidoId(review.getPartido().getId())
                .usuarioQueCalificaId(review.getUsuarioQueCalifica().getId())
                .usuarioCalificadoId(review.getUsuarioCalificado().getId())
                .nivel(review.getNivel())
                .deportividad(review.getDeportividad())
                .companerismo(review.getCompanerismo())
                .comentario(review.getComentario())
                .createdAt(review.getCreatedAt())
                .usuarioCalificado(usuarioCalificadoDTO)
                .build();
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