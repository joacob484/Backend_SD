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
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/partidos")
@CrossOrigin(origins = "${FRONTEND_URL:http://localhost:3000}")
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
    public ResponseEntity<ApiResponse<PartidoDTO>> obtenerPorId(@PathVariable UUID id) {
        try {
            PartidoDTO partido = partidoService.obtenerPartidoCompleto(id);
            return ResponseEntity.ok(new ApiResponse<>(partido, "Partido encontrado", true));
        } catch (RuntimeException e) {
            log.error("Partido no encontrado: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, "Partido no encontrado", false));
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
            @PathVariable UUID usuarioId) {
        try {
            List<PartidoDTO> partidos = partidoService.listarPartidosPorUsuario(usuarioId);
            return ResponseEntity.ok(new ApiResponse<>(partidos, "Partidos del usuario", true));
        } catch (Exception e) {
            log.error("Error obteniendo partidos del usuario {}", usuarioId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener partidos", false));
        }
    }
    
    /**
     * Actualizar un partido (solo organizador)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PartidoDTO>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PartidoDTO dto,
            Authentication auth) {
        try {
            PartidoDTO actualizado = partidoService.actualizarPartido(id, dto, auth);
            return ResponseEntity.ok(new ApiResponse<>(actualizado, "Partido actualizado", true));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    /**
     * Cancelar un partido (solo organizador)
     */
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ApiResponse<Void>> cancelar(
            @PathVariable UUID id,
            @RequestBody(required = false) String motivo,
            Authentication auth) {
        try {
            partidoService.cancelarPartido(id, motivo, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Partido cancelado", true));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    /**
     * Completar un partido manualmente (solo organizador)
     */
    @PostMapping("/{id}/completar")
    public ResponseEntity<ApiResponse<Void>> completar(
            @PathVariable UUID id,
            Authentication auth) {
        try {
            partidoService.completarPartido(id, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Partido completado", true));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    /**
     * Obtener jugadores de un partido
     */
    @GetMapping("/{id}/jugadores")
    public ResponseEntity<ApiResponse<List<UsuarioMinDTO>>> obtenerJugadores(
            @PathVariable UUID id) {
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
            @PathVariable UUID partidoId,
            @PathVariable UUID jugadorId,
            Authentication auth) {
        try {
            partidoService.removerJugador(partidoId, jugadorId, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Jugador removido", true));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }

    /**
     * Eliminar un partido (solo organizador, solo si está vacío)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @PathVariable UUID id,
            Authentication auth) {
        try {
            partidoService.eliminarPartido(id, auth);
            return ResponseEntity.ok(new ApiResponse<>(null, "Partido eliminado", true));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
        }
    }
}