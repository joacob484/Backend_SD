package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uy.um.faltauno.dto.ReviewDTO;
import uy.um.faltauno.util.ReviewMapper;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Review;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.repository.ReviewRepository;
import uy.um.faltauno.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final UsuarioRepository usuarioRepository;
    private final PartidoRepository partidoRepository;

    public ReviewDTO crearReview(ReviewDTO dto) {
        Usuario usuarioQueCalifica = usuarioRepository.findById(dto.getUsuarioQueCalificaId())
                .orElseThrow(() -> new RuntimeException("Usuario que califica no encontrado"));
        Usuario usuarioCalificado = usuarioRepository.findById(dto.getUsuarioCalificadoId())
                .orElseThrow(() -> new RuntimeException("Usuario calificado no encontrado"));
        Partido partido = partidoRepository.findById(dto.getPartidoId())
                .orElseThrow(() -> new RuntimeException("Partido no encontrado"));

        Review review = reviewMapper.toEntity(dto);
        review.setUsuarioQueCalifica(usuarioQueCalifica);
        review.setUsuarioCalificado(usuarioCalificado);
        review.setPartido(partido);
        review.setCreatedAt(LocalDateTime.now());

        return reviewMapper.toDTO(reviewRepository.save(review));
    }

    public List<ReviewDTO> listarReviewsUsuario(UUID usuarioId) {
        return reviewRepository.findByUsuarioCalificadoId(usuarioId)
                .stream()
                .map(reviewMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ReviewDTO> listarReviewsPartido(UUID partidoId) {
        return reviewRepository.findByPartidoId(partidoId)
                .stream()
                .map(reviewMapper::toDTO)
                .collect(Collectors.toList());
    }
}
