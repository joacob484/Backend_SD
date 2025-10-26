# 🔐 Configuración de Google OAuth

## ❌ **Error actual: "Error de autenticación"**

### **Posibles causas:**

1. **Falta configurar variables de entorno** en Cloud Run
2. **URL de redirect mal configurada** en Google Cloud Console
3. **Frontend URL incorrecta** en el backend

---

## ✅ **Solución paso a paso:**

### **1. Configurar Google Cloud Console**

#### **a) Crear credenciales OAuth 2.0:**
1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Selecciona tu proyecto
3. Ve a **APIs & Services** > **Credentials**
4. Click en **Create Credentials** > **OAuth 2.0 Client ID**
5. Tipo de aplicación: **Web application**
6. Nombre: `FaltaUno OAuth`

#### **b) Configurar URIs autorizadas:**

**JavaScript origins autorizados:**
```
https://faltauno-backend-169771742214.us-central1.run.app
http://localhost:8080
```

**URIs de redireccionamiento autorizadas:**
```
https://faltauno-backend-169771742214.us-central1.run.app/login/oauth2/code/google
http://localhost:8080/login/oauth2/code/google
```

#### **c) Copiar credenciales:**
- Copia el **Client ID**
- Copia el **Client Secret**

---

### **2. Configurar variables de entorno en Cloud Run (Backend)**

#### **Opción A: Desde consola web**
1. Ve a [Cloud Run Console](https://console.cloud.google.com/run)
2. Click en tu servicio **faltauno-backend**
3. Click en **EDIT & DEPLOY NEW REVISION**
4. En la sección **Variables & Secrets**, agrega:

```bash
GOOGLE_CLIENT_ID=tu-client-id-aqui.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=tu-client-secret-aqui
FRONTEND_URL=https://faltauno-frontend-169771742214.us-central1.run.app
```

5. Click en **DEPLOY**

#### **Opción B: Desde gcloud CLI**
```bash
gcloud run services update faltauno-backend \
  --region=us-central1 \
  --update-env-vars GOOGLE_CLIENT_ID=tu-client-id,GOOGLE_CLIENT_SECRET=tu-secret,FRONTEND_URL=https://faltauno-frontend-169771742214.us-central1.run.app
```

---

### **3. Para desarrollo local**

Crea un archivo `.env` en la raíz del backend:

```bash
# Backend_SD/.env
GOOGLE_CLIENT_ID=tu-client-id-aqui
GOOGLE_CLIENT_SECRET=tu-client-secret-aqui
FRONTEND_URL=http://localhost:3000
```

**⚠️ IMPORTANTE:** Agrega `.env` al `.gitignore` para no subir las credenciales

---

## 🧪 **Probar la configuración:**

### **1. Verificar variables en Cloud Run:**
```bash
gcloud run services describe faltauno-backend \
  --region=us-central1 \
  --format="value(spec.template.spec.containers[0].env)"
```

### **2. Probar login con Google:**
1. Ve a: `https://faltauno-frontend-169771742214.us-central1.run.app/login`
2. Click en "Iniciar con Google"
3. Deberías ser redirigido a Google
4. Después de autorizar, vuelves a `/oauth/success?token=...`

---

## 🔍 **Debugging:**

### **Ver logs en Cloud Run:**
```bash
gcloud run services logs read faltauno-backend \
  --region=us-central1 \
  --limit=50
```

### **Logs importantes:**
- `OAuth2SuccessHandler` - Cuando Google redirige exitosamente
- `JwtUtil.generateToken` - Cuando se genera el token
- Errores de autenticación

### **Verificar que el backend esté recibiendo la request:**
```bash
# Simular request de Google
curl "https://faltauno-backend-169771742214.us-central1.run.app/oauth2/authorization/google"
```

Debería redirigir a Google OAuth.

---

## 📝 **Checklist de verificación:**

- [ ] GOOGLE_CLIENT_ID configurado en Cloud Run
- [ ] GOOGLE_CLIENT_SECRET configurado en Cloud Run  
- [ ] FRONTEND_URL configurado en Cloud Run
- [ ] URIs de redirect configuradas en Google Console
- [ ] Backend desplegado con las variables
- [ ] Probar login con Google en producción

---

## ❓ **Errores comunes:**

### **Error: "redirect_uri_mismatch"**
- La URL de redirect en Google Console no coincide
- Verifica que sea exactamente: `https://faltauno-backend-169771742214.us-central1.run.app/login/oauth2/code/google`

### **Error: "invalid_client"**
- CLIENT_ID o CLIENT_SECRET incorrectos
- Verifica las variables de entorno

### **Error: "Error de autenticación" en frontend**
- Backend no está redirigiendo correctamente
- Verifica FRONTEND_URL en las variables de entorno

---

## 🚀 **Próximos pasos:**

Una vez configurado OAuth:
1. Los usuarios podrán registrarse/login con Google
2. No necesitarán verificar email
3. Irán directo a profile-setup (si es nuevo usuario)
4. O a home (si ya tienen perfil completo)
