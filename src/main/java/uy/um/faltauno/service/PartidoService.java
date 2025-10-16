package uy.um.faltauno.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.um.faltauno.dto.PartidoDTO;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.repository.UsuarioRepository;

import java.util.UUID;

@Service
public class PartidoService {

    private final PartidoRepository partidoRepository;
    private final UsuarioRepository usuarioRepository;

    public PartidoService(PartidoRepository partidoRepository, UsuarioRepository usuarioRepository) {
        this.partidoRepository = partidoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Partido crear(PartidoDTO req) {
        Usuario organizador = usuarioRepository.findById(req.getOrganizadorId())
                .orElseThrow(() -> new IllegalArgumentException("Organizador no existe: " + req.getOrganizadorId()));

        Partido p = new Partido();
        p.setTipoPartido(req.getTipoPartido());
        p.setGenero(req.getGenero());
        p.setFecha(req.getFecha());
        p.setHora(req.getHora());
        p.setDuracionMinutos(req.getDuracionMinutos());
        p.setNombreUbicacion(req.getNombreUbicacion());
        p.setDireccionUbicacion(req.getDireccionUbicacion());
        p.setLatitud(req.getLatitud());
        p.setLongitud(req.getLongitud());
        p.setCantidadJugadores(req.getCantidadJugadores());
        p.setPrecioTotal(req.getPrecioTotal());
        p.setDescripcion(req.getDescripcion());
        p.setOrganizador(organizador);

        return partidoRepository.save(p);
    }

    @Transactional(readOnly = true)
    public Partido obtenerPorId(UUID id) {
        return partidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + id));
    }
}
