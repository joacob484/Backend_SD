package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.ReviewDTO;
import uy.um.faltauno.service.ReviewService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // Crear nueva review
    @PostMapping
    public ResponseEntity<ReviewDTO> crearReview(@RequestBody ReviewDTO reviewDTO) {
        ReviewDTO creado = reviewService.crearReview(reviewDTO);
        return ResponseEntity.ok(creado);
    }

    // Listar reviews de un usuario
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<ReviewDTO>> listarReviewsUsuario(@PathVariable UUID usuarioId) {
        List<ReviewDTO> reviews = reviewService.listarReviewsUsuario(usuarioId);
        return ResponseEntity.ok(reviews);
    }

    // Listar reviews de un partido
    @GetMapping("/partido/{partidoId}")
    public ResponseEntity<List<ReviewDTO>> listarReviewsPartido(@PathVariable UUID partidoId) {
        List<ReviewDTO> reviews = reviewService.listarReviewsPartido(partidoId);
        return ResponseEntity.ok(reviews);
    }
}