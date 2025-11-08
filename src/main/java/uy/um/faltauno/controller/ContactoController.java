package uy.um.faltauno.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uy.um.faltauno.dto.ApiResponse;
import uy.um.faltauno.dto.ContactoDTO;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.service.ContactoService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contactos")
@Slf4j
@RequiredArgsConstructor
public class ContactoController {
    
    private final ContactoService contactoService;
    
    /**
     * GET /api/contactos
     * Obtener todos los contactos sincronizados del usuario autenticado
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ContactoDTO>>> listarContactos(
            @AuthenticationPrincipal Usuario usuario) {
        try {
            log.info("GET /api/contactos - Usuario: {}", usuario.getEmail());
            
            List<ContactoDTO> contactos = contactoService.listarContactos(usuario);
            
            return ResponseEntity.ok(new ApiResponse<>(contactos, 
                    String.format("Contactos obtenidos: %d", contactos.size()), 
                    true));
        } catch (Exception e) {
            log.error("Error al listar contactos", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al obtener contactos", false));
        }
    }
    
    /**
     * POST /api/contactos/sincronizar
     * Sincronizar contactos del dispositivo
     * Body: { "contactos": [{"nombre": "Juan", "apellido": "Perez", "celular": "+59899123456"}, ...] }
     */
    @PostMapping("/sincronizar")
    public ResponseEntity<ApiResponse<List<ContactoDTO>>> sincronizarContactos(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody Map<String, List<Map<String, String>>> request) {
        try {
            List<Map<String, String>> contactosData = request.get("contactos");
            
            if (contactosData == null || contactosData.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(null, "No se proporcionaron contactos", false));
            }
            
            log.info("POST /api/contactos/sincronizar - Usuario: {}, Contactos: {}", 
                    usuario.getEmail(), contactosData.size());
            
            List<ContactoDTO> contactos = contactoService.sincronizarContactos(usuario, contactosData);
            
            long enLaApp = contactos.stream().filter(ContactoDTO::getIsOnApp).count();
            
            return ResponseEntity.ok(new ApiResponse<>(contactos, 
                    String.format("Sincronizados %d contactos, %d están en la app", 
                            contactos.size(), enLaApp), 
                    true));
        } catch (Exception e) {
            log.error("Error al sincronizar contactos", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null, "Error al sincronizar contactos: " + e.getMessage(), false));
        }
    }
}
