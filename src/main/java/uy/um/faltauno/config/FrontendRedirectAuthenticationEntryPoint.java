package uy.um.faltauno.config;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public class FrontendRedirectAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final String frontendLoginUrl;

    public FrontendRedirectAuthenticationEntryPoint(String frontendLoginUrl) {
        this.frontendLoginUrl = frontendLoginUrl;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        String accept = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        String contentType = request.getHeader("Content-Type");

        boolean isApiRequest = false;

        // Considerar como API/XHR si: Accept contiene application/json, o petición X-Requested-With = XMLHttpRequest,
        // o content-type JSON, o la ruta empieza con /api or /rest
        if ((accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE))
                || request.getRequestURI().startsWith("/api")
                || request.getRequestURI().startsWith("/rest")) {
            isApiRequest = true;
        }

        if (isApiRequest) {
            // devolver 401 JSON para que el frontend lo maneje
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Unauthorized\",\"status\":401}");
        } else {
            // redirigir a la página de login del frontend
            response.sendRedirect(frontendLoginUrl);
        }
    }
}

