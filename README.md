# Falta Uno - Backend

Backend REST API para la aplicación Falta Uno, desarrollado con Spring Boot 3.5.0 y Java 21.

## 🚀 Tecnologías

- **Java 21**
- **Spring Boot 3.5.0**
- **Spring Security 6** (JWT + OAuth2)
- **PostgreSQL** (Base de datos)
- **Maven** (Gestión de dependencias)
- **MapStruct** (Mapeo de entidades/DTOs)
- **Lombok** (Reducción de boilerplate)
- **Docker** (Containerización)

## 📋 Requisitos previos

- Java 21 o superior
- Maven 3.8+
- PostgreSQL 15+
- Docker (opcional, para desarrollo)

## ⚙️ Configuración

### Variables de entorno

Crear archivo `.env` en el directorio raíz con:

```bash
# Base de datos
DB_HOST=localhost
DB_PORT=5432
DB_NAME=faltauno
DB_USER=postgres
DB_PASSWORD=postgres

# JWT
JWT_SECRET=tu_clave_secreta_jwt_muy_larga_y_segura_minimo_256_bits
JWT_EXPIRATION=86400000

# Frontend URL (CORS)
FRONTEND_URL=http://localhost:3000

# OAuth2 Google (opcional)
GOOGLE_CLIENT_ID=tu_google_client_id
GOOGLE_CLIENT_SECRET=tu_google_client_secret
```

### Base de datos

El proyecto usa **Flyway** para migraciones automáticas. Las tablas se crean automáticamente al iniciar.

```sql
CREATE DATABASE faltauno;
```

## 🏃 Ejecución

### Desarrollo local

```bash
# Compilar
mvn clean install

# Ejecutar
mvn spring-boot:run

# O con variables de entorno personalizadas
mvn spring-boot:run -Dspring-boot.run.arguments="--DB_HOST=localhost --JWT_SECRET=mi_secreto"
```

La API estará disponible en: `http://localhost:8080`

### Con Docker Compose

```bash
# Desde el directorio raíz del backend
docker-compose up -d

# Ver logs
docker-compose logs -f backend

# Detener
docker-compose down
```

## 📁 Estructura del proyecto

```
src/
├── main/
│   ├── java/uy/um/faltauno/
│   │   ├── config/          # Configuración (Security, CORS, JWT)
│   │   ├── controller/      # Endpoints REST
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── entity/          # Entidades JPA
│   │   ├── repository/      # Repositorios JPA
│   │   ├── service/         # Lógica de negocio
│   │   ├── util/            # Mappers, utilidades
│   │   └── FaltaUnoApplication.java
│   └── resources/
│       ├── application.yaml # Configuración Spring
│       └── db/migration/    # Scripts Flyway
└── test/                    # Tests unitarios
```

## 🔐 Autenticación

El backend soporta dos métodos de autenticación:

### 1. JWT (Email/Password)

```bash
POST /api/auth/login-json
Content-Type: application/json

{
  "email": "usuario@example.com",
  "password": "contraseña"
}

Response:
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "usuario": { ... }
  }
}
```

### 2. OAuth2 (Google)

```
GET /oauth2/authorization/google
```

Redirige a Google OAuth y luego al frontend con el token JWT.

## 📡 Endpoints principales

### Usuarios
- `POST /api/usuarios` - Registrar usuario
- `GET /api/usuarios/me` - Obtener usuario actual
- `PUT /api/usuarios/me` - Actualizar perfil
- `GET /api/usuarios/me/amigos` - Listar amigos
- `GET /api/usuarios` - Listar todos los usuarios
- `GET /api/usuarios/{id}` - Obtener usuario por ID

### Partidos
- `POST /api/partidos` - Crear partido
- `GET /api/partidos` - Listar partidos
- `GET /api/partidos/{id}` - Obtener partido por ID
- `PUT /api/partidos/{id}` - Actualizar partido
- `DELETE /api/partidos/{id}` - Eliminar partido
- `POST /api/partidos/{id}/cancelar` - Cancelar partido
- `POST /api/partidos/{id}/completar` - Completar partido
- `POST /api/partidos/{id}/invitar` - Invitar jugador

### Inscripciones
- `POST /api/inscripciones` - Solicitar inscripción
- `POST /api/inscripciones/{id}/aceptar` - Aceptar solicitud
- `POST /api/inscripciones/{id}/rechazar` - Rechazar solicitud
- `DELETE /api/inscripciones/{id}` - Cancelar inscripción

### Amistades
- `POST /api/amistades/{amigoId}` - Enviar solicitud
- `POST /api/amistades/{id}/aceptar` - Aceptar solicitud
- `POST /api/amistades/{id}/rechazar` - Rechazar solicitud
- `DELETE /api/amistades/{amigoId}` - Eliminar amistad
- `GET /api/amistades` - Listar amigos
- `GET /api/amistades/pendientes` - Solicitudes pendientes

### Reviews
- `POST /api/reviews` - Crear review
- `GET /api/reviews/usuario/{id}` - Reviews de un usuario
- `GET /api/reviews/partido/{id}` - Reviews de un partido

### Mensajes
- `GET /api/mensajes/partido/{partidoId}` - Chat del partido
- `POST /api/mensajes` - Enviar mensaje

## 🧪 Testing

```bash
# Ejecutar todos los tests
mvn test

# Test con cobertura
mvn test jacoco:report
```

## 🐳 Docker

### Build de la imagen

```bash
docker build -t faltauno-backend:latest .
```

### Ejecutar con Docker

```bash
docker run -d \
  -p 8080:8080 \
  -e DB_HOST=postgres \
  -e DB_PASSWORD=postgres \
  -e JWT_SECRET=mi_secreto \
  --name faltauno-backend \
  faltauno-backend:latest
```

## 📝 Notas de desarrollo

### Flyway Migrations

Las migraciones están en `src/main/resources/db/migration/`:
- `V1__initial_schema.sql` - Esquema inicial
- `V2__add_inscripciones.sql` - Sistema de inscripciones
- ...

### Mappers MapStruct

Los mappers se autogeneran en `target/generated-sources/annotations/`:
```bash
mvn clean compile
```

### Logs

Los logs se configuran en `application.yaml`:
```yaml
logging:
  level:
    uy.um.faltauno: DEBUG
```

## 🔧 Troubleshooting

### Error: "Table 'usuario' doesn't exist"
- Verificar que Flyway esté habilitado
- Ejecutar: `mvn flyway:migrate`

### Error: "JWT signature does not match"
- Verificar que `JWT_SECRET` sea el mismo en todas las instancias
- Mínimo 256 bits (32 caracteres)

### Error de CORS
- Verificar `FRONTEND_URL` en variables de entorno
- Revisar configuración en `SecurityConfig.java`

## 📄 Licencia

Proyecto académico - Universidad de Montevideo

## 👥 Equipo

Desarrollado por el equipo de Falta Uno para Sistemas Distribuidos 2024