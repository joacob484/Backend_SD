# 🔵🟢 Zero-Downtime Deployment

Este proyecto implementa un sistema de **deployment automático con zero-downtime** usando la estrategia Blue-Green.

## 🎯 Cómo Funciona

### Flujo Automático (GitHub Actions)

Cada vez que haces `git push` a `main`:

1. **GitHub Actions se triggera automáticamente**
2. **SSH a la VM** (`faltauno-vm`)
3. **Git pull** del código más reciente
4. **Ejecuta `deploy-zero-downtime.sh`**

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
- `faltauno_rabbitmq` - Same RabbitMQ as BLUE  
- `faltauno_postgres` - Same Postgres as BLUE

**Sin necesidad de duplicar infraestructura.**

## �🔧 Scripts

### `deploy-zero-downtime.sh`

Script principal que implementa Blue-Green:

```bash
cd ~/Backend_SD
bash deploy-zero-downtime.sh
```

**Características:**
- ✅ Builds GREEN mientras BLUE sirve tráfico
- ✅ Health checks con timeout de 3 minutos
- ✅ Rollback automático si GREEN falla
- ✅ Logs detallados en caso de error
- ✅ Cleanup de containers huérfanos

### `deploy.sh` (Simple Restart)

Script alternativo para desarrollo (CON downtime):

```bash
cd ~/Backend_SD
bash deploy.sh
```

**Uso:** Solo para desarrollo/testing rápido.

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
- Se conecta a Redis/RabbitMQ/Postgres ya corriendo
- No levanta su propia infraestructura

### docker-compose.prod.yml

Define todos los servicios de producción:

```yaml
services:
  backend:    # Port 8080
  redis:      # Port 6379
  rabbitmq:   # Port 5672
  postgres:   # Port 5432
```

## 🚀 Deployment Manual

Si necesitas hacer deployment manual (sin GitHub Actions):

```bash
# SSH a la VM
gcloud compute ssh augus@faltauno-vm \
  --zone=us-central1-a \
  --project=master-might-274420

# Ir al directorio
cd ~/Backend_SD

# Pull código
git pull

# Ejecutar deployment
bash deploy-zero-downtime.sh
```

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
