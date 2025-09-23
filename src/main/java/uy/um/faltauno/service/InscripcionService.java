package uy.um.faltauno.service;

import uy.um.faltauno.dto.InscripcionDTO;
import uy.um.faltauno.util.InscripcionMapper;
import uy.um.faltauno.entity.Inscripcion;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.InscripcionRepository;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InscripcionService {

    private final InscripcionRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PartidoRepository partidoRepository;
    private final InscripcionMapper inscripcionMapper;

    public InscripcionDTO crearInscripcion(UUID partidoId, UUID usuarioId) {
        Partido partido = partidoRepository.findById(partidoId).orElseThrow();
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();

        Inscripcion inscripcion = Inscripcion.builder()
                .partido(partido)
                .usuario(usuario)
                .estado("PENDIENTE")
                .build();

        return inscripcionMapper.toDTO(inscripcionRepository.save(inscripcion));
    }

    public List<InscripcionDTO> listarPorUsuario(UUID usuarioId) {
        return inscripcionRepository.findByUsuarioId(usuarioId).stream()
                .map(inscripcionMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<InscripcionDTO> listarPorPartido(UUID partidoId) {
        return inscripcionRepository.findByPartidoId(partidoId).stream()
                .map(inscripcionMapper::toDTO)
                .collect(Collectors.toList());
    }
}
