package uy.um.faltauno.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.extern.slf4j.Slf4j;

/**
 * Controlador para manejar errores y mostrar página de error personalizada
 */
@Controller
@Slf4j
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    /**
     * Endpoint de error que muestra información del error
     */
    @GetMapping("/error")
    public String handleError(
        @RequestParam(required = false) String message,
        @RequestParam(required = false) String details,
        Model model
    ) {
        log.error("[ErrorController] Error page accessed - Message: {}, Details: {}", message, details);
        
        model.addAttribute("errorMessage", message != null ? message : "Ha ocurrido un error");
        model.addAttribute("errorDetails", details != null ? details : "Por favor, intenta nuevamente");
        
        return "error"; // Renderiza error.html
    }
}
