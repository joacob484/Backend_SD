package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uy.um.faltauno.dto.PendingReviewResponse;
import uy.um.faltauno.dto.UsuarioDTO;
import uy.um.faltauno.dto.UsuarioMinDTO;
import uy.um.faltauno.util.UsuarioMapper;
import uy.um.faltauno.entity.Amistad;
import uy.um.faltauno.entity.Inscripcion;
import uy.um.faltauno.entity.Mensaje;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Review;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.AmistadRepository;
import uy.um.faltauno.repository.InscripcionRepository;
import uy.um.faltauno.repository.MensajeRepository;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.repository.ReviewRepository;
import uy.um.faltauno.repository.UsuarioRepository;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    @Autowired
    private final UsuarioRepository usuarioRepository;
    private final AmistadRepository amistadRepository;
    private final MensajeRepository mensajeRepository;
    private final UsuarioMapper usuarioMapper;
    private final ReviewRepository reviewRepository;
    private final InscripcionRepository inscripcionRepository;

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

    public List<Map<String, Object>> obtenerSolicitudesAmistadPendientes(UUID userId) {
        List<Amistad> pendientes = amistadRepository.findByAmigoIdAndEstado(userId, "PENDIENTE");
        
        return pendientes.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("requesterId", a.getUsuarioId());
            map.put("createdAt", a.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> obtenerMensajesNoLeidos(UUID userId) {
        List<Mensaje> mensajes = mensajeRepository.findByDestinatarioIdAndLeido(userId, false);
        
        return mensajes.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("senderId", m.getRemitenteId());
            map.put("message", m.getContenido());
            map.put("createdAt", m.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> obtenerInvitaciones(UUID userId) {
        List<Inscripcion> pendientes = inscripcionRepository.findByUsuarioIdAndEstado(userId, "PENDIENTE");
        return pendientes.stream().map(i -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", i.getId());
            m.put("usuarioId", i.getUsuario().getId()); // ✅ acceso correcto
            m.put("matchId", i.getPartido().getId());
            m.put("title", "Invitación a partido");
            m.put("message", "Te han invitado al partido " + i.getPartido().getNombreUbicacion());
            m.put("time", i.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> obtenerActualizacionesPartidos(UUID userId) {
        // Traer inscripciones del usuario (podés filtrar estado si querés)
        // Asegurate de que Inscripcion tenga getPartido() y getUsuario()
        List<Inscripcion> inscripciones = inscripcionRepository.findByUsuarioId(userId);

        // Si preferís solo partidos confirmados:
        // List<Inscripcion> inscripciones = inscripcionRepository.findByUsuarioIdAndEstado(userId, "CONFIRMADO");

        List<Map<String, Object>> updates = new ArrayList<>();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;

        for (Inscripcion insc : inscripciones) {
            Partido p = insc.getPartido();
            if (p == null) continue;

            Map<String, Object> m = new HashMap<>();
            m.put("partidoId", p.getId());
            m.put("tipo_partido", p.getTipoPartido());
            m.put("nombre_ubicacion", p.getNombreUbicacion());
            m.put("direccion_ubicacion", p.getDireccionUbicacion());
            m.put("fecha", p.getFecha() != null ? p.getFecha().toString() : null);
            m.put("hora", p.getHora() != null ? p.getHora().toString() : null);
            m.put("duracion_minutos", p.getDuracionMinutos());
            m.put("cantidad_jugadores", p.getMaxJugadores());
            m.put("organizadorId", p.getOrganizador().getId()); // si tenés relación organizador
            m.put("mensaje", "Revisá los datos del partido"); // campo libre que el front puede usar
            m.put("inscripcionId", insc.getId());
            m.put("inscripcionEstado", insc.getEstado());
            m.put("createdAt", insc.getCreatedAt());

            updates.add(m);
        }

        return updates;
    }

    public void deleteUsuario(UUID id) {
        usuarioRepository.deleteById(id);
    }
}