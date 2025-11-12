# 🆓 Configuración de Termii - SMS Gratuito Sin Restricciones

## ¿Por qué Termii?

✅ **10 SMS gratis al mes** (sin tarjeta de crédito)  
✅ **Envío a CUALQUIER número** (sin verificación previa)  
✅ **Cobertura global** (incluyendo Uruguay, Argentina, Brasil, etc.)  
✅ **API simple** y bien documentada  
✅ **Sin "trial account"** en los mensajes  

---

## 📝 Paso 1: Crear cuenta en Termii

1. **Ir a**: https://termii.com/
2. **Click en "Get Started"** o "Sign Up"
3. **Completar registro**:
   - Nombre
   - Email
   - Contraseña
   - Teléfono (opcional)
4. **Verificar email** (revisa spam/promociones)
5. **Login**: https://accounts.termii.com/login

---

## 🔑 Paso 2: Obtener API Key

1. **Login** en https://accounts.termii.com/
2. **Dashboard** → Menú lateral
3. **Settings** → **API Settings**
4. **Copy API Key** (formato: `TLxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`)
5. **Guardar** en lugar seguro (lo usarás en `.env.local`)

---

## 📱 Paso 3: Configurar Sender ID (Opcional)

El **Sender ID** es el nombre que aparece como remitente del SMS.

### Opción A: Usar genérico (RECOMENDADO para empezar)
- Default: `FaltaUno` (ya configurado)
- **No requiere aprobación**
- Funciona inmediatamente

### Opción B: Sender ID personalizado
1. Dashboard → **Sender ID**
2. **Request Sender ID**
3. Llenar formulario:
   - Company name: Falta Uno
   - Purpose: User verification
   - Sample message: "Tu código de verificación de Falta Uno es: 123456"
4. **Esperar aprobación** (1-2 días hábiles)

> **NOTA**: Para Uruguay/América, el Sender ID genérico funciona bien. No es necesario personalizar.

---

## ⚙️ Paso 4: Configurar Backend

### Actualizar `.env.local`

```bash
# SMS Configuration - TERMII (GRATIS)
SMS_ENABLED=true
SMS_PROVIDER=termii

# Termii Credentials
TERMII_API_KEY=TLxxxxxxxxxxxxxxxxxxxxxxxxxxxxx  # ← Pegar tu API Key aquí
TERMII_SENDER_ID=FaltaUno

# Phone Verification
PHONE_VERIFICATION_ENABLED=true
```

### Verificar configuración

Correr el backend y buscar en logs:

```
[SMS] ✅ Termii configurado - Sender ID: FaltaUno
[SMS] ℹ️ Termii Plan Gratuito: 10 SMS/mes sin verificación
```

---

## 🧪 Paso 5: Probar Envío

### Opción A: Desde la aplicación

1. Backend corriendo con `.env.local` configurado
2. Frontend: navegar a `/phone-verification`
3. Ingresar **tu número real** (ej: +598 XXX XXX XXX)
4. Click "Enviar código de verificación"
5. **Esperar 5-30 segundos** (Termii puede tardar un poco)
6. **Revisar celular** - debe llegar SMS con código de 6 dígitos

### Opción B: Desde Termii Dashboard (testing)

1. Dashboard → **Messaging** → **Send SMS**
2. To: +598XXXXXXXXX (tu número)
3. Message: "Test desde Termii"
4. Channel: Generic
5. **Send**
6. Verificar que llega al celular

---

## 📊 Monitoreo y Uso

### Ver SMS enviados

1. Dashboard → **Messaging** → **SMS History**
2. Ver:
   - Message ID
   - Destination number
   - Status (sent, delivered, failed)
   - Timestamp
   - Cost

### Ver balance/créditos

1. Dashboard → **Billing** → **Account Balance**
2. Plan gratuito: muestra "10 free SMS/month"
3. Ver cuántos SMS quedan este mes

---

## 💰 Planes y Costos

### Plan Gratuito (actual)
- ✅ **10 SMS/mes gratis**
- ✅ Sin tarjeta de crédito
- ✅ Cualquier número
- ✅ Renovación automática cada mes

### Plan Paid (cuando lo necesites)
Precios aproximados por SMS (depende del país):

| País | Costo/SMS |
|------|-----------|
| Uruguay | ~$0.08 USD |
| Argentina | ~$0.06 USD |
| Brasil | ~$0.04 USD |
| USA | ~$0.02 USD |
| Nigeria | ~$0.01 USD |

### ¿Cuándo hacer upgrade?

Cuando necesites **más de 10 SMS/mes**:

1. Dashboard → **Billing** → **Fund Wallet**
2. Agregar fondos (mín. $10 USD)
3. Método de pago: Tarjeta, PayPal, crypto
4. ✅ Listo - envío ilimitado según fondos

---

## 🌍 Cobertura Global

Termii cubre **230+ países**, incluyendo:

✅ **América Latina**:
- Uruguay ✅
- Argentina ✅
- Brasil ✅
- Chile ✅
- Paraguay ✅
- México ✅

✅ **América del Norte**:
- USA ✅
- Canadá ✅

✅ **Europa**:
- España ✅
- Portugal ✅
- Italia ✅
- Francia ✅

