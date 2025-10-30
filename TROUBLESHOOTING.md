# 🔧 Guía Rápida de Troubleshooting

## Si el deployment falla...

### 1️⃣ Ver logs de GitHub Actions
```bash
# Ir a: https://github.com/joacob484/Backend_SD/actions
# Click en el workflow más reciente
# Ver los detalles del error
```

### 2️⃣ Ver logs de Cloud Build
```bash
# Si tienes gcloud instalado:
gcloud builds list --limit=5 --project=master-might-274420

# Ver detalles del último build:
gcloud builds log [BUILD_ID] --project=master-might-274420
```

### 3️⃣ Ver logs de Cloud Run
```bash
# Logs de la última revisión:
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=faltauno-backend" \
  --limit=50 \
  --project=master-might-274420 \
  --format=json
```

### 4️⃣ Verificar variables de entorno
```bash
gcloud run services describe faltauno-backend \
  --region=us-central1 \
  --project=master-might-274420 \
  --format=yaml > service-config.yaml

# Revisar la sección 'env' en service-config.yaml
cat service-config.yaml | grep -A 20 "env:"
```

## Errores Comunes y Soluciones

### ❌ "Container failed to start"

**Posibles causas:**
1. Redis no accesible → Verificar VPC Connector
2. Pub/Sub habilitado sin recursos → Ya está deshabilitado
3. Variable de entorno faltante → Verificar GitHub Secrets

**Solución rápida:**
```bash
# Rollback a la revisión anterior que funcionaba:
gcloud run services update-traffic faltauno-backend \
  --to-revisions=faltauno-backend-00090-scn=100 \
  --region=us-central1 \
  --project=master-might-274420
```

### ❌ "Service account does not have permission"

**Solución:**
```bash
# Ver permisos actuales:
gcloud projects get-iam-policy master-might-274420 \
  --flatten="bindings[].members" \
  --filter="bindings.members:169771742214-compute@developer.gserviceaccount.com"

# Agregar permisos necesarios:
gcloud projects add-iam-policy-binding master-might-274420 \
  --member="serviceAccount:169771742214-compute@developer.gserviceaccount.com" \
  --role="roles/cloudsql.client"
```

### ❌ "Cannot connect to Redis"

**Verificar:**
1. Redis está corriendo
2. VPC Connector configurado
3. IP correcta en variables de entorno

**Solución:**
```bash
# Listar instancias Redis:
gcloud redis instances list --region=us-central1 --project=master-might-274420

# Verificar VPC Connector:
gcloud compute networks vpc-access connectors list \
  --region=us-central1 \
  --project=master-might-274420
```

### ❌ "Health check failed"

**Verificar:**
```bash
# Probar health endpoint:
curl -v https://faltauno-backend-169771742214.us-central1.run.app/actuator/health

# Ver configuración de health check en Cloud Run:
gcloud run services describe faltauno-backend \
  --region=us-central1 \
  --project=master-might-274420 \
  --format='get(spec.template.spec.containers[0].livenessProbe)'
```

## 🚨 Rollback de Emergencia

Si nada funciona, hacer rollback:

```bash
# Ver revisiones disponibles:
gcloud run revisions list \
  --service=faltauno-backend \
  --region=us-central1 \
  --project=master-might-274420

# Rollback a la última revisión que funcionaba:
gcloud run services update-traffic faltauno-backend \
  --to-revisions=[REVISION_NAME]=100 \
  --region=us-central1 \
  --project=master-might-274420
```

## 📞 Comandos Útiles de Diagnóstico

```bash
# Estado del servicio:
./check-deployment.sh

# Logs en tiempo real:
gcloud logging tail "resource.type=cloud_run_revision AND resource.labels.service_name=faltauno-backend" \
  --project=master-might-274420

# Probar endpoint:
curl https://faltauno-backend-169771742214.us-central1.run.app/actuator/health

# Ver todas las revisiones:
gcloud run revisions list \
  --service=faltauno-backend \
  --region=us-central1 \
  --project=master-might-274420

# Describir servicio completo:
gcloud run services describe faltauno-backend \
  --region=us-central1 \
  --project=master-might-274420
```

## 🎯 Checklist Post-Deploy

- [ ] Servicio está "Ready"
- [ ] Health check responde 200
- [ ] Logs no muestran errores
- [ ] Frontend puede conectarse al backend
- [ ] Redis funciona (cache)
- [ ] PostgreSQL funciona (queries)
- [ ] OAuth Google funciona (si configurado)

## 📱 Contactos de Emergencia

Si después de 10 minutos el deploy sigue fallando:
1. Ejecutar `./check-deployment.sh`
2. Copiar los logs de error
3. Revisar `GOOGLE_CLOUD_SETUP.md`
4. Considerar rollback temporal
