package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uy.um.faltauno.dto.PartidoDTO;
import uy.um.faltauno.util.PartidoMapper;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.repository.UsuarioRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartidoService {

    private final PartidoRepository partidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PartidoMapper partidoMapper;

    public List<PartidoDTO> listarPartidos() {
        return partidoRepository.findAll()
                .stream()
                .map(partidoMapper::toDto)
                .collect(Collectors.toList());
    }

    public PartidoDTO crearPartido(PartidoDTO dto) {
        Partido partido = partidoMapper.toEntity(dto);
        partido.setOrganizador(usuarioRepository.findById(dto.getOrganizadorId())
                .orElseThrow(() -> new RuntimeException("Organizador no encontrado")));
        return partidoMapper.toDto(partidoRepository.save(partido));
    }

    public PartidoDTO obtenerPorId(UUID id) {
        return partidoRepository.findById(id)
                .map(partidoMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Partido no encontrado"));
    }
}