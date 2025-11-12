# Configuración de Twilio para SMS

## 📱 Paso 1: Crear Cuenta en Twilio

1. Ir a https://www.twilio.com/try-twilio
2. Crear cuenta gratuita (incluye $15 USD de crédito)
3. Verificar email y número de teléfono

## 🔑 Paso 2: Obtener Credenciales

1. En el dashboard de Twilio: https://console.twilio.com/
2. Copiar:
   - **Account SID**: ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   - **Auth Token**: (click en "Show" para ver)

## 📞 Paso 3: Obtener Número de Teléfono

1. En Twilio Console → Phone Numbers → Buy a Number
2. Seleccionar país (Uruguay: +598)
3. Comprar número con capacidad **SMS**
4. Copiar el número (formato: +598XXXXXXXX)

**NOTA**: Con cuenta de prueba, solo puedes enviar SMS a números verificados en Twilio.

## 🔐 Paso 4: Configurar Variables de Entorno

### Desarrollo Local (.env)

Crear archivo `.env` en la raíz del proyecto:

```bash
# SMS Configuration
SMS_ENABLED=true
SMS_PROVIDER=twilio

# Twilio Credentials
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token_here
TWILIO_FROM_NUMBER=+598XXXXXXXX
```

### Producción (Cloud Run)

En Google Cloud Console:

```bash
# Configurar variables de entorno en Cloud Run
gcloud run services update faltauno-backend \
  --region=us-central1 \
  --set-env-vars=SMS_ENABLED=true,SMS_PROVIDER=twilio,TWILIO_ACCOUNT_SID=ACxxx,TWILIO_AUTH_TOKEN=xxx,TWILIO_FROM_NUMBER=+598xxx
```

O desde la consola web:
1. Cloud Run → faltauno-backend → Edit & Deploy New Revision
2. Variables & Secrets → Add Variable:
   - `SMS_ENABLED` = `true`
   - `SMS_PROVIDER` = `twilio`
   - `TWILIO_ACCOUNT_SID` = tu Account SID
   - `TWILIO_AUTH_TOKEN` = tu Auth Token
   - `TWILIO_FROM_NUMBER` = tu número Twilio

## ✅ Paso 5: Verificar Números (Cuenta Gratuita)

Con cuenta gratuita de Twilio, solo puedes enviar SMS a números verificados:

1. Twilio Console → Phone Numbers → Verified Caller IDs
2. Click "+" para agregar número
3. Ingresar número de celular (+598XXXXXXXXX)
4. Twilio enviará código de verificación
5. Ingresar código para verificar

**Números verificados**: Puedes enviar SMS de prueba sin costo.

## 🚀 Paso 6: Probar

### Desde la aplicación:

1. Ir a `/phone-verification`
2. Click "Enviar Código"
3. Revisar celular (o logs si está en modo console)
4. Ingresar código de 6 dígitos
5. Verificar

### Logs esperados:

```
[SMS] ✅ Twilio inicializado correctamente
[SMS] 📱 Twilio - Enviando a +598XXXXXXXXX: Tu código de verificación de Falta Uno es: 123456. Válido por 15 minutos.
[SMS] ✅ Twilio - SMS enviado exitosamente. SID: SMxxxxxxxxxx, Status: queued
```

## 💰 Costos

### Cuenta Gratuita
- $15 USD de crédito inicial
- ~500-1000 SMS gratis
- Solo a números verificados

### Cuenta de Producción
- Uruguay: ~$0.05 USD por SMS
- Argentina: ~$0.03 USD por SMS
- USA: ~$0.0079 USD por SMS

**Cálculo**: 1000 usuarios × 1 verificación = ~$50 USD/mes (Uruguay)

## 🔄 Upgrade a Cuenta Paga

Cuando necesites enviar a números no verificados:

1. Twilio Console → Billing
2. Upgrade Your Account
3. Agregar método de pago
4. ¡Listo! Ya puedes enviar a cualquier número

## 🛡️ Seguridad

**Variables sensibles (NUNCA commitear):**
- ❌ `TWILIO_AUTH_TOKEN` 
- ❌ `TWILIO_ACCOUNT_SID`

**Buenas prácticas:**
- ✅ Usar variables de entorno
- ✅ Rotar Auth Token periódicamente
- ✅ Habilitar autenticación 2FA en Twilio
- ✅ Monitorear uso en Twilio Console

## 📊 Monitoreo

Ver SMS enviados:
1. Twilio Console → Monitor → Logs → Messaging
2. Ver estado de cada SMS (delivered, failed, etc.)
3. Costos acumulados en Usage

## 🧪 Modo Desarrollo (Sin Twilio)

Si no quieres configurar Twilio aún:

```bash
# .env
SMS_ENABLED=false  # Los códigos solo aparecen en logs
SMS_PROVIDER=console
```

Los códigos se mostrarán en los logs del servidor:
```
[SMS] 📱 MODO DESARROLLO - SMS a +598XXXXXXXXX: Tu código de verificación de Falta Uno es: 123456...
```

## 🆘 Troubleshooting

### Error: "TWILIO_ACCOUNT_SID no configurado"
→ Verificar que las variables de entorno estén configuradas correctamente

### Error: "Authentication Error"
→ Verificar que `TWILIO_AUTH_TOKEN` sea correcto

### SMS no llega
→ Verificar que el número esté verificado (cuenta gratuita)
→ Verificar formato del número: debe ser internacional (+598...)

### Error 21211: "Invalid 'To' Phone Number"
→ Número destino en formato incorrecto (debe ser +XXXXXXXXXXX)

### Error 21608: "Unverified number"
→ Con cuenta gratuita, verificar número en Twilio Console primero

## 📚 Recursos

- [Twilio Docs - Enviar SMS](https://www.twilio.com/docs/sms/send-messages)
- [Twilio Java SDK](https://www.twilio.com/docs/libraries/java)
- [Pricing](https://www.twilio.com/en-us/sms/pricing)
- [Errores Comunes](https://www.twilio.com/docs/api/errors)
