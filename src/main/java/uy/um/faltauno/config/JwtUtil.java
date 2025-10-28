package uy.um.faltauno.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utilidad JWT mejorada con manejo correcto de UUIDs y validación robusta.
 */
@Component
public class JwtUtil {
    
    @Value("${jwt.secret:mi_clave_super_segura_que_debe_ser_al_menos_256_bits_para_hs256}")
    private String secretKey;
    
    @Value("${jwt.expiration:86400000}") // 24 horas en milisegundos
    private long EXPIRATION_TIME;

    // Clave generada una vez para mejor seguridad
    private SecretKey getSigningKey() {
        // enforce UTF-8 to avoid platform charset differences
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un token JWT para un usuario.
     * @param userId UUID del usuario (se convierte a String internamente)
     * @param email Email del usuario (usado como subject)
     * @return Token JWT firmado
     */
    public String generateToken(UUID userId, String email) {
        return Jwts.builder()
                .subject(email) // Subject es el email (username)
                .claim("userId", userId.toString()) // UUID como claim separado
                .claim("roles", List.of("ROLE_USER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Sobrecarga para compatibilidad con código existente.
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("roles", List.of("ROLE_USER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey())
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
    // Use the parserBuilder API (jjwt 0.11+) to set the signing key and parse the JWS
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

    /**
     * Genera un token con claims adicionales personalizados.
     * @param extraClaims Claims adicionales a incluir en el token
     * @param email Email del usuario (usado como subject)
     * @return Token JWT firmado
     */
    public String generateToken(Map<String, Object> extraClaims, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Genera un token con userId como claim adicional.
     * @param email Email del usuario (usado como subject)
     * @param userId ID del usuario como string
     * @return Token JWT firmado
     */
    public String generateToken(String email, String userId) {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userId", userId);
        claims.put("roles", List.of("ROLE_USER"));
        return generateToken(claims, email);
    }
}