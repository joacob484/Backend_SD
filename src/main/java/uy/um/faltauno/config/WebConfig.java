package uy.um.faltauno.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uy.um.faltauno.interceptor.ActivityTrackingInterceptor;

@Configuration
@RequiredArgsConstructor
public class WebConfig {

    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;
    
    private final ActivityTrackingInterceptor activityTrackingInterceptor;

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**") // ✅ Aplicar a todas las rutas
                        .allowedOriginPatterns(
                            "http://localhost:*",
                            "http://host.docker.internal:*",
                            frontendUrl
                        )
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization", "Content-Type")
                        .allowCredentials(true)
                        .maxAge(3600);
            }

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
            }
        };
    }
}