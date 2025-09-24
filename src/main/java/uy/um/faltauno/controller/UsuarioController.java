package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.web.exchanges.HttpExchange.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import uy.um.faltauno.dto.UsuarioDTO;
import uy.um.faltauno.service.UsuarioService;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.dto.PendingReviewResponse;
import uy.um.faltauno.dto.VerificarCedulaResponse;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    @Autowired
    private final UsuarioService usuarioService;    

    @PostMapping
    public ResponseEntity<UsuarioDTO> createUsuario(@RequestBody UsuarioDTO dto) {
        return ResponseEntity.ok(usuarioService.createUsuario(dto));
    }

    @PostMapping("/verificar-cedula")
    public ApiResponse<VerificarCedulaResponse> verificarCedula(@RequestBody Map<String, String> body) {
        String cedula = body.get("cedula");
        if (cedula == null) {
            return new ApiResponse<>(null, "Cédula requerida", false);
        }

        boolean verified = usuarioService.verificarCedula(cedula);
        return new ApiResponse<>(new VerificarCedulaResponse(verified), "Verificación completada", true);
    }

    @PostMapping("/{id}/foto")
    public ResponseEntity<?> subirFoto(@PathVariable UUID id, @RequestParam("foto") MultipartFile foto) {
        try {
            // Guardar el archivo en la carpeta "uploads"
            String uploadsDir = "uploads/";
            Path uploadPath = Paths.get(uploadsDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFileName = foto.getOriginalFilename();
            // para evitar colisiones, podés prefijar el filename con UUID por ejemplo
            String safeFileName = UUID.randomUUID().toString() + "-" + (originalFileName != null ? originalFileName : "foto");
            Path filePath = uploadPath.resolve(safeFileName);
            Files.copy(foto.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Construir URL pública completa (opcionalmente usar propiedad de aplicación para base URL)
            // Si tu API corre en https://api.miapp.com, la URL final sería: https://api.miapp.com/uploads/{safeFileName}
            // Aquí devolvemos la ruta relativa; el frontend puede prepender el host si lo desea.
            String fotoUrl = "/uploads/" + safeFileName;

            // Guardar la URL de la foto en la base de datos
            usuarioService.actualizarFoto(id, fotoUrl);

            // Respuesta con la estructura que espera el front: { data: { url: string }, message, success }
            Map<String, Object> data = Map.of("url", fotoUrl);
            return ResponseEntity.ok().body(Map.of(
                "data", data,
                "message", "Foto subida correctamente",
                "success", true
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "data", Map.of(),
                "message", "Error subiendo la foto",
                "success", false
            ));
        }
    }

    @GetMapping("/{id}/friend-requests")
    public ApiResponse<List<Map<String, Object>>> getFriendRequests(@PathVariable UUID id) {
        // Llamamos a un método del service que devuelva solicitudes pendientes
        List<Map<String, Object>> requests = usuarioService.obtenerSolicitudesAmistadPendientes(id);
        return new ApiResponse<>(requests, "Solicitudes de amistad pendientes", true);
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<Map<String, Object>>> getUnreadMessages(@PathVariable UUID id) {
        List<Map<String, Object>> mensajes = usuarioService.obtenerMensajesNoLeidos(id);
        return new ApiResponse<>(mensajes, "Mensajes no leídos", true);
    }

    @GetMapping("/{id}/match-updates")
    public ApiResponse<List<Map<String, Object>>> getMatchUpdates(@PathVariable UUID id) {
        List<Map<String, Object>> updates = usuarioService.obtenerActualizacionesPartidos(id);
        return new ApiResponse<>(updates, "Actualizaciones de partidos", true);
    }

    @GetMapping("/{id}/match-invitations")
    public ApiResponse<List<Map<String, Object>>> getMatchInvitations(@PathVariable UUID id) {
        List<Map<String, Object>> invites = usuarioService.obtenerInvitaciones(id);
        return new ApiResponse<>(invites, "Invitaciones a partidos", true);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> getUsuario(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.getUsuario(id));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> getAllUsuarios() {
        return ResponseEntity.ok(usuarioService.getAllUsuarios());
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Principal principal) {
        // Esto asume que el principal contiene el UUID como nombre o que tenés seguridad configurada.
        // Ajustá según tu mecanismo de autenticación (JWT, session, etc.)
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("data", null, "message", "No autenticado", "success", false));
            }
            // suponer principal.getName() devuelve el UUID en string
            UUID userId = UUID.fromString(principal.getName());
            UsuarioDTO dto = usuarioService.getUsuario(userId);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("data", null, "message", "Error", "success", false));
        }
    }


    @GetMapping("/{id}/pending-reviews")
    public ApiResponse<List<PendingReviewResponse>> getPendingReviews(@PathVariable UUID id) {
        List<PendingReviewResponse> pendientes = usuarioService.obtenerPendingReviews(id);
        return new ApiResponse<>(pendientes, "Reseñas pendientes", true);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUsuario(@PathVariable UUID id) {
        usuarioService.deleteUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
