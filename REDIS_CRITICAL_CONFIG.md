# 🚨 CONFIGURACIÓN CRÍTICA DE REDIS - NO TOCAR SIN LEER

## ⚠️ IMPORTANTE: LEER ANTES DE CUALQUIER DEPLOY

Este documento documenta la configuración **CRÍTICA** de Redis que **NO DEBE MODIFICARSE** sin extremo cuidado.

---

## 📍 IP de Memorystore Redis (PRODUCCIÓN)

```
IP: 10.217.135.172
Puerto: 6379
VPC Connector: run-connector
Región: us-central1
```

**⚠️ NUNCA cambiar manualmente las variables de entorno en Cloud Run**  
**⚠️ SIEMPRE usar cloudbuild-cloudrun.yaml como fuente de verdad**

---

## ✅ Configuración Correcta Actual

### 1. `cloudbuild-cloudrun.yaml` (Líneas 204-207)

```yaml
substitutions:
  _REDIS_HOST: "10.217.135.172"
  _REDIS_PORT: "6379"
```

### 2. Deployment Step (usa las substitutions)

```yaml
- name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
  id: 'deploy-to-cloud-run'
  entrypoint: 'bash'
  args:
    - '-c'
    - |
      # Variables de entorno para Cloud Run
      gcloud run deploy faltauno-backend \
        --set-env-vars="SPRING_REDIS_HOST=${_REDIS_HOST}" \
        --set-env-vars="SPRING_REDIS_PORT=${_REDIS_PORT}" \
        --set-env-vars="REDIS_HOST=${_REDIS_HOST}" \
        ...
```

### 3. `application.yml`

```yaml
spring:
  data:
    redis:
      host: ${SPRING_REDIS_HOST}
      port: ${SPRING_REDIS_PORT:6379}
      timeout: 2000ms
      connect-timeout: 5000ms
```

---

## 🔍 Cómo Validar Antes de Cada Deploy

### Opción 1: Script Automático (RECOMENDADO)

```bash
bash validate-redis-config.sh
```

Este script verifica:
- ✓ IP correcta en cloudbuild-cloudrun.yaml
- ✓ Variables de sustitución usadas correctamente
- ✓ application.yml usa variables de entorno
- ✓ Cloud Run tiene las env vars correctas
- ✓ IP de Memorystore coincide

### Opción 2: Verificación Manual

```bash
# 1. Verificar cloudbuild-cloudrun.yaml
grep "_REDIS_HOST" cloudbuild-cloudrun.yaml
# Debe mostrar: _REDIS_HOST: "10.217.135.172"

# 2. Verificar Cloud Run actual
gcloud run services describe faltauno-backend \
  --region us-central1 \
  --format="value(spec.template.spec.containers[0].env.filter(name:SPRING_REDIS_HOST).value)"
# Debe mostrar: 10.217.135.172

# 3. Verificar Memorystore
gcloud redis instances list --region us-central1
# Debe mostrar IP: 10.217.135.172
```

---

## 🚀 Proceso de Deploy Correcto

### SIEMPRE usar Cloud Build con substitutions:

```bash
cd Back/Backend_SD

# Opción 1: Validar primero (RECOMENDADO)
bash validate-redis-config.sh && \
gcloud builds submit \
  --config=cloudbuild-cloudrun.yaml \
  --substitutions=_REDIS_HOST="10.217.135.172",_REDIS_PORT="6379" \
  --async

# Opción 2: Deploy directo (si ya validaste)
gcloud builds submit \
  --config=cloudbuild-cloudrun.yaml \
  --substitutions=_REDIS_HOST="10.217.135.172",_REDIS_PORT="6379" \
  --async
```

### ❌ NUNCA hacer esto:

```bash
# ❌ NO actualizar manualmente las env vars
gcloud run services update faltauno-backend \
  --set-env-vars SPRING_REDIS_HOST=OTRA_IP

# ❌ NO deployar sin substitutions
gcloud builds submit --config=cloudbuild-cloudrun.yaml

# ❌ NO modificar env vars desde la consola web de Cloud Run
```

