package uy.um.faltauno.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("!dev") // Solo activo cuando NO es perfil dev
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Value("${FRONTEND_URL:https://faltauno-frontend-169771742214.us-central1.run.app}")
    private String frontendUrl;

    // ======== Autenticación base ========
    @Bean
    @SuppressWarnings("deprecation")
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
        return new ProviderManager(List.of(provider)); // Updated to use List.of for compatibility
    }

    // ======== Configuración principal ========
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   DaoAuthenticationProvider provider) throws Exception {

        http
            // --- CORS y CSRF ---
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())

            // --- Manejo de sesiones (OAuth2 necesita una sesión temporal) ---
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            // --- Autorización de endpoints ---
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/verification/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                .requestMatchers("/api/novedades").permitAll()
                .requestMatchers("/ws/info").permitAll()  // SockJS info endpoint
                .requestMatchers("/public/**", "/actuator/health", "/error").permitAll()
                .anyRequest().authenticated()
            )

            // --- OAuth2 Login (Google) ---
            .oauth2Login(oauth -> oauth
                .loginPage("/oauth2/authorization/google")
                .successHandler(oAuth2SuccessHandler)
            )

            // --- Provider personalizado (usuario/clave) ---
            .authenticationProvider(provider)

            // --- Manejo de errores de autenticación ---
            .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint()));

        // --- Filtros personalizados ---
        // CRITICAL: Add CORS filter BEFORE any security filters
        http.addFilterBefore(new CorsFilter(corsConfigurationSource()), CsrfFilter.class);
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // --- Headers de seguridad ---
        http.headers(h -> h
            .frameOptions(f -> f.deny()) // evita clickjacking
            .xssProtection(xss -> xss.disable()) // ya controlado por CSP
            .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
        );

        return http.build();
    }

    // ======== Configuración CORS ========
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true); // Set BEFORE patterns (workaround Spring 6.2.7 bug)

        configuration.setAllowedOriginPatterns(List.of(
            "https://*.run.app",
            frontendUrl
        ));

        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With"
        ));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // ======== Entry Point para respuestas 401 JSON ========
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
                {"success":false,"message":"Unauthorized","data":null}
            """);
        };
    }
}