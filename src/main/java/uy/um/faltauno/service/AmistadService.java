package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.um.faltauno.config.CustomUserDetailsService;
import uy.um.faltauno.dto.AmistadDTO;
import uy.um.faltauno.dto.UsuarioMinDTO;
import uy.um.faltauno.entity.Amistad;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.AmistadRepository;
import uy.um.faltauno.repository.UsuarioRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmistadService {

    private final AmistadRepository amistadRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public AmistadDTO enviarSolicitud(UUID amigoId, Authentication auth) {
        log.info("[AmistadService] Enviando solicitud de amistad a usuarioId={}", amigoId);
        
        UUID usuarioId = getUserIdFromAuth(auth);
        
        if (usuarioId.equals(amigoId)) {
            log.warn("[AmistadService] Intento de enviarse solicitud a sí mismo");
            throw new IllegalArgumentException("No puedes enviarte una solicitud de amistad a ti mismo");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        Usuario amigo = usuarioRepository.findById(amigoId)
                .orElseThrow(() -> new RuntimeException("Usuario amigo no encontrado"));

        if (amistadRepository.sonAmigos(usuarioId, amigoId)) {
            log.warn("[AmistadService] Los usuarios ya son amigos");
            throw new IllegalStateException("Ya son amigos");
        }

        if (amistadRepository.existeSolicitudPendiente(usuarioId, amigoId)) {
            log.warn("[AmistadService] Ya existe una solicitud pendiente");
            throw new IllegalStateException("Ya existe una solicitud de amistad pendiente");
        }

        Amistad amistad = new Amistad();
        amistad.setUsuarioId(usuarioId);
        amistad.setAmigoId(amigoId);
        amistad.setEstado("PENDIENTE");
        amistad.setCreatedAt(LocalDateTime.now());

        Amistad guardada = amistadRepository.save(amistad);
        log.info("[AmistadService] ✅ Solicitud de amistad enviada: id={}", guardada.getId());

        return convertToDTO(guardada, usuario, amigo);
    }

    @Transactional
    public AmistadDTO aceptarSolicitud(UUID solicitudId, Authentication auth) {
        log.info("[AmistadService] Aceptando solicitud de amistad: id={}", solicitudId);
        
        UUID usuarioId = getUserIdFromAuth(auth);
        
        Amistad amistad = amistadRepository.findById(solicitudId)
                .orElseThrow(() -> {
                    log.error("[AmistadService] Solicitud no encontrada: {}", solicitudId);
                    return new RuntimeException("Solicitud de amistad no encontrada");
                });

        if (!amistad.getAmigoId().equals(usuarioId)) {
            log.warn("[AmistadService] Intento de aceptar solicitud ajena");
            throw new SecurityException("No puedes aceptar una solicitud que no es tuya");
        }

        if (!"PENDIENTE".equals(amistad.getEstado())) {
            log.warn("[AmistadService] Solicitud no está pendiente: estado={}", amistad.getEstado());
            throw new IllegalStateException("La solicitud no está pendiente");
        }

        amistad.setEstado("ACEPTADO");
        Amistad actualizada = amistadRepository.save(amistad);
        
        log.info("[AmistadService] ✅ Solicitud aceptada: id={}", solicitudId);

        Usuario usuario = usuarioRepository.findById(amistad.getUsuarioId()).orElse(null);
        Usuario amigo = usuarioRepository.findById(amistad.getAmigoId()).orElse(null);

        return convertToDTO(actualizada, usuario, amigo);
    }

    @Transactional
    public void rechazarSolicitud(UUID solicitudId, Authentication auth) {
        log.info("[AmistadService] Rechazando solicitud de amistad: id={}", solicitudId);
        
        UUID usuarioId = getUserIdFromAuth(auth);
        
        Amistad amistad = amistadRepository.findById(solicitudId)
                .orElseThrow(() -> {
                    log.error("[AmistadService] Solicitud no encontrada: {}", solicitudId);
                    return new RuntimeException("Solicitud de amistad no encontrada");
                });

        if (!amistad.getAmigoId().equals(usuarioId)) {
            log.warn("[AmistadService] Intento de rechazar solicitud ajena");
            throw new SecurityException("No puedes rechazar una solicitud que no es tuya");
        }

        if (!"PENDIENTE".equals(amistad.getEstado())) {
            log.warn("[AmistadService] Solicitud no está pendiente: estado={}", amistad.getEstado());
            throw new IllegalStateException("La solicitud no está pendiente");
        }

        amistadRepository.delete(amistad);
        log.info("[AmistadService] ✅ Solicitud rechazada y eliminada: id={}", solicitudId);
    }

    @Transactional
    public void cancelarSolicitud(UUID solicitudId, Authentication auth) {
        log.info("[AmistadService] Cancelando solicitud de amistad: id={}", solicitudId);
        
        UUID usuarioId = getUserIdFromAuth(auth);
        
        Amistad amistad = amistadRepository.findById(solicitudId)
                .orElseThrow(() -> {
                    log.error("[AmistadService] Solicitud no encontrada: {}", solicitudId);
                    return new RuntimeException("Solicitud de amistad no encontrada");
                });

        if (!amistad.getUsuarioId().equals(usuarioId)) {
            log.warn("[AmistadService] Intento de cancelar solicitud ajena");
            throw new SecurityException("No puedes cancelar una solicitud que no enviaste");
        }

        if (!"PENDIENTE".equals(amistad.getEstado())) {
            log.warn("[AmistadService] Solicitud no está pendiente: estado={}", amistad.getEstado());
            throw new IllegalStateException("Solo puedes cancelar solicitudes pendientes");
        }

        amistadRepository.delete(amistad);
        log.info("[AmistadService] ✅ Solicitud cancelada: id={}", solicitudId);
    }

    @Transactional
    public void eliminarAmistad(UUID amigoId, Authentication auth) {
        log.info("[AmistadService] Eliminando amistad con usuarioId={}", amigoId);
        
        UUID usuarioId = getUserIdFromAuth(auth);
        
        Amistad amistad = amistadRepository.findAmistadEntreUsuarios(usuarioId, amigoId)
                .orElseThrow(() -> {
                    log.error("[AmistadService] Amistad no encontrada");
                    return new RuntimeException("No existe una amistad entre estos usuarios");
                });

        if (!"ACEPTADO".equals(amistad.getEstado())) {
            log.warn("[AmistadService] Intento de eliminar amistad no aceptada");
            throw new IllegalStateException("No existe una amistad aceptada");
        }

        amistadRepository.delete(amistad);
        log.info("[AmistadService] ✅ Amistad eliminada: id={}", amistad.getId());
    }

    @Transactional(readOnly = true)
    public List<AmistadDTO> listarAmigos(Authentication auth) {
        log.debug("[AmistadService] Listando amigos");
        
        UUID usuarioId = getUserIdFromAuth(auth);
        
        List<Amistad> amistades = amistadRepository.findAmigosByUsuarioId(usuarioId);
        
        log.debug("[AmistadService] Encontrados {} amigos", amistades.size());

        return amistades.stream()
                .map(amistad -> {
                    UUID otroUsuarioId = amistad.getUsuarioId().equals(usuarioId) 
                            ? amistad.getAmigoId() 
                            : amistad.getUsuarioId();
                    
                    Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
                    Usuario amigo = usuarioRepository.findById(otroUsuarioId).orElse(null);
                    
                    return convertToDTO(amistad, usuario, amigo);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AmistadDTO> listarSolicitudesPendientes(Authentication auth) {
        log.debug("[AmistadService] Listando solicitudes pendientes recibidas");
        
        UUID usuarioId = getUserIdFromAuth(auth);
        
        List<Amistad> solicitudes = amistadRepository.findByAmigoIdAndEstado(usuarioId, "PENDIENTE");
        
        log.debug("[AmistadService] Encontradas {} solicitudes pendientes", solicitudes.size());

        return solicitudes.stream()
                .map(amistad -> {
                    Usuario usuario = usuarioRepository.findById(amistad.getUsuarioId()).orElse(null);
                    Usuario amigo = usuarioRepository.findById(amistad.getAmigoId()).orElse(null);
                    return convertToDTO(amistad, usuario, amigo);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AmistadDTO> listarSolicitudesEnviadas(Authentication auth) {
        log.debug("[AmistadService] Listando solicitudes enviadas");
        
        UUID usuarioId = getUserIdFromAuth(auth);
        
        List<Amistad> solicitudes = amistadRepository.findByUsuarioIdAndEstado(usuarioId, "PENDIENTE");
        
        log.debug("[AmistadService] Encontradas {} solicitudes enviadas", solicitudes.size());

        return solicitudes.stream()
                .map(amistad -> {
                    Usuario usuario = usuarioRepository.findById(amistad.getUsuarioId()).orElse(null);
                    Usuario amigo = usuarioRepository.findById(amistad.getAmigoId()).orElse(null);
                    return convertToDTO(amistad, usuario, amigo);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadoAmistad(UUID amigoId, Authentication auth) {
        log.debug("[AmistadService] Verificando estado de amistad con usuarioId={}", amigoId);
        
        UUID usuarioId = getUserIdFromAuth(auth);
        
        Map<String, Object> resultado = new HashMap<>();
        
        Optional<Amistad> amistadOpt = amistadRepository.findAmistadEntreUsuarios(usuarioId, amigoId);
        
        if (amistadOpt.isPresent()) {
            Amistad amistad = amistadOpt.get();
            resultado.put("existe", true);
            resultado.put("estado", amistad.getEstado());
            resultado.put("amistadId", amistad.getId());
            resultado.put("solicitudEnviada", amistad.getUsuarioId().equals(usuarioId));
            resultado.put("solicitudRecibida", amistad.getAmigoId().equals(usuarioId));
        } else {
            resultado.put("existe", false);
            resultado.put("estado", null);
        }
        
        return resultado;
    }

    @Transactional(readOnly = true)
    public long contarAmigos(UUID usuarioId) {
        return amistadRepository.countAmigosByUsuarioId(usuarioId);
    }

    @Transactional(readOnly = true)
    public long contarSolicitudesPendientes(UUID usuarioId) {
        return amistadRepository.countSolicitudesPendientes(usuarioId);
    }

    private AmistadDTO convertToDTO(Amistad amistad, Usuario usuario, Usuario amigo) {
        AmistadDTO dto = new AmistadDTO();
        dto.setId(amistad.getId());
        dto.setUsuarioId(amistad.getUsuarioId());
        dto.setAmigoId(amistad.getAmigoId());
        dto.setEstado(amistad.getEstado());
        dto.setCreatedAt(amistad.getCreatedAt());
        
        if (usuario != null) {
            dto.setUsuario(new UsuarioMinDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getFotoPerfil()
            ));
        }
        
        if (amigo != null) {
            dto.setAmigo(new UsuarioMinDTO(
                amigo.getId(),
                amigo.getNombre(),
                amigo.getApellido(),
                amigo.getFotoPerfil()
            ));
        }
        
        dto.setTiempoTranscurrido(calcularTiempoTranscurrido(amistad.getCreatedAt()));
        dto.setPuedeAceptar("PENDIENTE".equals(amistad.getEstado()));
        dto.setPuedeRechazar("PENDIENTE".equals(amistad.getEstado()));
        dto.setPuedeCancelar("PENDIENTE".equals(amistad.getEstado()));
        
        return dto;
    }

    private String calcularTiempoTranscurrido(LocalDateTime createdAt) {
        if (createdAt == null) {
            return null;
        }
        
        LocalDateTime ahora = LocalDateTime.now();
        Duration duracion = Duration.between(createdAt, ahora);
        
        long segundos = duracion.getSeconds();
        
        if (segundos < 60) {
            return "Hace " + segundos + " segundo" + (segundos != 1 ? "s" : "");
        }
        
        long minutos = segundos / 60;
        if (minutos < 60) {
            return "Hace " + minutos + " minuto" + (minutos != 1 ? "s" : "");
        }
        
        long horas = minutos / 60;
        if (horas < 24) {
            return "Hace " + horas + " hora" + (horas != 1 ? "s" : "");
        }
        
        long dias = horas / 24;
        if (dias < 30) {
            return "Hace " + dias + " día" + (dias != 1 ? "s" : "");
        }
        
        long meses = dias / 30;
        if (meses < 12) {
            return "Hace " + meses + " mes" + (meses != 1 ? "es" : "");
        }
        
        long años = meses / 12;
        return "Hace " + años + " año" + (años != 1 ? "s" : "");
    }

    private UUID getUserIdFromAuth(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            log.error("[AmistadService] Usuario no autenticado");
            throw new SecurityException("Usuario no autenticado");
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetailsService.UserPrincipal) {
            UUID userId = ((CustomUserDetailsService.UserPrincipal) principal).getId();
            log.debug("[AmistadService] Usuario autenticado: {}", userId);
            return userId;
        }
        
        log.error("[AmistadService] No se pudo obtener ID del usuario del principal");
        throw new SecurityException("No se pudo obtener el ID del usuario");
    }
}