---

## 🔧 Troubleshooting

### Problema: "Connection refused" o "Timeout" a Redis

**Causa común:** IP incorrecta en variables de entorno

**Solución:**
```bash
# 1. Verificar IP actual de Memorystore
gcloud redis instances describe faltauno-redis \
  --region us-central1 \
  --format="value(host)"

# 2. Verificar IP en Cloud Run
gcloud run services describe faltauno-backend \
  --region us-central1 \
  --format="value(spec.template.spec.containers[0].env.filter(name:SPRING_REDIS_HOST).value)"

# 3. Si no coinciden, re-deploy con Cloud Build
gcloud builds submit \
  --config=cloudbuild-cloudrun.yaml \
  --substitutions=_REDIS_HOST="10.217.135.172",_REDIS_PORT="6379" \
  --async
```

### Problema: Redis IP cambió (Memorystore recreado)

**Pasos a seguir:**
1. Obtener nueva IP:
   ```bash
   NEW_IP=$(gcloud redis instances describe faltauno-redis \
     --region us-central1 \
     --format="value(host)")
   echo "Nueva IP: $NEW_IP"
   ```

2. Actualizar `cloudbuild-cloudrun.yaml`:
   ```yaml
   substitutions:
     _REDIS_HOST: "NUEVA_IP_AQUI"
   ```

3. Commitear cambio:
   ```bash
   git add cloudbuild-cloudrun.yaml
   git commit -m "fix: actualizar IP de Redis a $NEW_IP"
   git push
   ```

4. Re-deploy:
   ```bash
   gcloud builds submit \
     --config=cloudbuild-cloudrun.yaml \
     --substitutions=_REDIS_HOST="$NEW_IP",_REDIS_PORT="6379" \
     --async
   ```

---

## 📊 Logs y Monitoreo

### Ver logs de conexión Redis en Cloud Run:

```bash
# Logs de los últimos 10 minutos
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=faltauno-backend AND textPayload=~\"Redis\"" \
  --limit 50 \
  --format json \
  --freshness=10m
```

### Verificar health de Redis:

```bash
# Obtener URL del servicio
SERVICE_URL=$(gcloud run services describe faltauno-backend \
  --region us-central1 \
  --format='value(status.url)')

# Verificar health endpoint
curl "$SERVICE_URL/actuator/health" | jq '.components.redis'
```

---

## 🎯 Checklist Pre-Deploy

Antes de cada deploy, verificar:

- [ ] IP de Redis es `10.217.135.172` en `cloudbuild-cloudrun.yaml`
- [ ] Puerto de Redis es `6379` en `cloudbuild-cloudrun.yaml`
- [ ] Variables de sustitución se usan en el deployment step
- [ ] `application.yml` usa `${SPRING_REDIS_HOST}` (no hardcoded)
- [ ] Ejecutar `bash validate-redis-config.sh` pasa sin errores
- [ ] NO hay cambios manuales pendientes en Cloud Run

---

## 📞 Soporte

Si encuentras problemas:

1. **Ejecutar validación:** `bash validate-redis-config.sh`
2. **Revisar logs:** Ver sección "Logs y Monitoreo"
3. **Verificar Memorystore:** `gcloud redis instances list --region us-central1`
4. **Rollback si es necesario:**
   ```bash
   # Ver revisiones
   gcloud run revisions list --service faltauno-backend --region us-central1
   
   # Rollback a revisión anterior
   gcloud run services update-traffic faltauno-backend \
     --to-revisions=faltauno-backend-REVISION=100 \
     --region us-central1
   ```

---

**Última actualización:** 2025-10-31  
**Autor:** Sistema  
**Estado:** PRODUCCIÓN - NO MODIFICAR SIN VALIDACIÓN
