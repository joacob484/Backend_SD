# 🚀 Deployment Automático a Cloud Run

Este proyecto usa GitHub Actions para deployment automático en cada push a `main`.

## Configuración Inicial (Una sola vez)

### 1. Crear Service Account Key

```bash
# Crear service account si no existe
gcloud iam service-accounts create github-deploy \
  --display-name="GitHub Deploy SA" \
  --project=master-might-274420

# Dar permisos necesarios
gcloud projects add-iam-policy-binding master-might-274420 \
  --member="serviceAccount:github-deploy@master-might-274420.iam.gserviceaccount.com" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding master-might-274420 \
  --member="serviceAccount:github-deploy@master-might-274420.iam.gserviceaccount.com" \
  --role="roles/cloudbuild.builds.builder"

gcloud projects add-iam-policy-binding master-might-274420 \
  --member="serviceAccount:github-deploy@master-might-274420.iam.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"

# Crear key JSON
gcloud iam service-accounts keys create github-sa-key.json \
  --iam-account=github-deploy@master-might-274420.iam.gserviceaccount.com \
  --project=master-might-274420
```

### 2. Agregar Secret a GitHub

1. Ir a: https://github.com/joacob484/Backend_SD/settings/secrets/actions
2. Click en **"New repository secret"**
3. Name: `GCP_SA_KEY`
4. Value: Copiar todo el contenido del archivo `github-sa-key.json`
5. Click en **"Add secret"**

### 3. ⚠️ Importante

Después de agregar el secret, **ELIMINAR** el archivo `github-sa-key.json` de tu máquina:

```bash
Remove-Item github-sa-key.json
```

## ✅ Uso

Una vez configurado, el deployment es automático:

```bash
# 1. Hacer cambios
git add .
git commit -m "tu mensaje"
git push

# 2. GitHub Actions automáticamente:
#    - Hace checkout del código
#    - Se autentica con Google Cloud
#    - Ejecuta: gcloud run deploy faltauno-backend --source=.
#    - Muestra la URL del servicio
```

## 📊 Monitorear Deployment

Ver el progreso en:
- GitHub Actions: https://github.com/joacob484/Backend_SD/actions
- Cloud Run Console: https://console.cloud.google.com/run?project=master-might-274420

## 🔧 Troubleshooting

### Error: "secrets.GCP_SA_KEY not found"
→ Debes agregar el secret `GCP_SA_KEY` en GitHub (ver paso 2 arriba)

### Error: "Permission denied"
→ Verificar que el service account tiene los roles correctos (paso 1)

### Build tarda mucho
→ Normal, el primer build con `--source=.` toma ~12-15 minutos

## 🎯 Deployment Manual (opcional)

Si quieres hacer deployment manual:

```bash
cd "c:\Users\augus\Desktop\Falta Uno\Back\Backend_SD"
gcloud run deploy faltauno-backend --source=. --region=us-central1 --allow-unauthenticated --memory=2Gi --timeout=600
```
