# Falta Uno - Backend (Informe técnico y operativo)

Este README es la versión consolidada y verificada del informe técnico del backend. Contiene únicamente funcionalidades y configuraciones que están implementadas en el repositorio.

Si encontrás alguna afirmación incorrecta o una dependencia que no se usa, avisame y lo corrijo.

---

## Resumen ejecutivo

Aplicación Spring Boot (Java 21) que expone una API REST y funcionalidades en tiempo real (WebSocket/STOMP). Está preparada para ejecutarse en Google Cloud Run dentro de un contenedor Docker multistage. Persistencia con PostgreSQL y migraciones mediante Flyway. Observability con Actuator + Micrometer (Prometheus) y logs JSON (Logback + google-cloud-logging-logback).

---

## Confirmado en el código (qué está implementado)

- Spring Boot 3.x aplicación: `src/main/java/uy/um/faltauno/FaltaUnoApplication.java`.
- Security: Spring Security + JWT support (clases en `config/` y JWT usage in auth controllers).
- OAuth2 Google login: endpoints handled (`/oauth2/authorization/google`, `login/oauth2` handlers exist).
- WebSocket + STOMP: server-side config in `config/WebSocketConfig.java`, controllers and publishers in `websocket/` (e.g., `WebSocketController.java`, `WebSocketEventPublisher.java`).
- SimpleBroker for development, and configuration to use a STOMP relay (RabbitMQ) in production (`application.yaml`, `WEBSOCKET_SCALABILITY.md`).
- Database: PostgreSQL expected; connection configured in `application.yaml` (Cloud SQL socket factory settings present). Flyway migrations in `src/main/resources/db/migration/`.
- Cache: Caffeine configured (`application.yaml`) and used via Spring Cache annotations in code.
- Metrics: Micrometer + Prometheus registry; Actuator endpoints (`/actuator/health`, `/actuator/prometheus`, `/actuator/metrics`) exposed per `application.yaml`.
- Logs: Logback JSON encoder and Google Cloud Logging appender dependency present; `logback-spring.xml` exists.
- Dockerfile: multi-stage builder optimized for Cloud Run (`Dockerfile` in project root).
- CI/CD: GitHub Actions workflow and/or Cloud Build configuration present (`.github/workflows/deploy.yml`, `cloudbuild` files found in repo).

---

## Items presentes pero intentionally disabled or optional

- GCP Pub/Sub dependencies are present in `pom.xml` and supporting classes exist, but `gcp.pubsub.enabled` is set to `false` by default in `application.yaml`. (So Pub/Sub is NOT active unless explicitly enabled and configured.)
- Redis libraries are included as dependencies (`spring-boot-starter-data-redis`, `lettuce-core`) but there is no active Redis-based caching configured by default; Caffeine is the active cache.

---

## Arquitectura (implementada)

- Monolito modular en Spring Boot with packages: `config`, `controller`, `service`, `repository`, `websocket`, `scheduled`, `util`.
- HTTP API: Spring MVC controllers under `controller/` implement REST endpoints.
- Real-time: WebSocket endpoints at `/ws` (STOMP over SockJS), publishers use `SimpMessagingTemplate`.
- Database: Cloud SQL recommended configuration is present (socket factory). Flyway runs on startup.

---

## Observability (implementado)

- Actuator endpoints available and configured in `application.yaml` (health, info, metrics, prometheus).
- Micrometer Prometheus registry dependency present; `actuator/prometheus` exposed.
- Logs are emitted in JSON via Logback encoder; Google Cloud Logging appender dependency present to forward logs in Cloud Run.

---

## Deployment notes (what's in repo)

- `Dockerfile` (multi-stage) builds the artifact and runs with optimized JVM flags for container environments.
- GitHub Actions workflow for deploy exists at `.github/workflows/deploy.yml` (review file to confirm pipeline behavior).
- Frontend build and deployment references exist in frontend folder (`cloudbuild-prod.yaml`, `Dockerfile.prod`).

Example manual deploy commands (adjust project IDs and secrets):

```powershell
gcloud builds submit --tag gcr.io/PROJECT_ID/faltauno-backend
gcloud run deploy faltauno-backend --image gcr.io/PROJECT_ID/faltauno-backend --platform managed --region us-central1 --set-env-vars "SPRING_PROFILES_ACTIVE=cloudrun"
```

Secrets should be injected from Secret Manager (DB password, `JWT_SECRET`, OAuth client secret).

---

## Runbook (operational checks)

