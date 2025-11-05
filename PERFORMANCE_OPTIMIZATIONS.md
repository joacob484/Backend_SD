# 🚀 Backend Performance Optimizations

## Aplicadas

### ✅ 1. Database Connection Pool (Hikari)
**Archivo:** `application.yaml`
```yaml
hikari:
  maximum-pool-size: 10  # Cloud Run tiene recursos limitados
  minimum-idle: 2
  connection-timeout: 20000
  idle-timeout: 300000
  max-lifetime: 1200000  # Renovar conexiones cada 20 min
  leak-detection-threshold: 60000
```

### ✅ 2. Hibernate Batch Processing
**Archivo:** `application.yaml`
```yaml
hibernate:
  jdbc:
    batch_size: 30  # Agrupar inserts/updates
    fetch_size: 50  # Reducir round-trips
  order_inserts: true
  order_updates: true
```

### ✅ 3. Query Plan Caching
**Archivo:** `application.yaml`
```yaml
hibernate:
  query:
    in_clause_parameter_padding: true
    plan_cache_max_size: 2048
    plan_parameter_metadata_max_size: 256
```

### ✅ 4. Caffeine Cache
**Archivo:** `application.yaml`
```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=10m,recordStats=true
```

### ✅ 5. JOIN FETCH Optimization
**Archivo:** `PartidoRepository.java`
- `findAllWithOrganizador()` - Evita N+1 queries
- `findByIdWithOrganizador()` - Single query con join
- `findByOrganizadorIdWithOrganizador()` - Optimizado

### ✅ 6. Lazy Loading Strategy
**Archivo:** `Partido.java`, `Inscripcion.java`, etc.
```java
@ManyToOne(fetch = FetchType.LAZY)
private Usuario organizador;
```

### ✅ 7. Optimistic Locking
**Archivo:** `Partido.java`
```java
@Version
private Long version;
```

## 🎯 Pendientes de Implementar

### 1. **@EntityGraph para eliminar ALL N+1 queries**
```java
@EntityGraph(attributePaths = {"organizador", "inscripciones"})
@Query("SELECT p FROM Partido p WHERE p.estado = :estado")
List<Partido> findByEstadoWithDetails(@Param("estado") String estado);
```

### 2. **Índices de Base de Datos**
Crear migration SQL:
```sql
-- Índices para búsquedas frecuentes
CREATE INDEX idx_partido_fecha ON partido(fecha);
CREATE INDEX idx_partido_estado ON partido(estado);
CREATE INDEX idx_partido_organizador ON partido(organizador_id);
CREATE INDEX idx_inscripcion_usuario ON inscripcion(usuario_id);
CREATE INDEX idx_inscripcion_partido ON inscripcion(partido_id);
CREATE INDEX idx_notificacion_usuario ON notificacion(usuario_id);

-- Índice compuesto para búsquedas combinadas
CREATE INDEX idx_partido_estado_fecha ON partido(estado, fecha);
```

### 3. **Paginación en Endpoints**
```java
@GetMapping("/partidos")
public Page<PartidoDTO> listarPartidos(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
    return partidoService.findAll(pageable);
}
```

### 4. **DTO Projections (reducir datos transferidos)**
```java
public interface PartidoSummaryProjection {
    UUID getId();
    String getNombreUbicacion();
    LocalDate getFecha();
    String getEstado();
}

@Query("SELECT p.id as id, p.nombreUbicacion as nombreUbicacion, p.fecha as fecha, p.estado as estado FROM Partido p")
List<PartidoSummaryProjection> findAllSummaries();
```

### 5. **Async Processing para operaciones pesadas**
```java
@Async
@Transactional
public CompletableFuture<Void> enviarNotificacionesAsync(UUID partidoId) {
    // Procesar en background
    return CompletableFuture.completedFuture(null);
}
```

### 6. **Response Compression**
Ya configurado en `application.yaml`:
```yaml
server:
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html
```

### 7. **Cache @Cacheable en Services**
```java
@Cacheable(value = "partidos", key = "#id")
public PartidoDTO obtenerPartido(UUID id) {
    // ...
}

@CacheEvict(value = "partidos", key = "#id")
public void actualizarPartido(UUID id, PartidoDTO dto) {
    // Invalida cache al actualizar
}
```

### 8. **Batch Loading con DataLoader**
Para GraphQL o REST con múltiples IDs:
```java
public Map<UUID, Usuario> batchLoadUsuarios(List<UUID> ids) {
    return usuarioRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(Usuario::getId, Function.identity()));
}
```

### 9. **Read Replicas (futuro)**
Separar lecturas de escrituras cuando escale:
```yaml
spring:
  datasource:
    hikari:
      read-only: true  # Para réplica de lectura
```

### 10. **Connection Pool Monitoring**
Agregar actuator metrics:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

## 📊 Métricas de Performance

### Objetivos:
- ✅ **API Response Time**: < 200ms (p95)
- ✅ **Database Query Time**: < 50ms (p95)
- ✅ **Connection Pool Usage**: < 70%
- ✅ **Cache Hit Ratio**: > 80%

### Monitoreo:
```bash
# Ver métricas Hikari
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# Ver cache stats
curl http://localhost:8080/actuator/metrics/cache.gets
```
