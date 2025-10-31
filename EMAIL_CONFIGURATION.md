# 📧 Configuración del Sistema de Emails

## 🎯 Resumen

El sistema de emails de **Falta Uno** está completamente implementado y listo para usar. Solo necesitas configurar las credenciales de Gmail.

---

## ⚡ Configuración Rápida (Gmail)

### 1️⃣ Obtener App Password de Gmail

1. **Ir a tu cuenta de Google**: https://myaccount.google.com/
2. **Seguridad** → **Verificación en dos pasos** (activarla si no está activa)
3. **App Passwords** (Contraseñas de aplicaciones)
4. **Seleccionar app**: "Correo"
5. **Seleccionar dispositivo**: "Otro (nombre personalizado)" → "Falta Uno"
6. **Generar** → Copiar el password de 16 caracteres

### 2️⃣ Configurar Variables de Entorno

**Opción A - Variables de Entorno del Sistema (Recomendado para producción)**:

```bash
# Windows PowerShell
$env:MAIL_USERNAME="tu-email@gmail.com"
$env:MAIL_PASSWORD="xxxx xxxx xxxx xxxx"  # App password de 16 dígitos

# Linux/Mac
export MAIL_USERNAME="tu-email@gmail.com"
export MAIL_PASSWORD="xxxx xxxx xxxx xxxx"
```

**Opción B - Archivo .env (Para desarrollo local)**:

Crear archivo `.env` en la raíz del proyecto:

```env
# Email Configuration
MAIL_USERNAME=tu-email@gmail.com
MAIL_PASSWORD=xxxx xxxx xxxx xxxx
```

### 3️⃣ Verificar Configuración

Reinicia la aplicación y busca en los logs:

```
╔═══════════════════════════════════════════════════════════╗
║  📧 Sistema de Email ACTIVADO                             ║
║  Servidor: smtp.gmail.com                                 ║
║  Usuario: tu-email@gmail.com                              ║
║  Los usuarios recibirán notificaciones por email         ║
╚═══════════════════════════════════════════════════════════╝
```

---

## 🚀 Configuración en Cloud Run (Google Cloud)

### Método 1: Cloud Console (UI)

1. Ir a **Cloud Run** → Seleccionar tu servicio
2. **Edit & Deploy New Revision**
3. **Variables & Secrets** → **Add Variable**
4. Agregar:
   - `MAIL_USERNAME` = `tu-email@gmail.com`
   - `MAIL_PASSWORD` = `xxxx xxxx xxxx xxxx`
5. **Deploy**

### Método 2: gcloud CLI

```bash
gcloud run services update faltauno-backend \
  --update-env-vars MAIL_USERNAME=tu-email@gmail.com,MAIL_PASSWORD="xxxx xxxx xxxx xxxx" \
  --region=us-central1
```

### Método 3: Secrets Manager (Más Seguro - Recomendado)

```bash
# 1. Crear secret para el password
echo -n "xxxx xxxx xxxx xxxx" | gcloud secrets create mail-password --data-file=-

# 2. Dar acceso al service account
gcloud secrets add-iam-policy-binding mail-password \
  --member="serviceAccount:YOUR-PROJECT-NUMBER-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# 3. Actualizar Cloud Run para usar el secret
gcloud run services update faltauno-backend \
  --update-env-vars MAIL_USERNAME=tu-email@gmail.com \
  --update-secrets MAIL_PASSWORD=mail-password:latest \
  --region=us-central1
```

---

