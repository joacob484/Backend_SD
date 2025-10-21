package uy.um.faltauno.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                DaoAuthenticationProvider provider,
                                                JwtAuthenticationFilter jwtFilter,
                                                OAuth2SuccessHandler oAuth2SuccessHandler) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
        .requestMatchers("/oauth2/**","/login/oauth2/**","/api/auth/**","/public/**","/actuator/health","/error","/h2-console/**").permitAll()
        .anyRequest().authenticated()
        )
        .oauth2Login(oauth -> oauth
        .loginPage("/oauth2/authorization/google")
        .successHandler(oAuth2SuccessHandler)
        )
        .authenticationProvider(provider);

    http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    http.headers(h -> h.frameOptions(f -> f.disable()));
    return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        // usa tu entrypoint si querés:
        // return frontendEntryPoint;
        return (request, response, ex) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\",\"data\":null}");
        };
    }
}