✅ **África y Asia**:
- Nigeria ✅ (mejores tarifas)
- Sudáfrica ✅
- India ✅

---

## 🔧 Troubleshooting

### "SMS no llega"

**Causas comunes**:

1. **Número inválido**
   - ✅ Verificar formato: debe ser `+{código país}{número}` (ej: `+59899123456`)
   - ✅ SIN espacios ni guiones
   - ✅ Código de país correcto (+598 para Uruguay)

2. **Balance insuficiente** (plan paid)
   - ✅ Verificar dashboard → Billing → Balance
   - ✅ Agregar fondos si está en $0

3. **API Key incorrecta**
   - ✅ Verificar que copiaste toda la key (empieza con `TL`)
   - ✅ Sin espacios antes/después
   - ✅ Regenerar key en Settings si es necesario

4. **Delay de red**
   - ✅ Termii puede tardar hasta 1 minuto en entregar
   - ✅ Revisar SMS History en dashboard para ver status

### "Error 401 Unauthorized"

```
[SMS] ❌ Termii - Error enviando SMS
```

**Solución**:
- ✅ TERMII_API_KEY está mal o vacía
- ✅ Verificar `.env.local` tiene la key correcta
- ✅ Reiniciar backend después de cambiar `.env.local`

### "Error 422 Invalid Sender ID"

**Solución**:
- ✅ TERMII_SENDER_ID tiene más de 11 caracteres
- ✅ Usar `FaltaUno` (10 caracteres, funciona siempre)
- ✅ O solicitar Sender ID personalizado en dashboard

### "Error al parsear respuesta"

```
[SMS] ❌ Termii - Error en respuesta
```

**Solución**:
- ✅ Revisar logs completos: `[SMS] ℹ️ Termii Response: {...}`
- ✅ Verificar que API Key es válida
- ✅ Contactar soporte Termii si persiste

---

## 📚 Documentación Oficial

- **API Docs**: https://developers.termii.com/
- **Send SMS**: https://developers.termii.com/messaging
- **Dashboard**: https://accounts.termii.com/
- **Support**: support@termii.com

---

## 🔐 Seguridad

### Proteger API Key

1. ✅ **NUNCA** commitear `.env.local` a Git
2. ✅ Verificar que está en `.gitignore`
3. ✅ No compartir API Key públicamente
4. ✅ Regenerar si se filtra

### Variables de entorno producción

Para Cloud Run:

```bash
gcloud run services update faltauno-backend \
  --set-env-vars="\
SMS_ENABLED=true,\
SMS_PROVIDER=termii,\
TERMII_API_KEY=TLxxxxxxxxxxxxx,\
TERMII_SENDER_ID=FaltaUno,\
PHONE_VERIFICATION_ENABLED=true"
```

O desde Google Cloud Console:
- Cloud Run → Service → **Edit & Deploy New Revision**
- **Variables & Secrets** → Add Variable
- Agregar cada variable

---

## 🎯 Uso Estimado

### Flujo normal
- **Registro usuario**: 1 SMS
- **Reenvío código**: 0-1 SMS (si usuario pide reenviar)
- **Total por usuario nuevo**: 1-2 SMS

### Proyecciones

| Usuarios nuevos/mes | SMS necesarios | Costo (plan paid) |
|---------------------|----------------|-------------------|
| 5 usuarios | ✅ Plan gratuito | $0 |
| 50 usuarios | ~60 SMS | ~$3-5 USD |
| 100 usuarios | ~120 SMS | ~$6-10 USD |
| 500 usuarios | ~600 SMS | ~$30-50 USD |
| 1000 usuarios | ~1200 SMS | ~$60-100 USD |

---

## ✅ Checklist de Configuración

- [ ] Cuenta Termii creada
- [ ] Email verificado
- [ ] API Key obtenida
- [ ] `.env.local` actualizado con TERMII_API_KEY
- [ ] SMS_PROVIDER=termii configurado
- [ ] Backend reiniciado
- [ ] Logs muestran: `[SMS] ✅ Termii configurado`
- [ ] Test enviado a tu número
- [ ] SMS recibido correctamente

---

## 🆚 Comparación: Termii vs Twilio

| Feature | Termii | Twilio |
|---------|--------|--------|
| Plan gratuito | ✅ 10 SMS/mes | ✅ $15 crédito (~300 SMS) |
| Verificación previa | ❌ No requerida | ✅ SÍ (trial) |
| Sender ID | Genérico OK | Número comprado |
| Costo/SMS (UY) | ~$0.08 | ~$0.05 |
| Setup | 5 minutos | 15 minutos |
| Documentación | Buena | Excelente |
| Soporte | Email | Email + Phone |

**Recomendación**: 
- **Empezar con Termii** (más simple, sin restricciones)
- **Upgrade a Twilio** si necesitas escalar mucho (mejor pricing a volumen)

---

## 🎉 ¡Listo!

Con Termii configurado, tu app puede:
- ✅ Enviar SMS a **cualquier número** (sin verificar)
- ✅ **10 SMS gratis/mes** (perfecto para testing/MVP)
- ✅ Upgrade fácil cuando necesites más
- ✅ Cobertura global

¿Preguntas? Revisa troubleshooting o contacta: support@termii.com
