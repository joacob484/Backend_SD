package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.dto.PartidoDTO;
import uy.um.faltauno.dto.UsuarioDTO;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.security.RequireAdmin;
import uy.um.faltauno.service.PartidoService;
import uy.um.faltauno.service.UsuarioService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador para funciones de administración
 */
@RestController
@RequestMapping("/api/admin")
@RequireAdmin
@Slf4j
@RequiredArgsConstructor
public class AdminController {
    
    private final UsuarioService usuarioService;
    private final PartidoService partidoService;
    
    /**
     * GET /api/admin/usuarios
     * Listar todos los usuarios (incluidos eliminados)
     */
    @GetMapping("/usuarios")
    public ResponseEntity<ApiResponse<List<UsuarioDTO>>> listarTodosUsuarios(
            @AuthenticationPrincipal Usuario admin) {
        try {
            log.info("[ADMIN] {} listando todos los usuarios", admin.getEmail());
            
            List<UsuarioDTO> usuarios = usuarioService.listarTodosInclusoEliminados();
            
            return ResponseEntity.ok(new ApiResponse<>(usuarios, 
                    String.format("Total: %d usuarios", usuarios.size()), 
                    true));
        } catch (Exception e) {
            log.error("[ADMIN] Error al listar usuarios", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al listar usuarios", false));
        }
    }
    
    /**
     * GET /api/admin/partidos
     * Listar todos los partidos
     */
    @GetMapping("/partidos")
    public ResponseEntity<ApiResponse<List<PartidoDTO>>> listarTodosPartidos(
            @AuthenticationPrincipal Usuario admin) {
        try {
            log.info("[ADMIN] {} listando todos los partidos", admin.getEmail());
            
            List<PartidoDTO> partidos = partidoService.listarTodosParaAdmin();
            
            return ResponseEntity.ok(new ApiResponse<>(partidos, 
                    String.format("Total: %d partidos", partidos.size()), 
                    true));
        } catch (Exception e) {
            log.error("[ADMIN] Error al listar partidos", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al listar partidos", false));
        }
    }
    
    /**
     * DELETE /api/admin/usuarios/{id}
     * Eliminar permanentemente un usuario (hard delete)
     */
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarUsuarioPermanente(
            @AuthenticationPrincipal Usuario admin,
            @PathVariable String id) {
        try {
            log.warn("[ADMIN] {} eliminando permanentemente usuario {}", admin.getEmail(), id);
            
            usuarioService.eliminarPermanentemente(id);
            
            return ResponseEntity.ok(new ApiResponse<>(null, 
                    "Usuario eliminado permanentemente", 
                    true));
        } catch (Exception e) {
            log.error("[ADMIN] Error al eliminar usuario", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al eliminar usuario", false));
        }
    }
    
    /**
     * DELETE /api/admin/partidos/{id}
     * Eliminar permanentemente un partido
     */
    @DeleteMapping("/partidos/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarPartido(
            @AuthenticationPrincipal Usuario admin,
            @PathVariable String id) {
        try {
            log.warn("[ADMIN] {} eliminando partido {}", admin.getEmail(), id);
            
            // Convertir String a Long
            Long partidoId = Long.parseLong(id);
            partidoService.eliminarPartidoAdmin(partidoId);
            
            return ResponseEntity.ok(new ApiResponse<>(null, 
                    "Partido eliminado", 
                    true));
        } catch (NumberFormatException e) {
            log.error("[ADMIN] ID de partido inválido: {}", id);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(null, "ID de partido inválido", false));
        } catch (Exception e) {
            log.error("[ADMIN] Error al eliminar partido", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al eliminar partido", false));
        }
    }
    
    /**
     * GET /api/admin/stats
     * Obtener estadísticas del sistema
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerEstadisticas(
            @AuthenticationPrincipal Usuario admin) {
        try {
            log.info("[ADMIN] {} obteniendo estadísticas", admin.getEmail());
            
            Map<String, Object> stats = new HashMap<>();
            // Total usuarios (no eliminados)
            stats.put("totalUsuarios", usuarioService.contarUsuariosActivos());
            // Usuarios activos en los últimos 30 días (con actividad reciente)
            stats.put("usuariosActivos", usuarioService.contarUsuariosConActividadReciente(30));
            stats.put("registrosRecientes", usuarioService.contarRegistrosRecientes(7));
            stats.put("totalPartidos", partidoService.contarPartidos());
            stats.put("partidosHoy", partidoService.contarPartidosHoy());
            
            return ResponseEntity.ok(new ApiResponse<>(stats, 
                    "Estadísticas obtenidas", 
                    true));
        } catch (Exception e) {
            log.error("[ADMIN] Error al obtener estadísticas", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al obtener estadísticas", false));
        }
    }
    
    /**
     * PUT /api/admin/usuarios/{id}/rol
     * Cambiar rol de un usuario
     */
    @PutMapping("/usuarios/{id}/rol")
    public ResponseEntity<ApiResponse<UsuarioDTO>> cambiarRol(
            @AuthenticationPrincipal Usuario admin,
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            String nuevoRol = request.get("rol");
            
            if (!"USER".equals(nuevoRol) && !"ADMIN".equals(nuevoRol)) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "Rol inválido. Debe ser USER o ADMIN", false));
            }
            
            log.warn("[ADMIN] {} cambiando rol de usuario {} a {}", admin.getEmail(), id, nuevoRol);
            
            UsuarioDTO usuario = usuarioService.cambiarRol(id, nuevoRol);
            
            return ResponseEntity.ok(new ApiResponse<>(usuario, 
                    "Rol actualizado correctamente", 
                    true));
        } catch (Exception e) {
            log.error("[ADMIN] Error al cambiar rol", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al cambiar rol", false));
        }
    }
}
