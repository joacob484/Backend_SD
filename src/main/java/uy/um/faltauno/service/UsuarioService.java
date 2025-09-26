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

    public List<PendingReviewResponse> obtenerPendingReviews(UUID userId) {
        // 1) Obtener todas las inscripciones del usuario (o sólo las de partidos finalizados si preferís)
        // Si querés sólo partidos pasados: puedes filtrar por fecha/hora del partido comparando con LocalDate.now()
        List<Inscripcion> misInscripciones = inscripcionRepository.findByUsuarioId(userId);

        // Map para agrupar pendientes por partidoId
        Map<UUID, List<UsuarioMinDTO>> pendientesPorPartido = new HashMap<>();

        for (Inscripcion miInsc : misInscripciones) {
            Partido partido = miInsc.getPartido();
            if (partido == null) continue;

            UUID partidoId = partido.getId();

            // Opcional: sólo considerar partidos con fecha pasada:
            // if (partido.getFecha().isAfter(LocalDate.now())) continue;

            // 2) Obtener todas las inscripciones de ese partido (todos los jugadores)
            List<Inscripcion> inscDelPartido = inscripcionRepository.findByPartidoId(partidoId);

            for (Inscripcion inscOtro : inscDelPartido) {
                // Saltar al propio usuario
                if (inscOtro.getUsuario() == null) continue;
                UUID otroUsuarioId = inscOtro.getUsuario().getId();
                if (userId.equals(otroUsuarioId)) continue;

                // 3) Verificar si ya existe una review del current user hacia ese jugador en este partido
                boolean yaReseñado = reviewRepository.existsByPartido_IdAndUsuarioQueCalifica_IdAndUsuarioCalificado_Id(
                        partidoId,
                        userId,
                        otroUsuarioId
                );

                if (!yaReseñado) {
                    // agregar a pendientesPorPartido
                    Usuario u = inscOtro.getUsuario();
                    UsuarioMinDTO um = new UsuarioMinDTO(u.getId(), u.getNombre(), u.getApellido(), u.getFotoPerfil());

                    pendientesPorPartido.computeIfAbsent(partidoId, k -> new ArrayList<>()).add(um);
                }
            }
        }

        // 4) Mapear pendientesPorPartido a List<PendingReviewResponse>
        List<PendingReviewResponse> result = new ArrayList<>();
        for (Map.Entry<UUID, List<UsuarioMinDTO>> entry : pendientesPorPartido.entrySet()) {
            UUID partidoId = entry.getKey();
            List<UsuarioMinDTO> jugadoresPendientes = entry.getValue();

            // Obtener partido para info (si no lo tenemos, saltamos)
            Partido partido = partidoRepository.findById(partidoId).orElse(null);
            if (partido == null) continue;

            PendingReviewResponse pr = new PendingReviewResponse();
            pr.setPartido_id(partido.getId());
            pr.setTipo_partido(partido.getTipoPartido());
            pr.setFecha(partido.getFecha() != null ? partido.getFecha().toString() : null);
            pr.setNombre_ubicacion(partido.getNombreUbicacion());
            pr.setJugadores_pendientes(jugadoresPendientes);

            result.add(pr);
        }

        return result;
    }

    public void actualizarFoto(UUID usuarioId, String fotoUrl) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setFotoPerfil(fotoUrl);
        usuarioRepository.save(usuario);
    }

    public List<Map<String, Object>> obtenerSolicitudesAmistadPendientes(UUID userId) {
        List<Amistad> pendientes = amistadRepository.findByAmigoIdAndEstado(userId, "PENDIENTE");
        return pendientes.stream().map(a -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("requesterId", a.getUsuarioId());
            m.put("createdAt", a.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
    }


    public List<Map<String, Object>> obtenerMensajesNoLeidos(UUID userId) {
        List<Mensaje> mensajes = mensajeRepository.findByDestinatarioIdAndLeido(userId, false);
        return mensajes.stream().map(m -> {
            Map<String,Object> mm = new HashMap<>();
            mm.put("id", m.getId());
            mm.put("senderId", m.getRemitenteId());
            mm.put("message", m.getContenido());
            mm.put("createdAt", m.getCreatedAt());
            return mm;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> obtenerInvitaciones(UUID userId) {
    List<Inscripcion> pendientes = inscripcionRepository.findByUsuario_IdAndEstado(userId, "PENDIENTE");
    return pendientes.stream().map(i -> {
        Map<String,Object> m = new HashMap<>();
        m.put("id", i.getId());
        m.put("usuarioId", i.getUsuario().getId());
        m.put("matchId", i.getPartido().getId());
        m.put("title", "Invitación a partido");
        m.put("message", "Te han invitado al partido " + i.getPartido().getNombreUbicacion());
        m.put("time", i.getCreatedAt());
        return m;
    }).collect(Collectors.toList());
}

    public List<Map<String, Object>> obtenerActualizacionesPartidos(UUID userId) {
        List<Inscripcion> inscripciones = inscripcionRepository.findByUsuarioId(userId);
        List<Map<String,Object>> updates = new ArrayList<>();
        for (Inscripcion insc : inscripciones) {
            Partido p = insc.getPartido();
            if (p == null) continue;
            Map<String,Object> m = new HashMap<>();
            m.put("partidoId", p.getId());
            m.put("tipo_partido", p.getTipoPartido());
            m.put("nombre_ubicacion", p.getNombreUbicacion());
            m.put("fecha", p.getFecha());
            m.put("hora", p.getHora());
            m.put("inscripcionId", insc.getId());
            m.put("inscripcionEstado", insc.getEstado());
            updates.add(m);
        }
        return updates;
    }


    public void deleteUsuario(UUID id) {
        usuarioRepository.deleteById(id);
    }
}