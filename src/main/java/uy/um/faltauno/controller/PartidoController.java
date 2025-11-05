package uy.um.faltauno.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.dto.InscripcionDTO;
import uy.um.faltauno.dto.PartidoDTO;
import uy.um.faltauno.dto.UsuarioMinDTO;
import uy.um.faltauno.service.PartidoService;
import uy.um.faltauno.service.InscripcionService;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/partidos")
@CrossOrigin(origins = "${FRONTEND_URL:https://faltauno.vercel.app}")
public class PartidoController {

    private final PartidoService partidoService;
    private final InscripcionService inscripcionService;

    public PartidoController(PartidoService partidoService, InscripcionService inscripcionService) {
        this.partidoService = partidoService;
        this.inscripcionService = inscripcionService;
    }

    /**
     * Crear un nuevo partido
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PartidoDTO>> crear(
            @Valid @RequestBody PartidoDTO dto,
            Authentication auth) {
        try {
            log.info("Creando partido: tipo={}, fecha={}, organizadorId={}", 
                    dto.getTipoPartido(), dto.getFecha(), dto.getOrganizadorId());
            
            PartidoDTO creado = partidoService.crearPartido(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(creado, "Partido creado exitosamente", true));
        } catch (IllegalArgumentException e) {
            log.error("Error de validación creando partido: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("Error creando partido", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al crear el partido", false));
        }
    }

    /**
     * Obtener un partido por ID con toda su información
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PartidoDTO>> obtenerPorId(@PathVariable("id") UUID id) {
        try {
            PartidoDTO partido = partidoService.obtenerPartidoCompleto(id);
            return ResponseEntity.ok(new ApiResponse<>(partido, "Partido encontrado", true));
        } catch (NoSuchElementException e) { // o EntityNotFoundException si prefieres
            log.warn("Partido no encontrado: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, "Partido no encontrado", false));
        } catch (Exception e) {
            log.error("Error obteniendo partido {}", id, e); // <-- log con stacktrace
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener el partido", false));
        }
    }

    /**
     * Listar partidos con filtros y paginación
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PartidoDTO>>> listar(
            @RequestParam(required = false) String tipoPartido,
            @RequestParam(required = false) String nivel,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Double latitud,
            @RequestParam(required = false) Double longitud,
            @RequestParam(required = false) Double radioKm,
            Pageable pageable) {
        
        try {
            log.info("Listando partidos con filtros: tipo={}, nivel={}, fecha={}, search={}", 
                    tipoPartido, nivel, fecha, search);
            
            List<PartidoDTO> partidos = partidoService.listarPartidos(
                    tipoPartido, nivel, genero, fecha, estado, search, 
                    latitud, longitud, radioKm, pageable);
            
            return ResponseEntity.ok(new ApiResponse<>(partidos, "Partidos encontrados", true));
        } catch (Exception e) {
            log.error("Error listando partidos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al listar partidos", false));
        }
    }

    /**
     * Obtener partidos de un usuario (creados e inscritos)
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<ApiResponse<List<PartidoDTO>>> listarPorUsuario(
            @PathVariable("usuarioId") UUID usuarioId) {
        try {
            List<PartidoDTO> partidos = partidoService.listarPartidosPorUsuario(usuarioId);
            return ResponseEntity.ok(new ApiResponse<>(partidos, "Partidos del usuario", true));
        } catch (Exception e) {
            log.error("Error obteniendo partidos del usuario {}", usuarioId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener partidos", false));
        }
    }

    @GetMapping("/{partidoId}/solicitudes")
    public ResponseEntity<ApiResponse<List<InscripcionDTO>>> obtenerSolicitudes(
            @PathVariable("partidoId") UUID partidoId,
            Authentication auth) {
        try {
            log.info("Obteniendo solicitudes pendientes para partido: {}", partidoId);
            List<InscripcionDTO> solicitudes = inscripcionService.obtenerSolicitudesPendientes(partidoId, auth);
            return ResponseEntity.ok(new ApiResponse<>(solicitudes, "Solicitudes pendientes", true));
        } catch (SecurityException e) {
            log.warn("Acceso denegado a solicitudes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("Error obteniendo solicitudes del partido {}", partidoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener solicitudes", false));
        }
    }

    /**
     * Actualizar un partido (solo organizador)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PartidoDTO>> actualizar(
            @PathVariable("id") UUID id,
            @Valid @RequestBody PartidoDTO dto,
            Authentication auth) {
        try {
            PartidoDTO actualizado = partidoService.actualizarPartido(id, dto, auth);
            return ResponseEntity.ok(new ApiResponse<>(actualizado, "Partido actualizado", true));
        } catch (IllegalArgumentException e) {
            log.warn("[PartidoController] Recurso no encontrado al actualizar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (IllegalStateException e) {
            log.warn("[PartidoController] Estado inválido al actualizar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("[PartidoController] Error inesperado actualizando partido", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al actualizar partido", false));
        }
    }

    /**
     * Cancelar un partido (solo organizador)
     */
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ApiResponse<Void>> cancelar(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) String motivo,
            Authentication auth) {
        try {
            partidoService.cancelarPartido(id, motivo, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Partido cancelado", true));
        } catch (IllegalArgumentException e) {
            log.warn("[PartidoController] Recurso no encontrado al cancelar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (IllegalStateException e) {
            log.warn("[PartidoController] Estado inválido al cancelar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("[PartidoController] Error inesperado cancelando partido", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al cancelar partido", false));
        }
    }

    /**
     * Completar un partido manualmente (solo organizador)
     */
    @PostMapping("/{id}/completar")
    public ResponseEntity<ApiResponse<Void>> completar(
            @PathVariable("id") UUID id,
            Authentication auth) {
        try {
            partidoService.completarPartido(id, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Partido completado", true));
        } catch (IllegalArgumentException e) {
            log.warn("[PartidoController] Recurso no encontrado al completar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("[PartidoController] Error inesperado completando partido", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al completar partido", false));
        }
    }

    /**
     * Confirmar un partido manualmente (solo organizador)
     * Permite confirmar antes de que se llenen todos los cupos
     */
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<ApiResponse<Void>> confirmar(
            @PathVariable("id") UUID id,
            Authentication auth) {
        try {
            partidoService.confirmarPartido(id, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Partido confirmado", true));
        } catch (IllegalArgumentException e) {
            log.warn("[PartidoController] Recurso no encontrado al confirmar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (IllegalStateException e) {
            log.warn("[PartidoController] Estado inválido al confirmar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("[PartidoController] Error inesperado confirmando partido", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al confirmar partido", false));
        }
    }

    /**
     * Obtener jugadores de un partido
     */
    @GetMapping("/{id}/jugadores")
    public ResponseEntity<ApiResponse<List<UsuarioMinDTO>>> obtenerJugadores(
            @PathVariable("id") UUID id) {
        try {
            List<UsuarioMinDTO> jugadores = partidoService.obtenerJugadores(id);
            return ResponseEntity.ok(new ApiResponse<>(jugadores, "Jugadores del partido", true));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    /**
     * Remover un jugador del partido (solo organizador)
     */
    @DeleteMapping("/{partidoId}/jugadores/{jugadorId}")
    public ResponseEntity<ApiResponse<Void>> removerJugador(
            @PathVariable("partidoId") UUID partidoId,
            @PathVariable("jugadorId") UUID jugadorId,
            Authentication auth) {
        try {
            partidoService.removerJugador(partidoId, jugadorId, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Jugador removido", true));
        } catch (IllegalArgumentException e) {
            log.warn("[PartidoController] Recurso no encontrado al remover jugador: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("[PartidoController] Error inesperado removiendo jugador", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al remover jugador", false));
        }
    }

    /**
     * Eliminar un partido (solo organizador, solo si está vacío)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @PathVariable("id") UUID id,
            Authentication auth) {
        try {
            partidoService.eliminarPartido(id, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Partido eliminado", true));
        } catch (IllegalArgumentException e) {
            log.warn("Partido no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("Error inesperado eliminando partido", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error interno del servidor", false));
        }
    }

    /**
     * Invitar un jugador al partido (solo organizador)
     */
    @PostMapping("/{partidoId}/invitar")
    public ResponseEntity<ApiResponse<Void>> invitarJugador(
            @PathVariable("partidoId") UUID partidoId,
            @RequestBody java.util.Map<String, String> body,
            Authentication auth) {
        try {
            String usuarioId = body.get("usuarioId");
            if (usuarioId == null || usuarioId.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(null, "usuarioId es requerido", false));
            }

            // ✅ Validar UUID antes de parsear
            UUID usuarioUuid;
            try {
                usuarioUuid = UUID.fromString(usuarioId);
            } catch (IllegalArgumentException e) {
                log.warn("UUID inválido recibido: {}", usuarioId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(null, "UUID de usuario inválido", false));
            }

            partidoService.invitarJugador(partidoId, usuarioUuid, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Invitación enviada exitosamente", true));
        } catch (IllegalArgumentException e) {
            log.warn("Recurso no encontrado al invitar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (SecurityException e) {
            log.warn("Acceso denegado al invitar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (IllegalStateException e) {
            // ✅ MEJORADO: Usar 409 Conflict para solicitudes duplicadas
            log.warn("Conflicto al invitar jugador: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (Exception e) {
            log.error("Error inesperado invitando jugador", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error interno del servidor", false));
        }
    }
}