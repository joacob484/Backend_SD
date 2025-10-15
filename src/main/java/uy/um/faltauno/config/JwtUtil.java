package uy.um.faltauno.config;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Ejemplo sencillo. Implementá con tu librería JWT preferida (jjwt, nimbus, etc).
 */
@Component
public class JwtUtil {

    // valida token (firma, expiración, etc)
    public boolean validateToken(String token) {
        // implementar validación real
        return token != null && !token.isBlank();
    }

    // extrae username/email del token
    public String getUsernameFromToken(String token) {
        // implementar: parsear claims y devolver subject o claim "sub"/"email"
        return "user@example.com"; // placeholder; reemplazar por valor real
    }

    // devuelve roles desde claims (si los tenés)
    public List<String> getRolesFromToken(String token) {
        // ejemplo: retornar lista vacía o ["ROLE_USER"]
        return Collections.singletonList("ROLE_USER");
    }
}