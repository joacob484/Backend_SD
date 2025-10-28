# 🔵🟢 Zero-Downtime Deployment

Este proyecto implementa un sistema de **deployment automático con zero-downtime** usando la estrategia Blue-Green.

## 🎯 Cómo Funciona

### Flujo Automático (GitHub Actions)

Cada vez que haces `git push` a `main` el flujo se ejecuta en CI (GitHub Actions) y despliega usando las configuraciones declarativas (`.github/workflows/deploy.yml` o `cloudbuild-*` si usas Cloud Build). Las antiguas instrucciones que ejecutaban `deploy-zero-downtime.sh` localmente fueron removidas; la recomendación es dejar que CI haga el despliegue para garantizar reproducibilidad y permisos adecuados.

### Proceso Blue-Green (Sin Downtime)

```
┌─────────────────────────────────────────────────────────┐
│  Antes del Deployment                                   │
│  ┌──────────────┐                                       │
│  │  BLUE (v1)   │ ◄── Sirviendo en 8080                 │
│  │  Container   │                                        │
│  └──────────────┘                                       │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Step 1: Build GREEN                                     │
│  ┌──────────────┐     ┌──────────────┐                  │
│  │  BLUE (v1)   │     │  GREEN (v2)  │                  │
│  │  Serving     │     │  Building... │                  │
│  └──────────────┘     └──────────────┘                  │
│         ▲                                                │
│         └─── Tráfico en 8080 ✅                          │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Step 2: Start GREEN on 8081                             │
│  ┌──────────────┐     ┌──────────────┐                  │
│  │  BLUE (v1)   │     │  GREEN (v2)  │                  │
│  │  Port 8080   │     │  Port 8081   │                  │
│  └──────────────┘     └──────────────┘                  │
│         ▲                     ▲                          │
│         │                     └─── Health checks...      │
│         └─── Tráfico en 8080 ✅                          │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Step 3: Wait for GREEN to be healthy                    │
│  ┌──────────────┐     ┌──────────────┐                  │
│  │  BLUE (v1)   │     │  GREEN (v2)  │ ✅ HEALTHY       │
│  │  Port 8080   │     │  Port 8081   │                  │
│  └──────────────┘     └──────────────┘                  │
│         ▲                                                │
│         └─── Tráfico en 8080 ✅                          │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Step 4: Switch GREEN to 8080                            │
│  ┌──────────────┐     ┌──────────────┐                  │
│  │  BLUE (v1)   │     │  GREEN (v2)  │                  │
│  │  Stopping... │     │  Port 8080   │ ◄── New traffic  │
│  └──────────────┘     └──────────────┘                  │
│                              ▲                           │
│                              └─── Tráfico ahora aquí ✅  │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Step 5: Remove BLUE                                     │
│                       ┌──────────────┐                   │
│                       │  GREEN (v2)  │                   │
│                       │  Port 8080   │ ◄── Serving       │
│                       └──────────────┘                   │
│                              ▲                           │
│                              └─── Todo el tráfico ✅     │
└─────────────────────────────────────────────────────────┘
```

### ⏱️ Timeline

- **T+0s**: Push a main
- **T+10s**: GitHub Actions inicia
- **T+20s**: SSH conectado, git pull completo
- **T+30s**: Docker build de GREEN inicia
- **T+90s**: GREEN container starting
- **T+180s**: GREEN healthy en 8081
- **T+185s**: Switch: GREEN → 8080, BLUE stops
- **T+190s**: Deployment completo ✅

**Downtime total: 0 segundos** 🎉

## � Key Implementation Details

### Image Reuse Strategy

El secreto del zero-downtime es **construir y verificar la imagen UNA sola vez**, luego reutilizarla:

```bash
# Step 1: Build GREEN image
docker-compose -f docker-compose.bluegreen.yml build backend-green

# Step 2: Tag the image for reuse
docker tag backend_sd-backend-green:latest backend_sd-backend:latest

# Step 3: Start GREEN on 8081 and verify health
docker-compose -f docker-compose.bluegreen.yml up -d backend-green
# ... health checks ...

# Step 4: Stop GREEN container (but keep the IMAGE)
docker stop backend-green
docker rm backend-green

# Step 5: Start the SAME image on 8080 using prod compose
# docker-compose.prod.yml uses: image: backend_sd-backend:latest
docker-compose -f docker-compose.prod.yml up -d backend
```

