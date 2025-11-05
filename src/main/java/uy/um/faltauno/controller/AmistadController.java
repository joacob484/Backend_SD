package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.dto.AmistadDTO;
import uy.um.faltauno.service.AmistadService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST para gestionar amistades entre usuarios.
 * 
 * Endpoints:
 * - POST /api/amistades/{amigoId}         : Enviar solicitud de amistad
 * - GET /api/amistades                    : Listar amigos aceptados
 * - GET /api/amistades/pendientes         : Listar solicitudes recibidas
 * - GET /api/amistades/enviadas           : Listar solicitudes enviadas
 * - POST /api/amistades/{solicitudId}/aceptar  : Aceptar solicitud
 * - POST /api/amistades/{solicitudId}/rechazar : Rechazar solicitud
 * - DELETE /api/amistades/{solicitudId}/cancelar : Cancelar solicitud enviada
 * - DELETE /api/amistades/{amigoId}       : Eliminar amistad existente
 * - GET /api/amistades/estado/{amigoId}   : Verificar estado de amistad
 * - GET /api/amistades/estadisticas       : Obtener contadores
 */
@RestController
@RequestMapping("/api/amistades")
@CrossOrigin(origins = "${FRONTEND_URL:https://faltauno.vercel.app}")
@RequiredArgsConstructor
@Slf4j
public class AmistadController {

    private final AmistadService amistadService;

