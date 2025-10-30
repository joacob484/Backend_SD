# 🚀 Configuración de Google Cloud - Falta Uno

Este documento detalla los recursos de Google Cloud necesarios y cómo configurarlos.

## 📊 Estado Actual de la Infraestructura

### ✅ Recursos Configurados

1. **Cloud SQL (PostgreSQL)**
   - Instancia: `faltauno-db`
   - Connection: `master-might-274420:us-central1:faltauno-db`
   - Base de datos: `faltauno_db`
   - Usuario: `app`
   - ✅ Funciona correctamente

2. **Cloud Run - Backend**
   - Service: `faltauno-backend`
   - Región: `us-central1`
   - Service Account: `169771742214-compute@developer.gserviceaccount.com`
   - ⚠️ Falla al iniciar (sin configuración completa)

3. **Cloud Run - Frontend**
   - Service: `faltauno-frontend`
   - URL: `https://faltauno-frontend-169771742214.us-central1.run.app`
   - ✅ Funciona

4. **Redis (Memorystore)**
   - IP: `10.128.0.2`
   - Puerto: `6379`
   - ✅ Configurado (verificar si está activo)

### ❌ Recursos Faltantes o Sin Configurar

## 🔧 Configuración Requerida

### 1. Pub/Sub (Opcional pero Recomendado)

Si quieres usar Pub/Sub para eventos (reemplaza RabbitMQ):

#### Crear Topic
```bash
gcloud pubsub topics create faltauno-events \
  --project=master-might-274420
```

#### Crear Subscription
```bash
gcloud pubsub subscriptions create faltauno-events-sub \
  --topic=faltauno-events \
  --project=master-might-274420 \
  --ack-deadline=60
```

#### Dar permisos al Service Account
```bash
# Permiso para publicar
gcloud projects add-iam-policy-binding master-might-274420 \
  --member="serviceAccount:169771742214-compute@developer.gserviceaccount.com" \
  --role="roles/pubsub.publisher"

# Permiso para suscribirse
gcloud projects add-iam-policy-binding master-might-274420 \
  --member="serviceAccount:169771742214-compute@developer.gserviceaccount.com" \
  --role="roles/pubsub.subscriber"
```

#### Habilitar Pub/Sub en el deployment

En `.github/workflows/deploy.yml`, cambiar:
```yaml
GCP_PUBSUB_ENABLED: "true"  # Cambiar de false a true
```

### 2. Verificar Redis (Memorystore)

#### Listar instancias de Redis
```bash
gcloud redis instances list \
  --region=us-central1 \
  --project=master-might-274420
```

Si no existe, crear una:
```bash
gcloud redis instances create faltauno-redis \
  --size=1 \
  --region=us-central1 \
  --redis-version=redis_7_0 \
  --tier=basic \
  --project=master-might-274420
```

**Importante:** Actualizar la IP de Redis en `.github/workflows/deploy.yml`:
```yaml
SPRING_REDIS_HOST: [LA_IP_QUE_TE_DIO_GCLOUD]
```

### 3. VPC Connector (Para conectar Cloud Run con servicios internos)

Verificar si existe:
```bash
gcloud compute networks vpc-access connectors list \
  --region=us-central1 \
  --project=master-might-274420
```

Si no existe `run-vpc`, crear uno:
```bash
gcloud compute networks vpc-access connectors create run-vpc \
  --region=us-central1 \
  --range=10.8.0.0/28 \
  --network=default \
  --project=master-might-274420
```

### 4. Secrets en Secret Manager

Verificar que existan los secrets necesarios:
```bash
gcloud secrets list --project=master-might-274420
```

Debe existir `db-password`. Si no existe:
```bash
echo -n "TU_PASSWORD_DE_DB" | gcloud secrets create db-password \
  --data-file=- \
  --project=master-might-274420

# Dar permiso al service account
gcloud secrets add-iam-policy-binding db-password \
  --member="serviceAccount:169771742214-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor" \
  --project=master-might-274420
```

## 🔍 Diagnóstico de Problemas Actuales

