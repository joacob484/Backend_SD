package uy.um.faltauno.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// IMPORTA tu implementación concreta si la tenés en otro paquete
// import uy.um.faltauno.config.FrontendRedirectAuthenticationEntryPoint;
import uy.um.faltauno.config.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Filtro JWT propio
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Handler de éxito de OAuth2 DEFINIDO COMO @Component (Opción A)
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    // Tu UserDetailsService (p.ej. CustomUserDetailsService) ya definido como @Service
    private final UserDetailsService userDetailsService;

    // PasswordEncoder (puede venir de donde lo tengas definido @Bean; si lo definís acá, también sirve)
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // Si ya tenés tu propio entry point, inyectalo aquí; de lo contrario se usa uno simple JSON
    private final FrontendRedirectAuthenticationEntryPoint frontendEntryPoint;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder);
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .cors(cors -> {}) // CORS lo maneja tu WebConfig, acá solo se habilita
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint())
            )

            .authorizeHttpRequests(auth -> auth
                // Rutas públicas propias
                .requestMatchers(
                    "/api/auth/**",
                    "/public/**",
                    "/actuator/health",
                    "/error",
                    "/h2-console/**"
                ).permitAll()

                // IMPORTANTE: permitir todo el flujo OAuth2
                .requestMatchers(
                    "/oauth2/**",
                    "/login/oauth2/**"
                ).permitAll()

                .anyRequest().authenticated()
            )

            // Login OAuth2 con handler externo (Opción A)
            .oauth2Login(oauth -> oauth
                .loginPage("/oauth2/authorization/google")
                .successHandler(oAuth2SuccessHandler)
            )

            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"success\":true,\"message\":\"Logout exitoso\",\"data\":null}");
                })
            )

            // Provider para auth por credenciales locales (si aplica en tu flujo)
            .authenticationProvider(authenticationProvider());

        // Filtro JWT antes del UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Consola H2 si la usás en dev
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    /**
     * Entry point por defecto si no inyectaste uno custom.
     * Si ya usás FrontendRedirectAuthenticationEntryPoint, podés retornar ese acá.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        // Si tenés un entrypoint custom, descomenta la siguiente línea y borra el lambda:
        // return frontendEntryPoint;

        // Fallback simple JSON
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\",\"data\":null}");
        };
    }
}