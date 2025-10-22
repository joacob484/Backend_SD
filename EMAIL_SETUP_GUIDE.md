# 🚀 Guía de Activación de Notificaciones por Email

## ✅ Estado Actual

El sistema de notificaciones por email está **completamente implementado** pero **desactivado por defecto**. La aplicación funciona normalmente sin configuración de email.

### Qué funciona SIN configurar email:
- ✅ Todas las notificaciones in-app (base de datos)
- ✅ Gestión de preferencias de usuario en Settings
- ✅ Endpoints de API para preferencias
- ✅ Frontend con toggles de notificaciones
- ✅ Toda la lógica de negocio
- ✅ Migración V7 aplicada (campos en BD)

### Qué se activa AL configurar email:
- 📧 Envío de emails automáticos cuando ocurren eventos
- 📧 Email de bienvenida al registrarse
- 📧 Emails respetando las preferencias del usuario

---

## 📝 Cómo Activar el Email (Cuando lo necesites)

### Paso 1: Obtener App Password de Gmail

1. Ir a [Google App Passwords](https://myaccount.google.com/apppasswords)
2. Iniciar sesión con tu cuenta de Gmail
3. Seleccionar:
   - **App**: Mail
   - **Device**: Other (custom name) → "Falta Uno Backend"
4. Click en **Generate**
5. Copiar la contraseña de 16 caracteres (ej: `abcd efgh ijkl mnop`)

### Paso 2: Configurar .env

Editar el archivo `/Back/Backend_SD/.env`:

```bash
# Descomentar y configurar estas líneas:
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu-email@gmail.com
MAIL_PASSWORD=abcdefghijklmnop  # Sin espacios, los 16 caracteres
```

### Paso 3: Reiniciar Contenedores

```bash
cd "C:\Users\augus\Desktop\Falta Uno\Back\Backend_SD"
docker compose down
docker compose up -d
docker compose logs -f backend  # Ver logs
```

### Paso 4: Verificar Activación

En los logs del backend deberías ver:
```
[EmailService] Sistema de email ACTIVADO - Listo para enviar notificaciones
```

Si no está configurado, verás:
```
[EmailService] Sistema de email DESACTIVADO - No se enviarán emails
```

---

## 🧪 Probar el Sistema

### 1. Registrar nuevo usuario
```bash
# POST http://localhost:8080/api/usuarios
{
  "email": "test@example.com",
  "password": "password123"
}
```
✅ **Resultado**: Email de bienvenida enviado (si está configurado)

### 2. Crear invitación a partido
```bash
# POST http://localhost:8080/api/partidos/{id}/invitar
{
  "usuarioId": "uuid-del-usuario"
}
```
✅ **Resultado**: Email de invitación enviado (si el usuario tiene habilitada la opción)

### 3. Verificar preferencias
```bash
# GET http://localhost:8080/api/usuarios/me/notification-preferences
```
✅ **Resultado**: Ver qué notificaciones tiene activas el usuario

---

## 🔧 Usar Otro Proveedor de Email

### Outlook/Hotmail
```bash
MAIL_HOST=smtp-mail.outlook.com
MAIL_PORT=587
MAIL_USERNAME=tu-email@outlook.com
MAIL_PASSWORD=tu-password
```

### Yahoo
```bash
MAIL_HOST=smtp.mail.yahoo.com
MAIL_PORT=587
MAIL_USERNAME=tu-email@yahoo.com
MAIL_PASSWORD=tu-app-password
```

### SendGrid (Recomendado para producción)
```bash
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=tu-sendgrid-api-key
```

### AWS SES (Recomendado para producción)
```bash
MAIL_HOST=email-smtp.us-east-1.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=tu-smtp-username
MAIL_PASSWORD=tu-smtp-password
```

---

## 📊 Tipos de Notificaciones por Email

| Tipo | Habilitado por Defecto | Campo en BD |
|------|------------------------|-------------|
| 🎯 Invitaciones a partidos | ✅ Sí | `notif_email_invitaciones` |
| 👥 Solicitudes de amistad | ✅ Sí | `notif_email_solicitudes_amistad` |
| 📅 Actualizaciones de partidos | ✅ Sí | `notif_email_actualizaciones_partido` |
| ⭐ Solicitudes de reseñas | ✅ Sí | `notif_email_solicitudes_review` |
| 💬 Nuevos mensajes | ❌ No | `notif_email_nuevos_mensajes` |
| 📢 Actualizaciones generales | ❌ No | `notif_email_generales` |

---

## 🎨 Personalizar Templates de Email

Los templates están en `EmailService.java`. Para personalizarlos:

1. Editar método `construirCuerpoEmail()`
2. Modificar colores en `obtenerColorPrincipal()`
3. Cambiar emojis en `obtenerEmoji()`
4. Actualizar estructura HTML según necesites

---

## ⚠️ Limitaciones por Proveedor

### Gmail (Cuenta Personal)
- **Max emails/día**: 500
- **Max destinatarios/email**: 100
- **Recomendado para**: Desarrollo y pruebas

### Gmail Workspace
- **Max emails/día**: 2,000
- **Max destinatarios/email**: 2,000
- **Recomendado para**: Pequeñas empresas

### SendGrid (Free)
- **Max emails/día**: 100 (gratis)
- **Max emails/mes**: 40,000 (plan $19.95/mes)
- **Recomendado para**: Producción

### AWS SES
- **Max emails/día**: 200 (free tier)
- **Costo**: $0.10 por 1,000 emails
- **Recomendado para**: Producción escalable

---

## 🐛 Troubleshooting

### "Authentication failed"
❌ **Problema**: Contraseña incorrecta  
✅ **Solución**: Usar App Password, no la contraseña de Gmail

### "Connection timeout"
❌ **Problema**: Puerto bloqueado  
✅ **Solución**: Verificar firewall permite puerto 587

### Emails van a spam
❌ **Problema**: Sin configuración SPF/DKIM  
✅ **Solución**: 
- En desarrollo: Aceptable
- En producción: Configurar dominio propio con SPF/DKIM

### No llegan emails
❌ **Problema**: Preferencias deshabilitadas  
✅ **Solución**: Verificar en Settings que el usuario tiene activa la notificación

---

## 📚 Archivos del Sistema

### Backend
```
service/
├── EmailService.java          # Lógica de envío de emails
├── NotificacionService.java   # Integra email con notificaciones
└── UsuarioService.java        # Email de bienvenida

config/
└── AsyncConfig.java           # Ejecución asíncrona

dto/
└── NotificationPreferencesDTO.java

controller/
└── UsuarioController.java     # Endpoints de preferencias

entity/
└── Usuario.java               # Campos de preferencias

resources/
├── application.yaml           # Config SMTP
└── db/migration/
    └── V7__add_notification_preferences.sql
```

### Frontend
```
lib/
└── api.ts                     # NotificationPreferencesAPI

components/pages/user/
└── settings-screen.tsx        # UI de preferencias
```

---

## 🚀 Roadmap Futuro

### Features Opcionales
- [ ] Dashboard de métricas de email (tasa de apertura, etc.)
- [ ] Programar envío de emails (batch jobs)
- [ ] Templates personalizables desde admin
- [ ] A/B testing de templates
- [ ] Desuscripción de emails (unsubscribe link)
- [ ] Preview de email antes de enviar
- [ ] Emails multilengua

### Mejoras de Producción
- [ ] Rate limiting por usuario
- [ ] Queue con RabbitMQ para emails
- [ ] Retry automático en caso de fallo
- [ ] Almacenar historial de emails enviados
- [ ] Webhook para eventos de email (bounces, opens)

---

## 📞 Soporte

Si necesitas ayuda para configurar:
1. Revisar logs en `docker compose logs -f backend`
2. Verificar variables en `.env`
3. Comprobar que App Password es correcta
4. Verificar que puerto 587 está abierto

---

**Estado**: ✅ Listo para activar cuando lo necesites  
**Última actualización**: Octubre 2025
