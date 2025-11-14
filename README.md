# Falta Uno - Backend

Backend REST API para la aplicación Falta Uno, desarrollado con Spring Boot 3.5.0 y Java 21.

**🚀 Deployment Automático**: Cada push a `main` dispara deployment automático en Cloud Run via Cloud Build.

---

##  Stack Tecnológico

### Core
- **Java 21** - Runtime moderno y eficiente
- **Spring Boot 3.5.0** - Framework principal
- **Maven** - Gestión de dependencias

### Seguridad
- **Spring Security 6** - Autenticación y autorización
- **JWT** - Tokens de autenticación
- **OAuth2** - Login con Google

### Persistencia
- **PostgreSQL 15** (Cloud SQL) - Base de datos principal
- **Flyway** - Migraciones de BD automáticas
- **Spring Data JPA** - ORM

### Cache & Performance
- **Caffeine** - Cache in-memory (10,000 entradas, 10min TTL)
- **Connection pooling** - HikariCP optimizado

### Email (Opcional)
- **Spring Mail** - Notificaciones por email
- Ver: [EMAIL_SETUP_GUIDE.md](./EMAIL_SETUP_GUIDE.md)

### Utilidades
- **MapStruct** - Mapeo de entidades/DTOs
- **Lombok** - Reducción de boilerplate

---

##  Requisitos Locales

- **Java 21** o superior
- **Maven 3.8+**
- **PostgreSQL 15+** (o Docker)
- **Git**

---

##  Configuración Local

### 1. Variables de Entorno

Crear archivo `.env` en el directorio raíz:

```bash
# Base de datos
DB_HOST=localhost
DB_PORT=5432
DB_NAME=faltauno
DB_USER=postgres
DB_PASSWORD=postgres

# JWT (generar con: openssl rand -base64 64)
JWT_SECRET=tu_clave_secreta_jwt_muy_larga_y_segura_minimo_256_bits
JWT_EXPIRATION=86400000

# Frontend URL (CORS)
FRONTEND_URL=http://localhost:3000

# OAuth2 Google (opcional)
GOOGLE_CLIENT_ID=tu_google_client_id
GOOGLE_CLIENT_SECRET=tu_google_client_secret

# Email SMTP (opcional)
# MAIL_HOST=smtp.gmail.com
# MAIL_PORT=587
# MAIL_USERNAME=tu-email@gmail.com
# MAIL_PASSWORD=tu-app-password
```

### 2. Base de Datos

```sql
CREATE DATABASE faltauno;
```

Las tablas se crean automáticamente con Flyway al iniciar la aplicación.

---

##  Ejecución Local

### Opción 1: Maven

```bash
# Compilar
mvn clean install

# Ejecutar
mvn spring-boot:run
```

### Opción 2: JAR

```bash
# Compilar JAR
mvn clean package -DskipTests

# Ejecutar
java -jar target/falta-uno-0.0.1-SNAPSHOT.jar
```

La API estará disponible en: **http://localhost:8080**

---

##  Estructura del Proyecto

```
src/
 main/
    java/uy/um/faltauno/
       config/          # Configuración (Security, Cache, CORS)
       controller/      # Endpoints REST
       dto/             # Data Transfer Objects
       entity/          # Entidades JPA
       repository/      # Repositorios Spring Data
       service/         # Lógica de negocio
       util/            # Mappers, utilidades
       FaltaUnoApplication.java
    resources/
        application.yaml # Configuración
        db/migration/    # Migraciones Flyway
 test/                    # Tests
```

---

##  Autenticación

### 1. JWT (Email/Password)

```http
POST /api/auth/login-json
Content-Type: application/json

{
  "email": "usuario@example.com",
  "password": "contraseña"
}
```