### Verificar estado del backend
```bash
gcloud run services describe faltauno-backend \
  --region=us-central1 \
  --project=master-might-274420
```

### Ver logs del backend
```bash
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=faltauno-backend" \
  --limit=50 \
  --project=master-might-274420 \
  --format=json
```

### Ver revisiones del backend
```bash
gcloud run revisions list \
  --service=faltauno-backend \
  --region=us-central1 \
  --project=master-might-274420
```

### Revisar última revisión fallida
```bash
gcloud run revisions describe [REVISION_NAME] \
  --region=us-central1 \
  --project=master-might-274420
```

## 📝 Variables de Entorno Configuradas en Cloud Run

El backend necesita estas variables (ya configuradas en el script de deploy):

### Obligatorias
- `SPRING_DATASOURCE_URL` - URL de conexión a Cloud SQL
- `SPRING_DATASOURCE_USERNAME` - Usuario de DB (app)
- `SPRING_DATASOURCE_PASSWORD` - Password de DB (desde Secret Manager)
- `SPRING_REDIS_HOST` - IP de Memorystore
- `SERVER_PORT` - Puerto (8080)
- `JWT_SECRET` - Secret para JWT (debe estar en GitHub Secrets)
- `GOOGLE_CLIENT_ID` - Para OAuth Google
- `GOOGLE_CLIENT_SECRET` - Para OAuth Google

### Opcionales (Pub/Sub)
- `GCP_PUBSUB_ENABLED` - true/false
- `GCP_PUBSUB_TOPIC` - Nombre del topic
- `GCP_PUBSUB_SUBSCRIPTION` - Nombre de la subscription
- `GCP_PROJECT_ID` - ID del proyecto

## 🚨 Problemas Conocidos y Soluciones

### 1. Container fails to start
**Problema:** El contenedor no escucha en el puerto 8080

**Causas posibles:**
- Redis no accesible (falta VPC connector)
- Pub/Sub mal configurado (habilitado sin topic/subscription)
- Falta alguna variable de entorno crítica

**Solución:**
1. Deshabilitar Pub/Sub temporalmente: `GCP_PUBSUB_ENABLED: "false"`
2. Verificar VPC connector
3. Verificar Redis está accesible desde Cloud Run

### 2. ClassCastException en cache
**Problema:** Redis devuelve LinkedHashMap en vez de PartidoDTO

**Solución:** Ya arreglado con cache version `partidos_v3` (solo necesitas deployar)

### 3. OAuth Google no funciona
**Problema:** Las variables `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET` no están configuradas

**Solución:** Agregar en GitHub Secrets y actualizar el workflow

## 🎯 Checklist de Deployment

- [ ] Redis (Memorystore) creado y accesible
- [ ] VPC Connector creado
- [ ] Cloud SQL funcionando
- [ ] Secret Manager con `db-password`
- [ ] Service Account con permisos:
  - [ ] Cloud SQL Client
  - [ ] Secret Manager Secret Accessor
  - [ ] Pub/Sub Publisher (si se usa)
  - [ ] Pub/Sub Subscriber (si se usa)
- [ ] Pub/Sub (opcional):
  - [ ] Topic `faltauno-events` creado
  - [ ] Subscription `faltauno-events-sub` creada
  - [ ] Permisos configurados
- [ ] GitHub Secrets configurados:
  - [ ] `SPRING_DATASOURCE_PASSWORD`
  - [ ] `JWT_SECRET`
  - [ ] `GOOGLE_CLIENT_ID`
  - [ ] `GOOGLE_CLIENT_SECRET`

## 🔄 Próximos Pasos Recomendados

1. **Inmediato:** Deshabilitar Pub/Sub (`GCP_PUBSUB_ENABLED: "false"`) para que el backend arranque
2. **Corto plazo:** Verificar Redis está accesible con VPC connector
3. **Medio plazo:** Configurar Pub/Sub correctamente si lo necesitas
4. **Largo plazo:** Migrar secrets de GitHub Actions a Secret Manager

---

**Última actualización:** 29 de octubre de 2025
**Mantenedor:** Backend Team
