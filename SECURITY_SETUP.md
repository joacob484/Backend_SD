# 🔒 Guía de Configuración de Seguridad Pre-Producción

Esta guía detalla los pasos necesarios para configurar el backend de manera segura antes de desplegar a producción.

---

## 📋 Variables de Entorno Requeridas

### Backend

Crear un archivo `.env` o configurar en el servidor:

```bash
# ===================================
# SEGURIDAD - JWT
# ===================================
# ⚠️ CRÍTICO: Generar clave segura de 256 bits mínimo
# Comando para generar: openssl rand -base64 64
JWT_SECRET=TU_CLAVE_SECRETA_SUPER_LARGA_Y_ALEATORIA_GENERADA_CON_OPENSSL

# Duración del token en milisegundos (24 horas = 86400000)
JWT_EXPIRATION=86400000

# ===================================
# BASE DE DATOS
# ===================================
SPRING_DATASOURCE_URL=jdbc:postgresql://tu-servidor:5432/faltauno_db
SPRING_DATASOURCE_USERNAME=faltauno_user
SPRING_DATASOURCE_PASSWORD=PASSWORD_SEGURO_AQUI

# ===================================
# REDIS
# ===================================
SPRING_REDIS_HOST=tu-redis-host
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=REDIS_PASSWORD_AQUI

# ===================================
# RABBITMQ
# ===================================
SPRING_RABBITMQ_HOST=tu-rabbitmq-host
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=rabbitmq_user
SPRING_RABBITMQ_PASSWORD=RABBITMQ_PASSWORD_AQUI

# ===================================
# OAUTH2 - GOOGLE
# ===================================
GOOGLE_CLIENT_ID=tu-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=TU_GOOGLE_CLIENT_SECRET

# ===================================
# APLICACIÓN
# ===================================
FRONTEND_URL=https://tu-dominio-frontend.com
SERVER_PORT=8080

# ===================================
# PERFIL DE SPRING
# ===================================
SPRING_PROFILES_ACTIVE=prod
```

---

## ✅ Checklist de Seguridad Pre-Producción

### Backend

- [ ] **JWT_SECRET configurado sin valor por defecto**
  - Generar clave con: `openssl rand -base64 64`
  - Configurar en variables de entorno
  - Verificar que no esté en código fuente

- [ ] **Perfil de producción activado**
  - `SPRING_PROFILES_ACTIVE=prod`
  - Verificar que `DevSecurityConfig` NO esté activo
  - H2 Console deshabilitada en producción

- [ ] **Actuator endpoints restringidos**
  - Solo `/actuator/health` público
  - Otros endpoints protegidos con autenticación
  - Configurar autenticación si se necesita acceso a métricas

- [ ] **Rate limiting activo**
  - `RateLimitingFilter` configurado
  - 5 intentos por minuto en endpoints de auth
  - Logs de rate limiting funcionando

- [ ] **Base de datos segura**
  - Usuario PostgreSQL con permisos mínimos (no root)
  - Password fuerte configurado
  - Conexiones SSL habilitadas si es posible

- [ ] **Redis protegido**
  - `requirepass` configurado
  - Solo accesible desde red interna
  - Persistencia habilitada si es necesario

- [ ] **RabbitMQ seguro**
  - Credenciales cambiadas (no guest/guest)
  - Management UI deshabilitado o protegido
  - Solo accesible desde red interna

- [ ] **Logs de producción**
  - Nivel `INFO` o `WARN` para la app
  - No loguear datos sensibles (passwords, tokens)
  - Rotación de logs configurada

- [ ] **HTTPS configurado**
  - Certificado SSL válido
  - Redirect HTTP → HTTPS
  - HSTS headers habilitados

### Frontend

- [ ] **Variables de entorno configuradas**
  - `NEXT_PUBLIC_API_URL` apuntando a backend
  - Logger en modo producción (sin console.log)

- [ ] **Build optimizado**
  - `npm run build` sin errores
  - Source maps deshabilitados o protegidos
  - Assets optimizados

- [ ] **CSP Headers configurados**
  - Content Security Policy en nginx/cloudflare
  - Permitir solo recursos necesarios

### Infraestructura

- [ ] **Firewall configurado**
  - Solo puertos necesarios expuestos (80, 443)
  - Puertos de BD/Redis/RabbitMQ solo accesibles internamente
  - SSH con key-based auth

- [ ] **Backups configurados**
  - Backup automático de PostgreSQL
  - Política de retención definida
  - Probado restauración

- [ ] **Monitoreo configurado**
  - Health checks funcionando
  - Alertas para servicios caídos
  - Métricas de performance

---

## 🚀 Comandos de Despliegue

### Generar JWT Secret

```bash
openssl rand -base64 64
```

### Verificar configuración

```bash
# Verificar que JWT_SECRET esté configurado
echo $JWT_SECRET

# Verificar perfil activo
curl http://localhost:8080/actuator/health

# Verificar rate limiting
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/auth/login-json \
    -H "Content-Type: application/json" \
    -d '{"email":"test@test.com","password":"wrong"}'
  echo ""
done
# Debe retornar 429 después del 5to intento
```

### Build y Deploy Backend

```bash
# Con Maven
mvn clean package -DskipTests
java -jar target/Backend_SD-0.0.1-SNAPSHOT.jar

# Con Docker
docker build -t faltauno-backend .
docker run -p 8080:8080 --env-file .env faltauno-backend
```

### Build y Deploy Frontend

```bash
# Next.js
npm run build
npm start

# Con Docker
docker build -t faltauno-frontend .
docker run -p 3000:3000 faltauno-frontend
```

---

## 🔍 Verificación Post-Despliegue

### Tests de Seguridad

```bash
# 1. Verificar que H2 Console no esté accesible
curl http://tu-dominio.com/h2-console
# Debe retornar 404 o 401

# 2. Verificar que actuator solo exponga /health
curl http://tu-dominio.com/actuator/metrics
# Debe retornar 401

curl http://tu-dominio.com/actuator/health
# Debe retornar 200

# 3. Verificar rate limiting
for i in {1..6}; do
  curl -X POST http://tu-dominio.com/api/auth/login-json \
    -H "Content-Type: application/json" \
    -d '{"email":"test@test.com","password":"wrong"}'
done
# Debe retornar 429 en el 6to intento

# 4. Verificar HTTPS redirect
curl -I http://tu-dominio.com
# Debe retornar 301 o 302 hacia https://
```

### Logs de Verificación

Revisar logs para confirmar:

```bash
# Verificar que NO aparezcan estos logs en producción:
grep -r "JWT_SECRET:mi_clave_super_segura" logs/
grep -r "console.log" logs/
grep -r "H2 console" logs/

# Verificar que SÍ aparezcan:
grep "Rate limit excedido" logs/
grep "JwtFilter processing" logs/
```

---

## 📞 Soporte

Si tienes dudas sobre la configuración de seguridad:

1. Revisar este documento
2. Consultar `OAUTH_DEBUG.md` para problemas de autenticación
3. Revisar logs con `docker compose logs -f backend`

---

**Última actualización**: Octubre 2025
