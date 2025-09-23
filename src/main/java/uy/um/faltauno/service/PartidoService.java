package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uy.um.faltauno.dto.PartidoDto;
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

    public List<PartidoDto> listarPartidos() {
        return partidoRepository.findAll()
                .stream()
                .map(PartidoMapper.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    public PartidoDto crearPartido(PartidoDto dto) {
        Partido partido = PartidoMapper.INSTANCE.toEntity(dto);
        partido.setOrganizador(usuarioRepository.findById(dto.getOrganizadorId())
                .orElseThrow(() -> new RuntimeException("Organizador no encontrado")));
        return PartidoMapper.INSTANCE.toDto(partidoRepository.save(partido));
    }

    public PartidoDto obtenerPorId(UUID id) {
        return partidoRepository.findById(id)
                .map(PartidoMapper.INSTANCE::toDto)
                .orElseThrow(() -> new RuntimeException("Partido no encontrado"));
    }
}