- Health check: `GET /actuator/health` (returns health and details when profile allows).
- Logs: `gcloud run services logs read faltauno-backend --region=us-central1`.
- Metrics: `GET /actuator/prometheus` to inspect Prometheus metrics when running.
- WebSocket debugging: check backend logs for STOMP broker relay messages and `StompBrokerRelay` connect logs.

---

## Files and locations of interest

- `src/main/java/uy/um/faltauno/` — application source
- `src/main/resources/application.yaml` — configuration and profiles (`dev`, `cloudrun`, `prod`)
- `src/main/resources/db/migration/` — Flyway migrations
- `Dockerfile` — build configuration
- `pom.xml` — dependencies (includes Pub/Sub and Redis as optional dependencies)
- `OBSERVABILITY_SETUP.md`, `WEBSOCKET_SCALABILITY.md`, `ARCHITECTURE_DECISION.md` — supporting documentation

---

## Diagramas (PlantUML)

Diagrams were added under `diagrams/` (backend-architecture, websocket-scaling, observability) and reflect the implemented integrations.

---

Si querés, puedo:
- Generar `DEPLOY.md` con comandos paso a paso para Cloud Run + Cloud SQL + Secret Manager
- Renderizar los `.puml` a `png`/`svg` y colocarlos en `diagrams/` junto con los `.puml`

Fin del README consolidado.
---

## Resumen ejecutivo

El backend es una aplicación monolítica modular construida con Spring Boot 3.5 (Java 21). Está diseñada para ejecutarse en Google Cloud Run dentro de un contenedor Docker optimizado. Se apoya en PostgreSQL (Cloud SQL) para persistencia, Flyway para migraciones y Micrometer/Prometheus + Grafana Cloud para métricas. Para comunicación en tiempo real se utiliza WebSocket con STOMP; en desarrollo `SimpleBroker` y en producción se recomienda `StompBrokerRelay` con RabbitMQ para soportar múltiples instancias.

Este README actúa como informe técnico listo para auditoría y operativo de SRE.

---

## 1) Arquitectura detallada

- **Aplicación:** Spring Boot monolítica con módulos claros (`config`, `controller`, `service`, `repository`, `websocket`, `scheduled`). Punto de entrada: `FaltaUnoApplication.java`.
- **Contenedorización:** `Dockerfile` multi-stage (Maven builder → runtime JRE) con flags JVM optimizados para Cloud Run.
- **Database:** PostgreSQL 15 en Cloud SQL, conexión optimizada con Cloud SQL Socket Factory.
- **Cache:** Caffeine in-memory por instancia; Redis presente como dependencia para futuras mejoras de caching distribuido.
- **Mensajería en tiempo real:** Spring WebSocket + STOMP. Broker: `SimpleBroker` (dev) y `RabbitMQ` (production relay) para distribuir mensajes entre instancias.
- **Asincronía/eventos:** Soporte para GCP Pub/Sub (dependencia en `pom.xml`) — actualmente deshabilitado por configuración, preparado para migrar jobs asíncronos y pipelines de eventos.

Diagrama principal: `diagrams/backend-architecture.puml`

---

## 2) Comunicaciones y patrones

- **HTTP/REST:** API REST implementada con Spring MVC. Autenticación con Spring Security + JWT; OAuth2 (Google) para flujo social. Endpoints documentados en `src/main/resources` y en la sección de API abajo.

- **WebSocket / STOMP (Realtime):**
  - Uso: chat, actualizaciones de partidos, notificaciones en tiempo real y typing indicators.
  - Rutas y destinos (ejemplos):
    - `/topic/partidos/{partidoId}` — broadcast de cambios en partido
    - `/topic/partidos/{partidoId}/chat` — mensajes de chat
    - `/user/{userId}/queue/notifications` — notificaciones privadas al usuario
  - Desarrollo: `SimpleBroker` embebido (no requiere infra externa) — limitado a single instance.
  - Producción: `StompBrokerRelay` hacia RabbitMQ (STOMP plugin, puerto 61613) para soporte multi-instance. Configuración en `application.yaml` (`websocket.broker.type=rabbitmq` + `spring.rabbitmq.*`).

- **Pub/Sub (GCP):** diseñado para desacoplar envío de emails y procesamiento batch; cuando se habilite reemplazará llamadas sincrónicas por eventos (`PARTIDO_CREADO`, `NUEVA_INSCRIPCION`, etc.).

---

## 3) Persistencia y migraciones

- **Flyway**: migrations en `src/main/resources/db/migration`. `application.yaml` configura Flyway con `baselineOnMigrate` y reintentos para entornos Cloud.
- **JPA/Hibernate**: configuración orientada a producción (no `ddl-auto`), batching (`jdbc.batch_size`), y settings de performance (`plan_cache_max_size`).

