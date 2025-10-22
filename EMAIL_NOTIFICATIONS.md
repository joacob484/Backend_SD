# Configuración de Notificaciones por Email

## 📧 Sistema de Notificaciones Implementado

Se ha implementado un sistema completo de notificaciones por email con las siguientes características:

### ✅ Funcionalidades

1. **Envío Asíncrono**: Los emails se envían en segundo plano sin bloquear las operaciones
2. **Preferencias Personalizables**: Cada usuario puede configurar qué notificaciones recibir
3. **Templates HTML**: Emails con diseño responsive y profesional
4. **Tipos de Notificación**:
   - 🎯 Invitaciones a partidos
   - 👥 Solicitudes de amistad
   - ⚽ Actualizaciones de partidos (cancelación, completado)
   - ⭐ Solicitudes de reseñas
   - 💬 Nuevos mensajes
   - 📢 Actualizaciones generales

### 🗄️ Cambios en Base de Datos

**Migración V7**: Agrega campos de preferencias al usuario
```sql
-- Columnas agregadas a tabla usuario
notif_email_invitaciones          BOOLEAN DEFAULT true
notif_email_solicitudes_amistad   BOOLEAN DEFAULT true
notif_email_actualizaciones_partido BOOLEAN DEFAULT true
notif_email_solicitudes_review    BOOLEAN DEFAULT true
notif_email_nuevos_mensajes       BOOLEAN DEFAULT false
notif_email_generales             BOOLEAN DEFAULT false
```

### ⚙️ Configuración Requerida

#### 1. Variables de Entorno

Agregar al archivo `.env`:

```bash
# EMAIL - SMTP Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu-email@gmail.com
MAIL_PASSWORD=tu-app-password-aqui
```

#### 2. Gmail - App Password

Para usar Gmail como proveedor SMTP:

