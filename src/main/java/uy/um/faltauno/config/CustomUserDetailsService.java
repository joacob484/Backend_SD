package uy.um.faltauno.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import uy.um.faltauno.repository.UsuarioRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Servicio que carga UserDetails a partir de la entidad Usuario.
 * Usa una proyección (AuthProjection) para no leer LOBs (foto) durante autenticación.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UsuarioRepository.AuthProjection proj = usuarioRepository.findAuthProjectionByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

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