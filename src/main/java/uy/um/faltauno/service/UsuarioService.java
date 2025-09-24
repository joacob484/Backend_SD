package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uy.um.faltauno.dto.PendingReviewResponse;
import uy.um.faltauno.dto.UsuarioDTO;
import uy.um.faltauno.dto.UsuarioMinDTO;
import uy.um.faltauno.util.UsuarioMapper;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Review;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.repository.ReviewRepository;
import uy.um.faltauno.repository.UsuarioRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    @Autowired
    private final UsuarioRepository usuarioRepository;

    private final UsuarioMapper usuarioMapper;
    private final ReviewRepository reviewRepository;
    private final PartidoRepository partidoRepository;

    public boolean verificarCedula(String cedula) {
        // Lógica de verificación con el registro uruguayo
        // Por ahora devuelve true temporalmente
        return true;
    }


    public UsuarioDTO createUsuario(UsuarioDTO dto) {
        Usuario usuario = usuarioMapper.toEntity(dto);
        usuario.setCreatedAt(java.time.LocalDateTime.now());
        usuario = usuarioRepository.save(usuario);
        return usuarioMapper.toDTO(usuario);
    }

    public UsuarioDTO getUsuario(UUID id) {
        return usuarioRepository.findById(id)
                .map(usuarioMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public List<UsuarioDTO> getAllUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(usuarioMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<PendingReviewResponse> obtenerPendingReviews(UUID id) {

        // 1️⃣ Traer todos los reviews pendientes del usuario
        List<Review> pendientes = reviewRepository.findByUsuarioQueCalificaAndNivelIsNull(id);

        // 2️⃣ Agrupar por partido
        Map<UUID, List<Review>> reviewsPorPartido = pendientes.stream()
                .collect(Collectors.groupingBy(r -> r.getPartido().getId()));

        // 3️⃣ Mapear a PendingReviewResponse
        return reviewsPorPartido.entrySet().stream()
                .map(entry -> {
                    Partido partido = entry.getValue().get(0).getPartido(); // todos son del mismo partido

                    List<UsuarioMinDTO> jugadoresPendientes = entry.getValue().stream()
                            .map(r -> {
                                Usuario u = r.getUsuarioCalificado();
                                return new UsuarioMinDTO(u.getId(), u.getNombre(), u.getApellido(), u.getFotoPerfil());
                            })
                            .collect(Collectors.toList());

                    PendingReviewResponse response = new PendingReviewResponse();
                    response.setPartido_id(partido.getId());
                    response.setTipo_partido(partido.getTipoPartido());
                    response.setFecha(partido.getFecha().toString());
                    response.setNombre_ubicacion(partido.getNombreUbicacion());
                    response.setJugadores_pendientes(jugadoresPendientes);

                    return response;
                })
                .collect(Collectors.toList());
    }

    public void actualizarFoto(UUID usuarioId, String fotoUrl) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setFotoPerfil(fotoUrl);
        usuarioRepository.save(usuario);
    }

    public void deleteUsuario(UUID id) {
        usuarioRepository.deleteById(id);
    }
}