    /**
     * Enviar solicitud de amistad a otro usuario
     * 
     * @param amigoId ID del usuario al que se enviará la solicitud
     * @param auth Usuario autenticado
     * @return Solicitud de amistad creada
     */
    @PostMapping("/{amigoId}")
    public ResponseEntity<ApiResponse<AmistadDTO>> enviarSolicitud(
            @PathVariable UUID amigoId,
            Authentication auth) {
        try {
            log.info("[AmistadController] POST /api/amistades/{} - Enviando solicitud", amigoId);
            
            AmistadDTO amistad = amistadService.enviarSolicitud(amigoId, auth);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(amistad, "Solicitud de amistad enviada", true));
                    
        } catch (IllegalArgumentException e) {
            // Distinguir 404 (usuario no encontrado) vs 400 (auto-solicitud)
            if (e.getMessage().contains("no encontrado") || e.getMessage().contains("no encontrada")) {
                log.warn("[AmistadController] Recurso no encontrado: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(null, e.getMessage(), false));
            } else {
                log.warn("[AmistadController] Validación fallida: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(null, e.getMessage(), false));
            }
                    
        } catch (IllegalStateException e) {
            log.warn("[AmistadController] IllegalStateException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (SecurityException e) {
            log.warn("[AmistadController] SecurityException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[AmistadController] Error inesperado enviando solicitud", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al enviar solicitud", false));
        }
    }

    /**
     * Listar amigos aceptados del usuario actual
     * 
     * @param auth Usuario autenticado
     * @return Lista de amigos
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AmistadDTO>>> listarAmigos(
            Authentication auth) {
        try {
            log.debug("[AmistadController] GET /api/amistades - Listando amigos");
            
            List<AmistadDTO> amigos = amistadService.listarAmigos(auth);
            
            return ResponseEntity.ok(
                    new ApiResponse<>(amigos, "Lista de amigos", true));
                    
        } catch (SecurityException e) {
            log.warn("[AmistadController] SecurityException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[AmistadController] Error listando amigos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al listar amigos", false));
        }
    }

    /**
     * Listar solicitudes de amistad pendientes (recibidas)
     * 
     * @param auth Usuario autenticado
     * @return Lista de solicitudes pendientes
     */
    @GetMapping("/pendientes")
    public ResponseEntity<ApiResponse<List<AmistadDTO>>> listarSolicitudesPendientes(
            Authentication auth) {
        try {
            log.debug("[AmistadController] GET /api/amistades/pendientes");
            
            List<AmistadDTO> solicitudes = amistadService.listarSolicitudesPendientes(auth);
            
            return ResponseEntity.ok(
                    new ApiResponse<>(solicitudes, "Solicitudes pendientes", true));
                    
        } catch (SecurityException e) {
            log.warn("[AmistadController] SecurityException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[AmistadController] Error listando solicitudes pendientes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al listar solicitudes", false));
        }
    }

    /**
     * Listar solicitudes de amistad enviadas por el usuario
     * 
     * @param auth Usuario autenticado
     * @return Lista de solicitudes enviadas
     */
    @GetMapping("/enviadas")
    public ResponseEntity<ApiResponse<List<AmistadDTO>>> listarSolicitudesEnviadas(
            Authentication auth) {
        try {
            log.debug("[AmistadController] GET /api/amistades/enviadas");
            
            List<AmistadDTO> solicitudes = amistadService.listarSolicitudesEnviadas(auth);
            
            return ResponseEntity.ok(
                    new ApiResponse<>(solicitudes, "Solicitudes enviadas", true));
                    
        } catch (SecurityException e) {
            log.warn("[AmistadController] SecurityException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[AmistadController] Error listando solicitudes enviadas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al listar solicitudes", false));
        }
    }

    /**
     * Aceptar una solicitud de amistad recibida
     * 
     * @param solicitudId ID de la solicitud
     * @param auth Usuario autenticado
     * @return Amistad aceptada
     */
    @PostMapping("/{solicitudId}/aceptar")
    public ResponseEntity<ApiResponse<AmistadDTO>> aceptarSolicitud(
            @PathVariable UUID solicitudId,
            Authentication auth) {
        try {
            log.info("[AmistadController] POST /api/amistades/{}/aceptar", solicitudId);
            
            AmistadDTO amistad = amistadService.aceptarSolicitud(solicitudId, auth);
            
            return ResponseEntity.ok(
                    new ApiResponse<>(amistad, "Solicitud aceptada", true));
                    
        } catch (IllegalArgumentException e) {
            log.warn("[AmistadController] Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (SecurityException e) {
            log.warn("[AmistadController] Acceso denegado al aceptar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (IllegalStateException e) {
            log.warn("[AmistadController] Error de estado al aceptar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[AmistadController] Error inesperado aceptando solicitud", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al aceptar solicitud", false));
        }
    }

    /**
     * Rechazar una solicitud de amistad recibida
     * 
     * @param solicitudId ID de la solicitud
     * @param auth Usuario autenticado
     * @return Confirmación de rechazo
     */
    @PostMapping("/{solicitudId}/rechazar")
    public ResponseEntity<ApiResponse<Void>> rechazarSolicitud(
            @PathVariable UUID solicitudId,
            Authentication auth) {
        try {
            log.info("[AmistadController] POST /api/amistades/{}/rechazar", solicitudId);
            
            amistadService.rechazarSolicitud(solicitudId, auth);
            
            return ResponseEntity.ok(
                    new ApiResponse<>(null, "Solicitud rechazada", true));
                    
        } catch (IllegalArgumentException e) {
            log.warn("[AmistadController] Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (SecurityException e) {
            log.warn("[AmistadController] Acceso denegado al rechazar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (IllegalStateException e) {
            log.warn("[AmistadController] Error de estado al rechazar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[AmistadController] Error inesperado rechazando solicitud", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al rechazar solicitud", false));
        }
    }

    /**
     * Cancelar una solicitud de amistad enviada
     * 
     * @param solicitudId ID de la solicitud
     * @param auth Usuario autenticado
     * @return Confirmación de cancelación
     */
    @DeleteMapping("/{solicitudId}/cancelar")
    public ResponseEntity<ApiResponse<Void>> cancelarSolicitud(
            @PathVariable UUID solicitudId,
            Authentication auth) {
        try {
            log.info("[AmistadController] DELETE /api/amistades/{}/cancelar", solicitudId);
            
            amistadService.cancelarSolicitud(solicitudId, auth);
            
            return ResponseEntity.ok(
                    new ApiResponse<>(null, "Solicitud cancelada", true));
                    
        } catch (IllegalArgumentException e) {
            log.warn("[AmistadController] Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (SecurityException e) {
            log.warn("[AmistadController] Acceso denegado al cancelar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (IllegalStateException e) {
            log.warn("[AmistadController] Error de estado al cancelar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[AmistadController] Error inesperado cancelando solicitud", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al cancelar solicitud", false));
        }
    }

    /**
     * Eliminar una amistad existente
     * 
     * @param amigoId ID del amigo a eliminar
     * @param auth Usuario autenticado
     * @return Confirmación de eliminación
     */
    @DeleteMapping("/{amigoId}")
    public ResponseEntity<ApiResponse<Void>> eliminarAmistad(
            @PathVariable UUID amigoId,
            Authentication auth) {
        try {
            log.info("[AmistadController] DELETE /api/amistades/{}", amigoId);
            
            amistadService.eliminarAmistad(amigoId, auth);
            
            return ResponseEntity.ok(
                    new ApiResponse<>(null, "Amistad eliminada", true));
                    
        } catch (IllegalArgumentException e) {
            log.warn("[AmistadController] Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (SecurityException e) {
            log.warn("[AmistadController] Acceso denegado al eliminar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (IllegalStateException e) {
            log.warn("[AmistadController] Error de estado al eliminar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[AmistadController] Error inesperado eliminando amistad", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al eliminar amistad", false));
        }
    }

    /**
     * Verificar el estado de la relación con otro usuario
     * 
     * @param amigoId ID del usuario a verificar
     * @param auth Usuario autenticado
     * @return Estado de la relación (existe, estado, etc.)
     */
    @GetMapping("/estado/{amigoId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerEstadoAmistad(
            @PathVariable UUID amigoId,
            Authentication auth) {
        try {
            log.debug("[AmistadController] GET /api/amistades/estado/{}", amigoId);
            
            Map<String, Object> estado = amistadService.obtenerEstadoAmistad(amigoId, auth);
            
            return ResponseEntity.ok(
                    new ApiResponse<>(estado, "Estado de amistad", true));
                    
        } catch (SecurityException e) {
            log.warn("[AmistadController] SecurityException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[AmistadController] Error obteniendo estado de amistad", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener estado", false));
        }
    }

    /**
     * Obtener estadísticas de amistades del usuario actual
     * 
     * @param auth Usuario autenticado
     * @return Contadores de amigos y solicitudes pendientes
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerEstadisticas(
            Authentication auth) {
        try {
            log.debug("[AmistadController] GET /api/amistades/estadisticas");
            
            // Obtener userId del authentication
            UUID userId = getUserIdFromAuth(auth);
            
            long totalAmigos = amistadService.contarAmigos(userId);
            long solicitudesPendientes = amistadService.contarSolicitudesPendientes(userId);
            
            Map<String, Object> estadisticas = Map.of(
                "total_amigos", totalAmigos,
                "solicitudes_pendientes", solicitudesPendientes
            );
            
            return ResponseEntity.ok(
                    new ApiResponse<>(estadisticas, "Estadísticas de amistades", true));
                    
        } catch (SecurityException e) {
            log.warn("[AmistadController] SecurityException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(null, e.getMessage(), false));
                    
        } catch (Exception e) {
            log.error("[AmistadController] Error obteniendo estadísticas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, "Error al obtener estadísticas", false));
        }
    }

    /**
     * Método auxiliar para extraer el UUID del usuario autenticado
     */
    private UUID getUserIdFromAuth(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("Usuario no autenticado");
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof uy.um.faltauno.config.CustomUserDetailsService.UserPrincipal) {
            return ((uy.um.faltauno.config.CustomUserDetailsService.UserPrincipal) principal).getId();
        }
        
        throw new SecurityException("No se pudo obtener el ID del usuario");
    }
}