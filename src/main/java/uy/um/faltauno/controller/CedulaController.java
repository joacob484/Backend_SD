package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.UsuarioDTO;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.service.UsuarioService;

import java.util.Map;

/**
 * Controlador REST para verificación de cédula uruguaya.
 * 
 * Endpoints:
 * - POST /api/cedula/verify - Verificar validez de una cédula
 * - POST /api/cedula/save - Guardar cédula verificada para el usuario autenticado
 */
@Slf4j
@RestController
@RequestMapping("/api/cedula")
@RequiredArgsConstructor
public class CedulaController {

    private final UsuarioService usuarioService;

    /**
     * Verificar la validez de una cédula uruguaya sin guardarla.
     * Endpoint público para que el frontend pueda validar en tiempo real.
     *
     * @param payload JSON con campo "cedula"
     * @return {success: boolean, valid: boolean, message: string}
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verificarCedula(@RequestBody Map<String, String> payload) {
        String cedula = payload.get("cedula");
        
        if (cedula == null || cedula.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "valid", false,
                "message", "La cédula es requerida"
            ));
        }

        try {
            boolean esValida = usuarioService.verificarCedula(cedula);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "valid", esValida,
                "message", esValida ? "Cédula válida" : "Cédula inválida - verifica los dígitos"
            ));
        } catch (Exception e) {
            log.error("[CedulaController] Error al verificar cédula", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "valid", false,
                "message", "Error al verificar la cédula: " + e.getMessage()
            ));
        }
    }

    /**
     * Guardar cédula verificada para el usuario autenticado.
     * Solo guarda si la cédula es válida.
     *
     * @param user Usuario autenticado (inyectado por Spring Security)
     * @param payload JSON con campo "cedula"
     * @return {success: boolean, data: UsuarioDTO, message: string}
     */
    @PostMapping("/save")
    public ResponseEntity<?> guardarCedula(
            @AuthenticationPrincipal Usuario user,
            @RequestBody Map<String, String> payload
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Usuario no autenticado"
            ));
        }

        String cedula = payload.get("cedula");
        
        if (cedula == null || cedula.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "La cédula es requerida"
            ));
        }

        try {
            // Primero verificar que sea válida
            boolean esValida = usuarioService.verificarCedula(cedula);
            
            if (!esValida) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "La cédula ingresada no es válida. Verifica los dígitos."
                ));
            }

            // Si es válida, guardarla
            UsuarioDTO usuarioActualizado = usuarioService.saveCedulaForUser(user.getId(), cedula);
            
            log.info("[CedulaController] Cédula guardada exitosamente para usuario: {}", user.getEmail());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", usuarioActualizado,
                "message", "Cédula guardada correctamente"
            ));
        } catch (Exception e) {
            log.error("[CedulaController] Error al guardar cédula para usuario: {}", user.getEmail(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al guardar la cédula: " + e.getMessage()
            ));
        }
    }
}
