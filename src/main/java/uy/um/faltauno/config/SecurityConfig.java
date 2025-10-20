package uy.um.faltauno.config;

import lombok.RequiredArgsConstructor;
import uy.um.faltauno.service.UsuarioService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UsuarioService usuarioService;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.authenticationProvider(authenticationProvider());

        http
            .cors().and()
            .csrf().disable()
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos existentes
                .requestMatchers(
                    "/api/auth/**",
                    "/api/usuarios",
                    "/public/**",
                    "/actuator/health",
                    "/h2-console/**",
                    "/error"
                ).permitAll()
                // >>> IMPORTANTE: permitir OAuth2 <<<
                .requestMatchers(
                    "/oauth2/**",
                    "/login/oauth2/**"
                ).permitAll()
                .anyRequest().authenticated()
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"success\":false,\"message\":\"Unauthorized\",\"data\":null}"
                    );
                })
            )

            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"success\":true,\"message\":\"Logout exitoso\",\"data\":null}"
                    );
                })
            )

            // >>> IMPORTANTE: habilitar oauth2Login y success handler <<<
            .oauth2Login(oauth -> oauth
                .loginPage("/oauth2/authorization/google")
                .successHandler(oauth2SuccessHandler())
            );

        // Filtro JWT antes del UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.headers().frameOptions().disable();
        return http.build();
    }

    @Bean
    public org.springframework.security.web.authentication.AuthenticationSuccessHandler oauth2SuccessHandler() {
        return (request, response, authentication) -> {
            // Usuario de Google
            var principal = (org.springframework.security.oauth2.core.user.DefaultOAuth2User) authentication.getPrincipal();
            String email = principal.getAttribute("email");
            String name  = principal.getAttribute("name");

            // Upsert de usuario (crea si no existe, actualiza si existe)
            var usuario = usuarioService.upsertGoogleUser(email, name, principal.getAttributes());

            // Generar JWT (usa tu JwtUtil actual)
            String jwt = jwtUtil.generateToken(email, usuario.getId().toString());

            // Redirigir al front con el token
            String frontend = System.getenv("FRONTEND_URL"); // ej: http://localhost:3000
            if (frontend == null || frontend.isBlank()) frontend = "http://localhost:3000";
            String target = frontend + "/oauth/success?token=" + java.net.URLEncoder.encode(jwt, java.nio.charset.StandardCharsets.UTF_8);
            response.sendRedirect(target);
        };
    }

}