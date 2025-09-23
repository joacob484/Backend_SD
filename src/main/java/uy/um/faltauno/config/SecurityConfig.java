package uy.um.faltauno.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
            // CORS habilitado (usa tu WebMvcConfigurer si lo tenés)
            .cors(Customizer.withDefaults())

            // Desactivamos CSRF para API SPA; si usás cookies y quieres protección CSRF, cambiar esto.
            .csrf().disable()

            // Configuración de rutas
            .authorizeHttpRequests(authorize -> authorize
                // endpoints públicos
                .requestMatchers("/auth/**", "/oauth2/**", "/public/**", "/actuator/health", "/h2-console/**").permitAll()
                // rutas estáticas (ajusta si tenés otras)
                .requestMatchers("/static/**", "/favicon.ico").permitAll()
                // todo lo demás requiere autenticación
                .anyRequest().authenticated()
            )

            // Configurar punto de entrada (si no autenticado)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new FrontendRedirectAuthenticationEntryPoint(frontendLoginUrl))
            )

            // Configurar logout (opcional)
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    // redirigir al frontend después del logout
                    response.sendRedirect(frontendUrl + "/login?logged_out=1");
                })
            )

            // Si usás OAuth2 login (Google), habilitalo
            .oauth2Login(oauth -> oauth
                .defaultSuccessUrl(frontendUrl + "/") // a donde ir despues de login OAuth
            );

        // Si usas H2 console en dev:
        http.headers().frameOptions().disable();

        return http.build();
    }
}
