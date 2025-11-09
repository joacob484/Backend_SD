package uy.um.faltauno.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
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
     * Genera un token JWT para un usuario con token versioning.
     * @param userId UUID del usuario
     * @param email Email del usuario (usado como subject)
     * @param tokenVersion Versión actual del token del usuario
     * @param rol Rol del usuario (USER o ADMIN)
     * @return Token JWT firmado
     */
    public String generateToken(UUID userId, String email, Integer tokenVersion, String rol) {
        Date now = new Date();
        String jti = UUID.randomUUID().toString(); // JWT ID único por token
        
        // Determinar rol Spring Security basado en el rol del usuario
        String springRole = "ADMIN".equals(rol) ? "ROLE_ADMIN" : "ROLE_USER";
        
        return Jwts.builder()
                .subject(email) // Subject es el email (username)
                .claim("userId", userId.toString()) // UUID como claim separado
                .claim("tokenVersion", tokenVersion) // Versión del token (CRÍTICO)
                .claim("jti", jti) // ID único del token
                .claim("rol", rol) // Rol del usuario (USER/ADMIN)
                .claim("roles", List.of(springRole)) // Para Spring Security
                .issuedAt(now)
                .expiration(new Date(now.getTime() + EXPIRATION_TIME))
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Genera un token JWT para un usuario con token versioning (sin rol - deprecated).
     * @param userId UUID del usuario
     * @param email Email del usuario (usado como subject)
     * @param tokenVersion Versión actual del token del usuario
     * @return Token JWT firmado
     * @deprecated Usar generateToken(UUID, String, Integer, String) para incluir el rol
     */
    @Deprecated
    public String generateToken(UUID userId, String email, Integer tokenVersion) {
        return generateToken(userId, email, tokenVersion, "USER");
    }
    
    /**
     * Genera un token JWT para un usuario (backwards compatible).
     * @param userId UUID del usuario (se convierte a String internamente)
     * @param email Email del usuario (usado como subject)
     * @return Token JWT firmado
     */
    public String generateToken(UUID userId, String email) {
        return generateToken(userId, email, 1); // Default version 1
    }

    /**
     * Sobrecarga para compatibilidad con código existente.
     */
    public String generateToken(String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("roles", List.of("ROLE_USER"))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + EXPIRATION_TIME))
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
     * Extrae la versión del token (CRÍTICO para invalidación).
     * @param token Token JWT
     * @return Versión del token, o null si no existe
     */
    public Integer extractTokenVersion(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get("tokenVersion", Integer.class);
        } catch (Exception e) {
            return null; // Tokens viejos sin versión
        }
    }
    
    /**
     * Extrae el JWT ID único del token.
     * @param token Token JWT
     * @return JTI (JWT ID), o null si no existe
     */
    public String extractJti(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get("jti", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrae el username (email) del subject del token.
     */
    public String getUsernameFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extrae todos los claims del token usando jjwt 0.12.x.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())   // establece la key de verificación
                .build()
                .parseSignedClaims(token)      // valida firma y parsea
                .getPayload();                 // obtiene los Claims
    }

    /**
     * Valida si el token es válido (firma correcta y no expirado).
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token); // lanza si es inválido
            Date exp = claims.getExpiration();
            return exp != null && exp.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extrae el rol del usuario desde el claim 'rol'.
     * @param token Token JWT
     * @return Rol del usuario (USER/ADMIN), o USER por defecto
     */
    public String extractRol(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String rol = claims.get("rol", String.class);
            return rol != null ? rol : "USER";
        } catch (Exception e) {
            return "USER";
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
        } catch (Exception ignored) {}
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