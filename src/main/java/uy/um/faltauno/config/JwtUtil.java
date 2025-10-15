package uy.um.faltauno.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Utilidad JWT mejorada con manejo correcto de UUIDs y validación robusta.
 */
@Component
public class JwtUtil {
    
    @Value("${jwt.secret:mi_clave_super_segura_que_debe_ser_al_menos_256_bits_para_hs256}")
    private String SECRET_KEY;
    
    @Value("${jwt.expiration:86400000}") // 24 horas en milisegundos
    private long EXPIRATION_TIME;

    // Clave generada una vez para mejor seguridad
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * Genera un token JWT para un usuario.
     * @param userId UUID del usuario (se convierte a String internamente)
     * @param email Email del usuario (usado como subject)
     * @return Token JWT firmado
     */
    public String generateToken(UUID userId, String email) {
        return Jwts.builder()
                .setSubject(email) // Subject es el email (username)
                .claim("userId", userId.toString()) // UUID como claim separado
                .claim("roles", List.of("ROLE_USER"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Sobrecarga para compatibilidad con código existente.
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("roles", List.of("ROLE_USER"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extrae el userId del token (desde el claim).
     * @param token Token JWT
     * @return UUID del usuario, o null si no existe el claim
     */
    public UUID extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String userIdStr = claims.get("userId", String.class);
            if (userIdStr != null && !userIdStr.isBlank()) {
                return UUID.fromString(userIdStr);
            }
        } catch (Exception e) {
            // Si no hay claim userId, intentar parsear el subject como UUID (fallback)
            try {
                String subject = getUsernameFromToken(token);
                return UUID.fromString(subject);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * Extrae el username (email) del subject del token.
     */
    public String getUsernameFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extrae todos los claims del token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Valida si el token es válido (firma correcta y no expirado).
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token); // Si esto no lanza excepción, el token es válido
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica si el token está expirado.
     */
    private boolean isTokenExpired(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Extrae los roles desde el claim 'roles'.
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List) {
                return (List<String>) rolesObj;
            }
        } catch (Exception e) {
            // Ignorar
        }
        return Collections.singletonList("ROLE_USER");
    }

    /**
     * Extrae el email del token (alias de getUsernameFromToken).
     */
    public String extractEmail(String token) {
        return getUsernameFromToken(token);
    }
}