**Respuesta:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "usuario": { ... }
  }
}
```

### 2. OAuth2 (Google)

```http
GET /oauth2/authorization/google
```

Redirige a Google OAuth  Callback  Redirige al frontend con JWT.

---

##  API Endpoints

### Health Check
- `GET /actuator/health` - Estado del servicio

### Autenticación
- `POST /api/auth/register` - Registro
- `POST /api/auth/login-json` - Login JWT
- `GET /oauth2/authorization/google` - Login Google

### Usuarios
- `GET /api/usuarios/me` - Usuario actual (requiere auth)
- `PUT /api/usuarios/me` - Actualizar perfil
- `GET /api/usuarios/me/amigos` - Listar amigos
- `GET /api/usuarios` - Listar usuarios
- `GET /api/usuarios/{id}` - Obtener por ID

### Partidos
- `POST /api/partidos` - Crear partido
- `GET /api/partidos` - Listar partidos
- `GET /api/partidos/{id}` - Detalle
- `PUT /api/partidos/{id}` - Actualizar
- `DELETE /api/partidos/{id}` - Eliminar
- `POST /api/partidos/{id}/cancelar` - Cancelar
- `POST /api/partidos/{id}/completar` - Completar
- `POST /api/partidos/{id}/invitar` - Invitar jugador

### Inscripciones
- `POST /api/inscripciones` - Solicitar
- `POST /api/inscripciones/{id}/aceptar` - Aceptar
- `POST /api/inscripciones/{id}/rechazar` - Rechazar
- `DELETE /api/inscripciones/{id}` - Cancelar

### Amistades
- `POST /api/amistades/{amigoId}` - Enviar solicitud
- `POST /api/amistades/{id}/aceptar` - Aceptar
- `POST /api/amistades/{id}/rechazar` - Rechazar
- `DELETE /api/amistades/{amigoId}` - Eliminar
- `GET /api/amistades` - Listar amigos
- `GET /api/amistades/pendientes` - Pendientes

### Reviews
- `POST /api/reviews` - Crear review
- `GET /api/reviews/usuario/{id}` - Por usuario
- `GET /api/reviews/partido/{id}` - Por partido

### Notificaciones
- `GET /api/notificaciones` - Listar
- `GET /api/notificaciones/no-leidas` - No leídas
- `GET /api/notificaciones/count` - Contar no leídas
- `PUT /api/notificaciones/{id}/leer` - Marcar leída
- `PUT /api/notificaciones/leer-todas` - Marcar todas
- `DELETE /api/notificaciones/{id}` - Eliminar

---

##  Deployment (Google Cloud Run)

### CI/CD Automático

El proyecto está configurado con **Cloud Build** para deployment automático:

1. Push a `main`  Dispara Cloud Build
2. Build con Dockerfile  Crea imagen Docker
3. Deploy a Cloud Run  Zero downtime
4. Health check automático

### Variables de Entorno (Cloud Run)

Configuradas como **Secret Manager secrets**:
- `SPRING_DATASOURCE_PASSWORD`  Cloud SQL password
- `JWT_SECRET`  JWT signing key
- `GOOGLE_CLIENT_ID`  OAuth2 Google
- `GOOGLE_CLIENT_SECRET`  OAuth2 Google
- `MAIL_USERNAME`  Email SMTP (opcional)
- `MAIL_PASSWORD`  Email SMTP (opcional)

### Configuración Cloud SQL

El perfil `cloudrun` en `application.yaml` usa Cloud SQL Socket Factory:
```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/faltauno_db?cloudSqlInstance=...&socketFactory=...
```

---

##  Testing

```bash
# Todos los tests
mvn test

# Con cobertura
mvn test jacoco:report

# Solo un test
mvn test -Dtest=UsuarioServiceTest
```

---

##  Cache (Caffeine)

El cache está configurado para mejorar performance:

**Configuración:**
- Tamaño máximo: 10,000 entradas
- TTL: 10 minutos
- Estadísticas habilitadas

**Uso en código:**
```java
@Cacheable(value = "usuarios", key = "#id")
public Usuario getUsuario(Long id) { ... }

@CacheEvict(value = "usuarios", key = "#id")
public void updateUsuario(Long id, ...) { ... }
```

---

##  Troubleshooting

### Error: "column verification_code does not exist"
 **Solucionado**: Las columnas se crean automáticamente con `@PostConstruct` en startup.

### Error: JWT signature mismatch
- Verificar que `JWT_SECRET` sea consistente
- Debe tener mínimo 256 bits (32 caracteres)

### Error de CORS
- Verificar `FRONTEND_URL` en variables
- Revisar `SecurityConfig.java`

### Health DOWN
- Verificar logs: `gcloud run services logs read faltauno-backend`
- Verificar Cloud SQL connection
- Verificar secrets configurados

---

##  Documentación Adicional

- [EMAIL_SETUP_GUIDE.md](./EMAIL_SETUP_GUIDE.md) - Configurar notificaciones email
- [EMAIL_NOTIFICATIONS.md](./EMAIL_NOTIFICATIONS.md) - Sistema de notificaciones
- [SECURITY_SETUP.md](./SECURITY_SETUP.md) - Configuración de seguridad
- [GOOGLE_CLOUD_SETUP.md](./GOOGLE_CLOUD_SETUP.md) - Setup en Google Cloud
- [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) - Solución de problemas

---

##  Contribuir

1. Fork del proyecto
2. Crear branch: `git checkout -b feature/nueva-funcionalidad`
3. Commit: `git commit -m 'feat: Descripción'`
4. Push: `git push origin feature/nueva-funcionalidad`
5. Pull Request

---

##  Licencia

Este proyecto es privado y confidencial.
