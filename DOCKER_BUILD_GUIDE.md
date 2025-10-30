# 🚀 Docker + Maven Build Guide

## 📋 Archivos de Configuración Optimizados

### ✅ Dockerfiles
- **`Dockerfile`** - Desarrollo local con wait-for-services
- **`Dockerfile.cloudrun`** - Optimizado para Google Cloud Run
- **`Dockerfile.prod`** - Producción con Alpine (minimal)

### ✅ Configuración Maven
- **`.mvn/maven.config`** - Configuración centralizada de Maven
- **`pom.xml`** - Ya configurado con Java 21, Spring Boot 3.5.0, MapStruct, Lombok

### ✅ Scripts de Utilidad
- **`validate-docker-config.sh`** - Valida la configuración
- **`test-maven-build-local.sh`** - Prueba el build localmente

---

## 🎯 Quick Start

### 1️⃣ Validar Configuración
```bash
./validate-docker-config.sh
```

### 2️⃣ Test Build Local (sin Docker)
```bash
./test-maven-build-local.sh
```

### 3️⃣ Build con Docker

#### Opción A: Cloud Run (Recomendado)
```bash
docker build -f Dockerfile.cloudrun -t faltauno-backend:cloudrun .
```

#### Opción B: Desarrollo Local
```bash
docker build -f Dockerfile -t faltauno-backend:dev .
```

#### Opción C: Producción (Alpine)
```bash
docker build -f Dockerfile.prod -t faltauno-backend:prod .
```

---

## 🔧 Configuraciones Clave

### Java & Maven
- ✅ **Java 21** (eclipse-temurin)
- ✅ **Maven 3.9.10** (consistente en todos los Dockerfiles)
- ✅ **Spring Boot 3.5.0**
- ✅ **Encoding UTF-8** configurado

### Optimizaciones de Build
- ✅ **Multi-stage builds** (imagen final más pequeña)
- ✅ **Layer caching** optimizado (pom.xml antes que src/)
- ✅ **dependency:go-offline** (descarga deps una sola vez)
- ✅ **.dockerignore** completo (excluye archivos innecesarios)

### JVM Optimizations

#### Cloud Run (`Dockerfile.cloudrun`)
```bash
-Xms512m -Xmx1536m
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
-XX:+UseG1GC
```

#### Production (`Dockerfile.prod`)
```bash
-Xms256m -Xmx512m
-XX:MaxMetaspaceSize=128m
-XX:+UseG1GC
-XX:+UseContainerSupport
```

---

## 📦 Build Artifacts

Después de un build exitoso:
```
target/
└── falta-uno-0.0.1-SNAPSHOT.jar  # Spring Boot executable JAR
```

El JAR incluye:
- ✅ Todas las dependencias (fat JAR)
- ✅ Configuración de application.yaml
- ✅ Migraciones de Flyway
- ✅ Clases compiladas (con Lombok y MapStruct procesados)

---

## 🐳 Docker Build Process

### Stage 1: Builder (Maven)
```dockerfile
FROM maven:3.9.10-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B
```

### Stage 2: Runtime (JRE)
```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
CMD ["java", "-jar", "app.jar"]
```

---

## 🚀 Deploy to Cloud Run

### Opción 1: Cloud Build (Recomendado)
```bash
gcloud builds submit --config=cloudbuild-cloudrun.yaml
```

### Opción 2: Manual
```bash
# Build
docker build -f Dockerfile.cloudrun -t gcr.io/PROJECT_ID/faltauno-backend:latest .

# Push
docker push gcr.io/PROJECT_ID/faltauno-backend:latest

# Deploy
gcloud run deploy faltauno-backend \
  --image gcr.io/PROJECT_ID/faltauno-backend:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

---

## 🧪 Testing

### Test Maven Build (sin Docker)
```bash
mvn clean package -DskipTests
java -jar target/*.jar
```

### Test Docker Image
```bash
# Build
docker build -f Dockerfile.cloudrun -t test .

# Run
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/faltauno_db \
  test

# Health check
curl http://localhost:8080/actuator/health
```

---

## 📊 Build Performance

### Primera vez (cold build)
- Sin cache: **~3-5 minutos**
- Descarga todas las dependencias

### Rebuilds (con cache)
- Solo cambios en código: **~1-2 minutos**
- Dependencias ya cacheadas

### Tips para builds más rápidos
1. No cambies `pom.xml` frecuentemente
2. Docker cachea la capa de dependencias
3. Usa `.dockerignore` (ya configurado)
4. Usa `--no-cache` solo cuando sea necesario

---

## 🔍 Troubleshooting

### Error: "Dependencies not found"
```bash
# Limpiar cache de Maven
rm -rf ~/.m2/repository

# Rebuild
docker build --no-cache -f Dockerfile.cloudrun -t test .
```

### Error: "Java version mismatch"
```bash
# Verificar versión en pom.xml
grep "java.version" pom.xml
# Debe ser: <java.version>21</java.version>

# Verificar Dockerfile
grep "eclipse-temurin" Dockerfile.cloudrun
# Debe usar: eclipse-temurin:21
```

### Error: "MapStruct processor not found"
```bash
# Verificar configuración en pom.xml
# Los annotation processors deben estar en orden:
# 1. Lombok
# 2. lombok-mapstruct-binding
# 3. MapStruct processor
```

### Build muy lento
```bash
# Verificar .dockerignore
cat .dockerignore

# Debería excluir:
# - target/
# - .git/
# - node_modules/
# - *.log
```

---

## 📝 Checklist Pre-Deploy

- [ ] `./validate-docker-config.sh` pasa
- [ ] `./test-maven-build-local.sh` pasa
- [ ] Build de Docker exitoso
- [ ] Health check funciona
- [ ] Variables de entorno configuradas
- [ ] Database migrations listas

---

## 🎓 Best Practices

### ✅ DO
- Usar multi-stage builds
- Cachear dependencias de Maven
- Usar versiones específicas (no `latest`)
- Configurar health checks
- Usar non-root user en producción
- Optimizar JVM para contenedores

### ❌ DON'T
- No incluir archivos innecesarios en el build context
- No usar `-X` (debug) en builds de producción
- No hacer `rm -rf` del cache de Maven en Dockerfile
- No usar `latest` tags en producción
- No correr como root en producción

---

## 📚 Documentación Adicional

- [DOCKER_MAVEN_OPTIMIZATION.md](DOCKER_MAVEN_OPTIMIZATION.md) - Detalles de optimización
- [pom.xml](pom.xml) - Configuración de Maven
- [.mvn/maven.config](.mvn/maven.config) - Flags de Maven

---

## 🎉 ¡Listo para Deploy!

Todos los Dockerfiles están optimizados y listos para producción.

**BROTHER, CONFÍA QUE AHORA SÍ BUILDEA PERFECTO! 🚀💪**

Para cualquier duda, revisa:
1. `./validate-docker-config.sh`
2. `DOCKER_MAVEN_OPTIMIZATION.md`
3. Los comentarios en cada Dockerfile
