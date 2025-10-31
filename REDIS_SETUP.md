# Redis Configuration for Cloud Run

## 📋 Overview

Redis está configurado usando **Google Cloud Memorystore** con acceso privado a través del VPC Connector.

## 🔧 Configuración Actual

### Redis Instance
- **IP Address**: `10.217.135.172`
- **Port**: `6379`
- **Network**: VPC con `run-connector`
- **Version**: Redis 7.x

### Variables de Entorno (Cloud Run)
```yaml
SPRING_REDIS_HOST: 10.217.135.172
SPRING_REDIS_PORT: 6379
```

### Application Configuration (application.yaml)
```yaml
spring:
  redis:
    host: ${SPRING_REDIS_HOST:localhost}
    port: ${SPRING_REDIS_PORT:6379}
    timeout: 2000ms
    connect-timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 2
      shutdown-timeout: 100ms

  cache:
    type: redis
    redis:
      cache-null-values: false
      time-to-live: 600s  # 10 minutos
```

## 🎯 Uso en la Aplicación

### Caché de Usuarios
```java
@Service
@Slf4j
public class UsuarioService {
    
    @Cacheable(value = "usuarios", key = "#usuarioId")
    public UsuarioDTO obtenerPorId(UUID usuarioId) {
        // Cachea usuarios por 10 minutos
    }
    
    @CacheEvict(value = "usuarios", key = "#usuarioId")
    public void subirFoto(UUID usuarioId, MultipartFile file) {
        // Invalida caché cuando se actualiza foto
    }
}
```

### Health Checks
Redis NO está incluido en el readiness probe porque es opcional para el funcionamiento básico:

```yaml
management:
  health:
    redis:
      enabled: true  # Monitorea Redis pero no bloquea
  endpoint:
    health:
      group:
        readiness:
          include: db,readinessState  # Redis NO es crítico
```

## 🚀 Deployment

El despliegue automático incluye:

1. **VPC Connector**: `run-connector` para acceso a Memorystore
2. **Environment Variables**: `SPRING_REDIS_HOST` y `SPRING_REDIS_PORT`
3. **Network Egress**: `private-ranges-only` (tráfico interno VPC)

## 🔍 Troubleshooting

### Error: "Unable to connect to Redis"

**Causa**: VPC Connector no configurado o IP incorrecta

**Solución**:
```bash
# Verificar Memorystore instance
gcloud redis instances describe faltauno-redis --region=us-central1

# Verificar VPC Connector
gcloud compute networks vpc-access connectors describe run-connector --region=us-central1

# Actualizar variables de entorno en Cloud Run
gcloud run services update faltauno-backend \
  --region=us-central1 \
  --set-env-vars="SPRING_REDIS_HOST=10.217.135.172,SPRING_REDIS_PORT=6379"
```

### Error: "Connection timeout"

**Causa**: Firewall o network policy bloqueando tráfico

**Solución**:
```bash
# Verificar firewall rules
gcloud compute firewall-rules list --filter="name:redis OR name:memorystore"

# Verificar que Cloud Run tenga vpc-egress configurado
gcloud run services describe faltauno-backend --region=us-central1 --format="value(spec.template.spec.containers[0].env)"
```

## 📊 Monitoring

### Logs de Conexión
```bash
# Ver logs de Redis connection
gcloud logging read "resource.type=cloud_run_revision AND textPayload=~\"redis|Redis|REDIS\"" --limit=50 --format=json
```

### Métricas de Memorystore
```bash
# Ver uso de memoria de Redis
gcloud redis instances describe faltauno-redis --region=us-central1 --format="value(currentLocationId,memorySizeGb,persistenceConfig)"
```

## 🔐 Security

- **No passwords**: Memorystore en VPC privado no requiere AUTH
- **Network isolation**: Acceso solo a través de VPC connector
- **Encryption**: En tránsito y en reposo (default de Memorystore)

## 📝 Notes

- Redis **NO** es crítico para el funcionamiento de la app
- Si Redis falla, el cache simplemente no se usa
- Los health checks no bloquean startup si Redis está down
- Timeout de conexión: 2 segundos (fail-fast)