---

## 4) Observabilidad

- **Logging:** `logback-spring.xml` + `logstash-logback-encoder` para JSON estructurado. En producción usar `google-cloud-logging-logback` appender para que Cloud Run envíe logs a Cloud Logging.
- **Métricas:** Micrometer + Prometheus registry; Actuator expone `/actuator/prometheus`. `application.yaml` incluye remote write/push a Grafana Cloud si está configurado.
- **Dashboards y alertas:** recomendaciones y queries PromQL están en `OBSERVABILITY_SETUP.md`. Alertas sugeridas: error rate, p95 latency, DB connection pressure, WebSocket dropped connections.
- **Tracing:** Recomendado instrumentar con OpenTelemetry y exportar a Grafana Cloud / Cloud Trace (no incluido por defecto en el repo).

Diagrama observabilidad: `diagrams/observability.puml`

---

## 5) CI/CD y despliegue (ejemplos)

Pipeline existente: Cloud Build (o GitHub Actions) que construye la imagen y despliega a Cloud Run.

Ejemplos de comandos para configurar y desplegar manualmente:

```powershell
# Build y push a Google Container Registry
gcloud builds submit --tag gcr.io/PROJECT_ID/faltauno-backend

# Deploy a Cloud Run
gcloud run deploy faltauno-backend `
  --image gcr.io/PROJECT_ID/faltauno-backend `
  --platform managed `
  --region us-central1 `
  --allow-unauthenticated `
  --set-env-vars "SPRING_PROFILES_ACTIVE=cloudrun,SPRING_DATASOURCE_USERNAME=app" 

