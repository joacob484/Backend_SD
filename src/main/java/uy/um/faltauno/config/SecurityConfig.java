package uy.um.faltauno.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // URL de login frontend (a donde redirigir)
        String frontendLoginUrl = frontendUrl + "/login";

        http
            // Habilitamos CORS
            .cors(Customizer.withDefaults())

            // CSRF deshabilitado para SPA
            .csrf().disable()

            // Configuración de rutas
            .authorizeHttpRequests(authorize -> authorize
                // Endpoints públicos
                .requestMatchers(HttpMethod.POST, "/api/usuarios", "/api/auth/register", "/api/auth/register-user").permitAll()
                .requestMatchers("/auth/**", "/oauth2/**", "/public/**", "/actuator/health", "/h2-console/**").permitAll()
                .requestMatchers("/static/**", "/favicon.ico").permitAll()
                // Todo lo demás requiere autenticación
                .anyRequest().authenticated()
            )

            // Punto de entrada si no autenticado
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new FrontendRedirectAuthenticationEntryPoint(frontendLoginUrl))
            )

            // Logout
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.sendRedirect(frontendUrl + "/login?logged_out=1");
                })
            )

            // OAuth2 login (Google)
            .oauth2Login(oauth -> oauth
                .defaultSuccessUrl(frontendUrl + "/")
            );

        // Permitir H2 console en dev
        http.headers().frameOptions().disable();

        return http.build();
    }
}