1. Ir a [Google Account App Passwords](https://myaccount.google.com/apppasswords)
2. Seleccionar "App: Mail" y "Device: Other"
3. Copiar la contraseña generada (16 caracteres)
4. Usar esta contraseña en `MAIL_PASSWORD` (NO tu contraseña de Gmail)

#### 3. Otros Proveedores SMTP

**Outlook/Hotmail:**
```bash
MAIL_HOST=smtp.office365.com
MAIL_PORT=587
```

**Yahoo:**
```bash
MAIL_HOST=smtp.mail.yahoo.com
MAIL_PORT=587
```

**Custom SMTP:**
```bash
MAIL_HOST=tu-smtp-server.com
MAIL_PORT=587  # o 465 para SSL
```

### 📝 Nuevos Archivos

#### Backend

1. **EmailService.java**
   - Envío de emails con templates HTML
   - Lógica de verificación de preferencias
   - Email de bienvenida

2. **AsyncConfig.java**
   - Configuración de ejecución asíncrona
   - Thread pool para emails

3. **NotificationPreferencesDTO.java**
   - DTO para preferencias de notificación

4. **V7__add_notification_preferences.sql**
   - Migración de base de datos

#### Actualizaciones Backend

1. **Usuario.java**: Agregados campos de preferencias
2. **UsuarioService.java**: Métodos para obtener/actualizar preferencias
3. **UsuarioController.java**: Endpoints de preferencias
4. **NotificacionService.java**: Integración con EmailService
5. **application.yaml**: Configuración SMTP
6. **pom.xml**: Dependencia spring-boot-starter-mail

#### Frontend

1. **api.ts**: API de preferencias de notificación
2. **settings-screen.tsx**: Carga y guardado de preferencias

### 🎨 Diseño de Emails

Los emails incluyen:
- Header con gradiente de color según tipo de notificación
- Emoji identificador del tipo de notificación
- Contenido con fondo destacado
- Botón de acción (ver detalles)
- Footer con links a ayuda y configuración
- Diseño responsive para móviles

### 🔌 API Endpoints

```http
# Obtener preferencias
GET /api/usuarios/me/notification-preferences

# Actualizar preferencias
PUT /api/usuarios/me/notification-preferences
Content-Type: application/json

{
  "matchInvitations": true,
  "friendRequests": true,
  "matchUpdates": true,
  "reviewRequests": true,
  "newMessages": false,
  "generalUpdates": false
}
```

### 🧪 Testing

#### 1. Verificar Configuración SMTP

```bash
# En el backend, revisar logs al iniciar
# Debe mostrar: "JavaMailSenderImpl configured for smtp.gmail.com:587"
```

#### 2. Probar Envío de Email

1. Registrar un nuevo usuario
2. Crear una notificación (ej: invitar a partido)
3. Verificar logs:
   ```
   [EmailService] ✅ Email enviado a usuario@email.com: tipo=INVITACION_PARTIDO
   ```
4. Revisar bandeja de entrada del usuario

#### 3. Probar Preferencias

1. Ir a `/settings`
2. Desactivar "Invitaciones a partidos"
3. Guardar cambios
4. Crear invitación → No debe llegar email
5. Verificar logs: `Usuario tiene deshabilitadas las notificaciones de tipo INVITACION_PARTIDO`

### ⚠️ Troubleshooting

#### Error: Authentication failed

- **Causa**: Contraseña incorrecta o 2FA no configurado
- **Solución**: Usar App Password de Gmail, no contraseña normal

#### Error: Connection timeout

- **Causa**: Puerto bloqueado por firewall
- **Solución**: Verificar que puerto 587 esté abierto

#### Emails no llegan

1. Revisar logs del backend
2. Verificar carpeta de spam
3. Confirmar que preferencias están habilitadas
4. Verificar que `MAIL_USERNAME` sea correcto

#### Email va a spam

- Configurar SPF/DKIM en tu dominio (producción)
- Usar servicio profesional (SendGrid, AWS SES) en producción
- Gmail para desarrollo es aceptable

### 🚀 Recomendaciones para Producción

1. **Usar servicio de email profesional**:
   - [SendGrid](https://sendgrid.com) (12,000 emails/mes gratis)
   - [AWS SES](https://aws.amazon.com/ses/) (muy económico)
   - [Mailgun](https://www.mailgun.com)
   - [Postmark](https://postmarkapp.com)

2. **Configurar dominio personalizado**:
   - SPF record
   - DKIM signature
   - DMARC policy

3. **Monitoreo**:
   - Tasa de entrega
   - Tasa de apertura
   - Bounces y complaints

4. **Rate Limiting**:
   - Gmail: Max 500 emails/día (cuenta gratuita)
   - Gmail Workspace: Max 2000 emails/día
   - Servicios profesionales: 10,000+ emails/día

### 📊 Métricas de Notificación

Para agregar métricas (futuro):

```java
// En EmailService
private void registrarEnvio(Usuario usuario, TipoNotificacion tipo, boolean exitoso) {
    // Guardar en tabla de métricas
    // - Fecha/hora envío
    // - Tipo notificación
    // - Usuario
    // - Éxito/Error
}
```

### 🔄 Flujo Completo

1. **Usuario recibe notificación in-app** → Se crea registro en tabla `notificacion`
2. **NotificacionService** → Llama a `EmailService.enviarNotificacionEmail()`
3. **EmailService** → Verifica preferencias del usuario
4. **Si habilitado** → Construye HTML y envía email de forma asíncrona
5. **Usuario recibe email** → Click en "Ver detalles" → Redirige al frontend

### 📚 Referencias

- [Spring Boot Mail](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)
- [JavaMail API](https://javaee.github.io/javamail/)
- [Gmail SMTP](https://support.google.com/mail/answer/7126229)
- [Email Templates Best Practices](https://www.emailonacid.com/blog/article/email-development/)

---

**Implementado**: Octubre 2025  
**Versión**: 1.0.0  
**Migración DB**: V7
