package uy.um.faltauno.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        String frontendLoginUrl = frontendUrl + "/login";

        // Registrar el provider para que Spring use tu CustomUserDetailsService + PasswordEncoder
        http.authenticationProvider(authenticationProvider());

        http
            .cors(Customizer.withDefaults())
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/usuarios", "/api/auth/**").permitAll()
                .requestMatchers("/public/**", "/actuator/health", "/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                // Para accesos no autenticados en navegadores redirigimos al login frontend
                .authenticationEntryPoint(new FrontendRedirectAuthenticationEntryPoint(frontendLoginUrl))
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, auth) -> response.sendRedirect(frontendLoginUrl + "?logged_out=1"))
            )
            // Form login configurado para API: no redirecciones, devolvemos JSON en success/failure
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    try {
                        // Puedes devolver más datos (ej: user id) si lo deseas
                        response.getWriter().write("{\"success\":true}");
                    } catch (IOException e) {
                        // nada extra; si falla la escritura, status ya es 200
                    }
                })
                .failureHandler((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    try {
                        // No exponemos detalles sensibles; devolvemos mensaje genérico
                        response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}");
                    } catch (IOException e) {
                        // nada
                    }
                })
                .permitAll()
            )
            .httpBasic(Customizer.withDefaults())
            .oauth2Login(oauth -> oauth
                .defaultSuccessUrl(frontendUrl + "/")
            );

        http.headers().frameOptions().disable(); // H2 console

        return http.build();
    }
}