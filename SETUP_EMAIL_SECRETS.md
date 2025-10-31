# 📧 Script para Configurar Secrets de Email en Google Cloud

## ⚡ Configuración Rápida

### 1️⃣ Obtener App Password de Gmail

1. Ir a: https://myaccount.google.com/security
2. **Verificación en dos pasos** → Activar si no está activa
3. **App Passwords** (buscar en la página)
4. Seleccionar:
   - **App**: Correo
   - **Dispositivo**: Otro (nombre personalizado) → "Falta Uno"
5. **Generar** → Copiar el password de 16 caracteres (formato: `xxxx xxxx xxxx xxxx`)

---

### 2️⃣ Crear Secrets en Google Cloud

**Opción A: Cloud Console (UI)** ✨ RECOMENDADO PARA PRINCIPIANTES

1. Ir a: https://console.cloud.google.com/security/secret-manager
2. Seleccionar proyecto: `master-might-274420`
3. **CREATE SECRET**

**Secret 1: mail-username**
- Name: `mail-username`
- Secret value: `tu-email@gmail.com` (sin comillas)
- Click **CREATE SECRET**

**Secret 2: mail-password**
- Name: `mail-password`  
- Secret value: `xxxx xxxx xxxx xxxx` (tu App Password de 16 dígitos)
- Click **CREATE SECRET**

4. **Dar permisos** al Service Account:
   - Para cada secret creado:
   - **PERMISSIONS** tab
   - **GRANT ACCESS**
   - Principal: `169771742214-compute@developer.gserviceaccount.com`
   - Role: `Secret Manager Secret Accessor`
   - **SAVE**

---

**Opción B: gcloud CLI** 🚀 RECOMENDADO PARA AVANZADOS

```powershell
# 1. Configurar proyecto
gcloud config set project master-might-274420

# 2. Crear secret para username
echo -n "tu-email@gmail.com" | gcloud secrets create mail-username --data-file=-

# 3. Crear secret para password (REEMPLAZAR con tu App Password)
echo -n "xxxx xxxx xxxx xxxx" | gcloud secrets create mail-password --data-file=-

# 4. Dar permisos al Service Account para ambos secrets
gcloud secrets add-iam-policy-binding mail-username `
  --member="serviceAccount:169771742214-compute@developer.gserviceaccount.com" `
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding mail-password `
  --member="serviceAccount:169771742214-compute@developer.gserviceaccount.com" `
  --role="roles/secretmanager.secretAccessor"

# 5. Verificar que los secrets existen
gcloud secrets list --filter="name:mail-*"
```

---

### 3️⃣ Hacer Deploy con Emails Configurados

```powershell
cd "c:\Users\augus\Desktop\Falta Uno\Back\Backend_SD"

# Deploy automático (usa los secrets configurados)
gcloud builds submit --config=cloudbuild-cloudrun.yaml --project=master-might-274420 --async
```

---

### 4️⃣ Verificar que Funciona

Después del deploy, revisar logs:

```powershell
# Ver logs de Cloud Run
gcloud run services logs read faltauno-backend --region=us-central1 --limit=50

# Buscar línea de confirmación:
# ╔═══════════════════════════════════════════════════════════╗
# ║  📧 Sistema de Email ACTIVADO                             ║
# ║  Servidor: smtp.gmail.com                                 ║
# ║  Usuario: tu-email@gmail.com                              ║
# ╚═══════════════════════════════════════════════════════════╝
```

---

## 🔄 Actualizar Secrets (Cambiar Password)

Si necesitas cambiar el email o password:

```powershell
# Actualizar username
echo -n "nuevo-email@gmail.com" | gcloud secrets versions add mail-username --data-file=-

# Actualizar password
echo -n "nuevo xxxx xxxx xxxx xxxx" | gcloud secrets versions add mail-password --data-file=-

# Hacer nuevo deploy para aplicar cambios
gcloud builds submit --config=cloudbuild-cloudrun.yaml --project=master-might-274420 --async
```

---

## 🐛 Troubleshooting

### Secret "mail-username" no existe

```powershell
# Crear el secret
echo -n "tu-email@gmail.com" | gcloud secrets create mail-username --data-file=-

# Dar permisos
gcloud secrets add-iam-policy-binding mail-username `
  --member="serviceAccount:169771742214-compute@developer.gserviceaccount.com" `
  --role="roles/secretmanager.secretAccessor"
```

### Error de permisos

```powershell
# Verificar permisos del secret
gcloud secrets get-iam-policy mail-username

# Debe mostrar:
# bindings:
# - members:
#   - serviceAccount:169771742214-compute@developer.gserviceaccount.com
#   role: roles/secretmanager.secretAccessor
```

### Gmail bloquea emails

1. Verificar que usas **App Password** (no la contraseña normal)
2. Verificar que **Verificación en dos pasos** está activada
3. Probar enviar email de prueba desde la app

---

## 📊 Verificar Configuración Actual

```powershell
# Ver todos los secrets del proyecto
gcloud secrets list

# Ver última versión de mail-username
gcloud secrets versions access latest --secret=mail-username

# Ver metadata de mail-password (NO muestra el valor)
gcloud secrets describe mail-password
```

---

## 🔐 Seguridad

### ✅ BUENAS PRÁCTICAS

- ✅ Usar **App Passwords** de Gmail
- ✅ Guardar passwords en **Secret Manager**
- ✅ **NUNCA** commitear passwords en código
- ✅ Rotar passwords cada 6 meses
- ✅ Usar email dedicado para la app (no personal)

### ❌ EVITAR

- ❌ Usar contraseña normal de Gmail
- ❌ Guardar passwords en variables de entorno en texto plano
- ❌ Commitear credentials en Git
- ❌ Compartir App Passwords

---

## 💰 Costos

- **Secret Manager**: $0.06 por 10,000 accesos/mes
- **Gmail**: Gratis hasta 500 emails/día
- **Costo estimado**: ~$0.50/mes para emails de la app

---

## 🎯 Resumen de Comandos

```powershell
# 1. Crear secrets
echo -n "tu-email@gmail.com" | gcloud secrets create mail-username --data-file=-
echo -n "xxxx xxxx xxxx xxxx" | gcloud secrets create mail-password --data-file=-

# 2. Dar permisos
gcloud secrets add-iam-policy-binding mail-username --member="serviceAccount:169771742214-compute@developer.gserviceaccount.com" --role="roles/secretmanager.secretAccessor"
gcloud secrets add-iam-policy-binding mail-password --member="serviceAccount:169771742214-compute@developer.gserviceaccount.com" --role="roles/secretmanager.secretAccessor"

# 3. Deploy
cd "c:\Users\augus\Desktop\Falta Uno\Back\Backend_SD"
gcloud builds submit --config=cloudbuild-cloudrun.yaml --project=master-might-274420 --async

# 4. Verificar logs
gcloud run services logs read faltauno-backend --region=us-central1 --limit=50
```

---

## 📚 Referencias

- **App Passwords**: https://support.google.com/accounts/answer/185833
- **Secret Manager**: https://cloud.google.com/secret-manager/docs
- **Cloud Run Secrets**: https://cloud.google.com/run/docs/configuring/secrets

---

✅ Una vez configurados los secrets, el sistema de emails funcionará automáticamente!