## 📝 Configuración Actual (application.yaml)

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME:}  # ← Configurar variable de entorno
    password: ${MAIL_PASSWORD:}  # ← Configurar variable de entorno
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
```

**Valores por defecto**:
- Si `MAIL_USERNAME` está vacío → Sistema de emails DESACTIVADO
- Si `MAIL_USERNAME` está configurado → Sistema de emails ACTIVADO

---

## ✅ Emails Soportados

### 1. Email de Verificación
- **Cuándo**: Al registrarse con email/password (no OAuth)
- **Contenido**: Código de 6 dígitos válido por 15 minutos
- **Template**: Profesional con timer y consejos de seguridad

### 2. Email de Bienvenida
- **Cuándo**: Al completar el registro exitosamente
- **Contenido**: Bienvenida + próximos pasos + CTA para completar perfil
- **Template**: Amigable y motivador

### 3. Notificaciones por Email
- ⚽ Invitaciones a partidos
- 👋 Solicitudes de amistad
- 🤝 Amistades aceptadas
- ✅ Inscripciones aceptadas
- 🚫 Partidos cancelados
- ⭐ Reviews pendientes
- 💬 Nuevos mensajes
- ⏰ Recordatorios de partidos
- Y más...

---

## 🎨 Personalización de Templates

Los templates de email están en `EmailService.java` y usan:

- **Colores dinámicos** según tipo de notificación
- **Emojis contextuales** para cada tipo
- **Gradientes profesionales** en header
- **Botones CTA** personalizados
- **Links a configuración** de preferencias
- **Diseño responsive** compatible con todos los clientes de email

---

## 🔧 Preferencias de Usuario

Los usuarios pueden configurar qué notificaciones recibir por email:

```typescript
// Frontend: /settings
{
  notifEmailInvitaciones: true/false,
  notifEmailSolicitudesAmistad: true/false,
  notifEmailActualizacionesPartido: true/false,
  notifEmailSolicitudesReview: true/false,
  notifEmailNuevosMensajes: true/false,
  notifEmailGenerales: true/false
}
```

El backend respeta estas preferencias automáticamente.

---

## 🐛 Troubleshooting

### Email no se envía

**Verificar**:
1. Variables `MAIL_USERNAME` y `MAIL_PASSWORD` configuradas
2. App Password de Gmail (no la contraseña normal)
3. Verificación en dos pasos activada en Gmail
4. Logs de la aplicación: `[EmailService] ✅ Email enviado...`

**Logs de error comunes**:
```
[EmailService] ❌ Error enviando email: Authentication failed
→ Verificar App Password correcto

[EmailService] 📭 Email no configurado. Saltando envío.
→ Configurar MAIL_USERNAME y MAIL_PASSWORD
```

### Gmail bloquea emails

Si Gmail bloquea los emails:
1. Ir a https://myaccount.google.com/lesssecureapps
2. **NO recomendado**: Permitir apps menos seguras
3. **Recomendado**: Usar App Passwords (ya configurado)

### Límite de envíos

Gmail tiene límite de **500 emails/día** por cuenta gratuita.

**Soluciones**:
- **Gmail Workspace**: 2000 emails/día por usuario
- **SendGrid**: 100 emails/día gratis, luego de pago
- **Amazon SES**: $0.10 por 1000 emails
- **Mailgun**: 5000 emails/mes gratis

---

## 🌐 Proveedores Alternativos

### SendGrid

```yaml
spring:
  mail:
    host: smtp.sendgrid.net
    port: 587
    username: apikey
    password: ${SENDGRID_API_KEY}
```

### Amazon SES

```yaml
spring:
  mail:
    host: email-smtp.us-east-1.amazonaws.com
    port: 587
    username: ${AWS_SES_USERNAME}
    password: ${AWS_SES_PASSWORD}
```

### Mailgun

```yaml
spring:
  mail:
    host: smtp.mailgun.org
    port: 587
    username: ${MAILGUN_USERNAME}
    password: ${MAILGUN_PASSWORD}
```

---

## 📊 Monitoreo

### Logs de Éxito
```
[EmailService] ✅ Email enviado a user@example.com: tipo=INVITACION_PARTIDO
[EmailService] ✅ Email de bienvenida enviado a user@example.com
[EmailService] ✅ Código de verificación enviado a user@example.com
```

### Logs de Error
```
[EmailService] ❌ Error enviando email a user@example.com: [motivo]
```

### Métricas Recomendadas
- Total de emails enviados
- Tasa de error de envío
- Tiempo promedio de envío
- Tipos de emails más enviados

---

## 🔐 Seguridad

### Mejores Prácticas

1. **NUNCA** commitear passwords en código
2. **Usar** App Passwords de Gmail (no contraseña normal)
3. **Usar** Secrets Manager en producción
4. **Rotar** passwords periódicamente
5. **Monitorear** actividad sospechosa en cuenta Gmail

### Variables de Entorno Seguras

```bash
# ✅ CORRECTO
MAIL_PASSWORD=xxxx xxxx xxxx xxxx

# ❌ INCORRECTO
spring.mail.password=mi-password-real  # En application.yaml
```

---

## 📚 Recursos

- **Gmail App Passwords**: https://support.google.com/accounts/answer/185833
- **Spring Boot Mail**: https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email
- **SendGrid Docs**: https://docs.sendgrid.com/
- **AWS SES**: https://docs.aws.amazon.com/ses/

---

## ✨ Estado Actual

- ✅ Sistema completamente implementado
- ✅ Templates profesionales
- ✅ Async (no bloquea operaciones)
- ✅ Manejo de errores robusto
- ✅ Logs detallados
- ✅ Respeta preferencias de usuario
- ⚠️ **Falta**: Configurar credenciales Gmail

**Siguiente paso**: Configurar `MAIL_USERNAME` y `MAIL_PASSWORD` en variables de entorno.
