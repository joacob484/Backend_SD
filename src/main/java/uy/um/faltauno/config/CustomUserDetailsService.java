package uy.um.faltauno.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.UsuarioRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Servicio que carga UserDetails a partir de la entidad Usuario.
 * Usa una proyección (AuthProjection) para no leer LOBs (foto) durante autenticación.
 * 
 * ⚠️ SEGURIDAD: Valida que solo usuarios LOCAL puedan autenticarse con email/password.
 * Usuarios OAuth (Google, etc.) deben usar su flujo respectivo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1️⃣ Cargar proyección ligera (sin LOBs)
        UsuarioRepository.AuthProjection proj = usuarioRepository.findAuthProjectionByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        // 2️⃣ ⚡ CRÍTICO: Cargar entidad completa para validar provider
        // Esto es necesario porque AuthProjection no incluye el campo 'provider'
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
        
        // 3️⃣ ⚡ SEGURIDAD: Validar que usuarios OAuth NO puedan autenticarse con password
        if (!"LOCAL".equals(usuario.getProvider())) {
            throw new BadCredentialsException(
                "Este usuario debe autenticarse con " + usuario.getProvider() + ". " +
                "No se permite autenticación con email/password para cuentas OAuth."
            );
        }
        
        // 4️⃣ ⚡ SEGURIDAD: Validar que el password no sea NULL (usuarios OAuth tienen password=NULL)
        if (proj.getPassword() == null || proj.getPassword().isEmpty()) {
            throw new BadCredentialsException(
                "Credenciales inválidas. Este usuario debe autenticarse con OAuth."
            );
        }
        
        // 5️⃣ ⚡ SEGURIDAD: Validar que el usuario no esté eliminado (soft delete)
        if (usuario.getDeletedAt() != null) {
            throw new BadCredentialsException(
                "Esta cuenta ha sido eliminada. Contacta soporte para recuperarla."
            );
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        // No incluimos fotoPerfil aquí, para evitar problemas con LOBs fuera de tx.
        return new UserPrincipal(proj.getId(), proj.getEmail(), proj.getPassword(), authorities);
    }

    public static class UserPrincipal implements UserDetails {
        private final UUID id;
        private final String username;
        private final String password;
        private final Collection<? extends GrantedAuthority> authorities;

        public UserPrincipal(UUID id, String username, String password, Collection<? extends GrantedAuthority> authorities) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.authorities = authorities;
        }

        public UUID getId() { return id; }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
        @Override public String getPassword() { return password; }
        @Override public String getUsername() { return username; }
        @Override public boolean isAccountNonExpired() { return true; }
        @Override public boolean isAccountNonLocked() { return true; }
        @Override public boolean isCredentialsNonExpired() { return true; }
        @Override public boolean isEnabled() { return true; }
    }
}