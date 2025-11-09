package uy.um.faltauno.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración Web MVC para recursos estáticos e interceptores.
 * 
 * ⚠️ IMPORTANTE: La configuración CORS está en SecurityConfig.java
 * para evitar conflictos con Spring Security.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig {
    
    // ✅ Inyectar como interfaz HandlerInterceptor (no como clase concreta)
    // Esto permite que Spring use proxies JDK para @Async sin problemas
    private final HandlerInterceptor activityTrackingInterceptor;
    private final HandlerInterceptor adminRoleInterceptor;

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {

            @Override
            public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations("file:uploads/");
            }
            
            @Override
            public void addInterceptors(@NonNull InterceptorRegistry registry) {
                // Interceptor de tracking de actividad de usuarios
                registry.addInterceptor(activityTrackingInterceptor)
                        .addPathPatterns("/api/**") // Solo rutas de API
                        .excludePathPatterns(
                            "/api/auth/login",
                            "/api/auth/register", 
                            "/api/auth/oauth/**",
                            "/api/health",
                            "/api/actuator/**"
                        );
                
                // Interceptor para verificar permisos de administrador
                registry.addInterceptor(adminRoleInterceptor)
                        .addPathPatterns("/api/**"); // Aplica a todas las rutas de API
            }
        };
    }
}