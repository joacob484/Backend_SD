package uy.um.faltauno.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Performance Monitoring Aspect
 * 
 * Monitorea automáticamente el tiempo de ejecución de métodos de servicio
 * Solo activo cuando performance.monitoring.enabled=true
 */
@Aspect
@Component
@ConditionalOnProperty(name = "performance.monitoring.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class PerformanceMonitoringAspect {

    /**
     * Monitorear métodos públicos de servicios
     */
    @Around("execution(public * uy.um.faltauno.service..*(..))")
    public Object monitorServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        long start = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            
            // Solo loguear si es lento (>500ms)
            if (duration > 500) {
                log.warn("⚠️ SLOW: {}.{} took {}ms", className, methodName, duration);
            } else if (log.isDebugEnabled()) {
                log.debug("✅ {}.{} took {}ms", className, methodName, duration);
            }
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("❌ {}.{} failed after {}ms: {}", className, methodName, duration, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Monitorear métodos de controller para requests HTTP
     */
    @Around("execution(public * uy.um.faltauno.controller..*(..))")
    public Object monitorControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        long start = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            
            // Loguear requests lentos
            if (duration > 1000) {
                log.warn("⚠️ SLOW HTTP: {}.{} took {}ms", className, methodName, duration);
            } else if (duration > 500) {
                log.info("📊 {}.{} took {}ms", className, methodName, duration);
            }
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("❌ HTTP ERROR: {}.{} failed after {}ms", className, methodName, duration);
            throw e;
        }
    }
}