# Actualizar variables de entorno (ejemplo: RabbitMQ, JWT_SECRET)
gcloud run services update faltauno-backend --set-env-vars "RABBITMQ_HOST=...",JWT_SECRET="projects/PROJECT_ID/secrets/JWT_SECRET/versions/latest"
```

Notas:
- Secrets sensibles conviene inyectarlos desde Secret Manager y mapearlos en Cloud Run.
- Para WebSocket relay con RabbitMQ, configurar `WEBSOCKET_BROKER_TYPE=rabbitmq` y `spring.rabbitmq.*`.

---

## 6) Runbook y troubleshooting (operativo)

- Health DOWN en Cloud Run:
  1. `gcloud run services logs read faltauno-backend --region=us-central1`
  2. Verificar Cloud SQL connection y secrets
  3. Revisar `actuator/health` y `actuator/info`

- Problemas de WebSocket (mensajes no llegan entre instancias):
  1. Confirmar `websocket.broker.type=rabbitmq` en variables de entorno
  2. Verificar conexión STOMP en RabbitMQ Management UI (15672)
  3. Ver logs: `gcloud run services logs read faltauno-backend --filter=StompBrokerRelay`

- Problemas de migraciones Flyway:
  1. Revisar `flyway_schema_history` en DB
  2. Chequear `application.yaml` profile `cloudrun` y credenciales

---

## 7) API: endpoints y contrato (resumen operativo)

- Health: `GET /actuator/health`
- Auth: `POST /api/auth/register`, `POST /api/auth/login-json`, OAuth2 redirect `/oauth2/authorization/google`
- Usuarios: `GET /api/usuarios/me`, `PUT /api/usuarios/me`, `GET /api/usuarios/{id}`
- Partidos: `CRUD /api/partidos` + acciones (`/cancelar`, `/completar`, `/invitar`)
- Inscripciones: `POST /api/inscripciones`, acciones de aceptar/rechazar
- Chat/WS: `WS /ws` (STOMP destinations `/topic/...`, `/user/...`)

---

## 8) Seguridad

- JWT secret: obligatorio en producción (mínimo 256 bits). Guardar en Secret Manager.
- CORS: configurar `FRONTEND_URL` en `application.yaml` para permitir solo orígenes conocidos.
- OAuth2: validar redirect URIs registradas en Google Cloud Console.

---

## 9) Archivos importantes (ubicación)

- `src/main/java/uy/um/faltauno/` — código fuente
- `src/main/resources/application.yaml` — configuración profiles (`dev`, `cloudrun`, `prod`)
- `src/main/resources/db/migration/` — Flyway migrations
- `Dockerfile` — multi-stage build
- `pom.xml` — dependencias y plugins
- `OBSERVABILITY_SETUP.md`, `WEBSOCKET_SCALABILITY.md`, `ARCHITECTURE_DECISION.md` — documentación complementaria

---

## 10) Diagramas (PlantUML)

He incluido diagramas en `diagrams/`:
- `diagrams/backend-architecture.puml` — arquitectura general
- `diagrams/websocket-scaling.puml` — flujo y solución para multi-instance
- `diagrams/observability.puml` — flujo de logs y métricas hacia Cloud Logging / Grafana

Puedes renderizarlos con PlantUML localmente o en un servicio online (ej. https://www.plantuml.com/plantuml).

---

Si querés, puedo:
- Generar un `DEPLOY.md` con pasos detallados y comandos `gcloud` para producción.
- Renderizar los diagramas y subir imágenes (si preferís que las incluya en el repo). 
- Crear un resumen ejecutivo en PDF listo para entregar.

Fin del informe técnico del backend.
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

---

## **Resumen de Arquitectura, Comunicaciones y Observabilidad**

- **Arquitectura general:** Aplicación monolítica modular basada en Spring Boot 3.5 (Java 21). Se despliega en Google Cloud Run usando contenedores Docker (multi-stage build). La base de datos principal es PostgreSQL (Cloud SQL) y las migraciones están gestionadas con Flyway.

- **Perfiles y entornos:**
  - `dev`: configuración para desarrollo local (H2/local Postgres, logging DEBUG, SimpleBroker para WebSocket).
  - `cloudrun`: perfil para producción en Cloud Run (Cloud SQL socket factory, cache Caffeine, optimizaciones Hikari, actuator y métricas activadas).
  - `prod`: perfil base para producción (ajustes de logging y parámetros finos).

- **Comunicación entre sistemas:**
  - **HTTP/REST:** API principal expuesta mediante controllers Spring MVC. Endpoints públicos protegidos con Spring Security + JWT; OAuth2 (Google) para login social.
  - **WebSocket/STOMP:** Comunicación en tiempo real para chat, actualizaciones de partido y typing indicators. En desarrollo se usa `SimpleBroker` (in-memory). En producción se recomienda usar un broker STOMP externo (RabbitMQ) configurado en `spring.rabbitmq` y `websocket.broker.type=rabbitmq` para soportar múltiples instancias y distribuir mensajes entre réplicas.
  - **Pub/Sub (GCP Pub/Sub):** Dependencia incluida y preparada para eventos asíncronos (emails, push, analytics) pero actualmente deshabilitada por `GCP_PUBSUB_ENABLED=false` — previsto para futuro desacoplamiento.
  - **Cache local:** Caffeine en cada instancia (in-memory). Rápido y sin infra, pero no compartido entre instancias; se documenta Redis/Redis Memorystore como opción futura para cache distribuido.

- **Despliegue y CI/CD:**
  - Cloud Build integrado con un workflow que construye la imagen Docker y despliega a Cloud Run al push sobre `main`. El `Dockerfile` usa multi-stage (maven builder + runtime JRE) con flags JVM optimizados para Cloud Run. Secrets (DB password, JWT secret, OAuth credentials) deben gestionarse mediante Secret Manager y variables de entorno en Cloud Run.

- **Observabilidad:**
  - **Logging:** Logback con encoder JSON + `google-cloud-logging-logback` appender para ingest en Google Cloud Logging (Cloud Run captura logs estructurados automáticamente). Se recomienda crear métricas basadas en logs (error count) en Cloud Logging.
  - **Métricas:** Micrometer + registry Prometheus; Actuator expone `/actuator/prometheus`. La configuración incluye push remoto a Grafana Cloud (remote_write) o uso de Prometheus scraping. Dashboards predefinidos y queries PromQL documentados en `OBSERVABILITY_SETUP.md`.
  - **Tracing / Alerts:** Integración planificada con Grafana Cloud; alertas para error rates y latencia en Prometheus/Grafana, y notificaciones via Slack/email.

- **Escalabilidad WebSocket:** Para escalar a múltiples instancias se usa RabbitMQ como STOMP relay (CloudAMQP o RabbitMQ gestionado). Documentado en `WEBSOCKET_SCALABILITY.md` con docker-compose y pasos de configuración en Cloud Run.

- **Recomendaciones rápidas:**
  - En producción usar `spring.profiles.active=cloudrun` y configurar `SPRING_DATASOURCE_*`, `JWT_SECRET`, `RABBITMQ_*` y credenciales de Grafana/Cloud Logging.
  - Para alta disponibilidad WebSocket, provisionar un RabbitMQ gestionado y ajustar `min-instances` en Cloud Run.
  - Considerar Redis Memorystore si se requiere cache compartida o rate-limiting distribuido.

---

_README actualizado automáticamente: incluye resumen de arquitectura, comunicaciones y observabilidad. Si querés, puedo integrar diagramas (PlantUML) o generar un `DEPLOY.md` más detallado con comandos `gcloud`._
