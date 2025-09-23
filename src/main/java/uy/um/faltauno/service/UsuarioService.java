package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uy.um.faltauno.dto.UsuarioDto;
import uy.um.faltauno.util.UsuarioMapper;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.UsuarioRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    public UsuarioDto createUsuario(UsuarioDto dto) {
        Usuario usuario = usuarioMapper.toEntity(dto);
        usuario.setCreatedAt(java.time.LocalDateTime.now());
        usuario = usuarioRepository.save(usuario);
        return usuarioMapper.toDTO(usuario);
    }

    public UsuarioDto getUsuario(UUID id) {
        return usuarioRepository.findById(id)
                .map(usuarioMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public List<UsuarioDto> getAllUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(usuarioMapper::toDTO)
                .collect(Collectors.toList());
    }

    public void deleteUsuario(UUID id) {
        usuarioRepository.deleteById(id);
    }
}