**Crucial:** `docker-compose.prod.yml` usa `image:` en lugar de `build:`, garantizando que usa **exactamente la misma imagen** que acabamos de verificar como GREEN.

### Network Strategy

GREEN se conecta a la **misma infraestructura** que BLUE:

```yaml
networks:
  faltauno-network:
    external: true  # Uses existing network
    name: backend_sd_faltauno-network
```

Esto permite que GREEN acceda a:
- `faltauno_redis` - Same Redis as BLUE
- `faltauno_postgres` - Same Postgres as BLUE

**Sin necesidad de duplicar infraestructura.**

## �🔧 Scripts

> Note: legacy local deploy scripts were removed from the repository. To perform deployments use the CI workflows mentioned above or build and run images locally for testing.

If you need a one-off manual deployment from a VM, follow the 'Deployment Manual' section below but use the GitHub Actions/Cloud Build approaches where possible. Creating new local scripts is fine but please document them and add tests/guardrails before use.

## 📋 Configuración

### docker-compose.bluegreen.yml

Define el container GREEN que se levanta temporalmente en 8081:

```yaml
services:
  backend-green:
    build:
      context: .
      dockerfile: Dockerfile.prod
    container_name: backend-green
    ports:
      - "8081:8080"
    networks:
      - faltauno-network  # Externa - ya existe
```

**Importante:**
- Usa la red `backend_sd_faltauno-network` existente
- Se conecta a Redis/Postgres ya corriendo
- No levanta su propia infraestructura

### docker-compose.prod.yml

Define todos los servicios de producción:

```yaml
services:
  backend:    # Port 8080
  redis:      # Port 6379
  postgres:   # Port 5432
```

## 🚀 Deployment Manual (deprecated)

Manual deployments that relied on local scripts were deprecated. Prefer CI-driven deployments. If you must perform an ad-hoc deploy from a VM, build and tag the image locally or use the Cloud Build `retag` pipeline, then deploy with `gcloud run deploy` as shown in `deploy/README.md`.

## 🔍 Monitoreo

### Durante el Deployment

Puedes monitorear en tiempo real:

```bash
# Terminal 1: Ver containers
watch -n 1 'docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"'

# Terminal 2: Ver logs de GREEN
docker logs -f backend-green

# Terminal 3: Health checks
watch -n 1 'curl -s http://localhost:8081/actuator/health/readiness | jq'
```

### Después del Deployment

```bash
# Ver estado
docker-compose -f docker-compose.prod.yml ps

# Ver logs
docker logs faltauno_backend --tail 100

# Health check
curl http://localhost:8080/actuator/health/readiness
```

## ❌ Rollback

Si GREEN falla, el script hace rollback automático:

1. Detecta que GREEN no pasa health check después de 3 minutos
2. Muestra logs de GREEN para debugging
3. Para y elimina GREEN
4. BLUE sigue sirviendo tráfico → **Sin downtime**

Rollback manual (si necesario):

```bash
# Si GREEN está en 8080 y quieres volver a BLUE anterior
docker stop faltauno_backend
docker rm faltauno_backend

# Levantar versión anterior
git checkout <commit-anterior>
docker-compose -f docker-compose.prod.yml up -d backend
```

## 🎯 Beneficios

✅ **Zero Downtime**: Usuarios no ven interrupciones  
✅ **Safe Rollback**: Si falla, BLUE sigue corriendo  
✅ **Automated**: Push y olvídate  
✅ **Fast**: ~3 minutos total  
✅ **Monitored**: Health checks automáticos  
✅ **Debuggable**: Logs detallados en caso de fallo

## 📊 Comparación

| Feature | `deploy.sh` | `deploy-zero-downtime.sh` |
|---------|-------------|---------------------------|
| Downtime | ⚠️ ~60s | ✅ 0s |
| Safety | ⚠️ Medium | ✅ High |
| Speed | ✅ 2 min | ⚠️ 3 min |
| Rollback | ❌ Manual | ✅ Automatic |
| Use Case | Development | Production |

## 🔗 Links

- **GitHub Actions**: https://github.com/joacob484/Backend_SD/actions
- **Workflow**: `.github/workflows/deploy-backend.yml`
- **VM**: `faltauno-vm` (us-central1-a)
- **Backend**: http://34.45.130.122:8080
- **Health**: http://34.45.130.122:8080/actuator/health
