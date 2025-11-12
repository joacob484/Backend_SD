# Decisiones de Arquitectura - Falta Uno Backend

## 📋 Overview

Este documento explica qué tecnologías usamos para cada caso y por qué.

---

## 🏗️ Stack Tecnológico

### 1. **Caffeine** (In-Memory Cache)
**Propósito:** Cache de datos de aplicación

**Uso actual:**
- ✅ Cache de queries JPA frecuentes
- ✅ Datos relativamente estáticos (usuarios, canchas)
- ✅ Reduce carga en PostgreSQL

**Configuración:**
```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=10m,recordStats=true
```

**Ventajas:**
- Muy rápido (in-memory, local a cada instancia)
- Sin infraestructura adicional
- Integración nativa con Spring Cache (`@Cacheable`)

**Desventajas:**
- No compartido entre instancias (cada instancia tiene su cache)
- Se pierde en restart

**Cuándo usar:**
- Cualquier dato que se lee mucho y cambia poco
- Queries repetitivos (perfil usuario, stats partido)

---

### 2. **WebSocket + RabbitMQ STOMP Broker**
**Propósito:** Comunicación en tiempo real cliente ↔ servidor

**Uso actual:**
- ✅ Chat en tiempo real
- ✅ Actualizaciones de partidos (inscripciones, estado)
- ✅ Indicador "usuario está escribiendo"
- ✅ Notificaciones push a usuarios conectados

**Arquitectura:**

#### Desarrollo Local (Default)
```yaml
websocket:
  broker:
    type: simple  # SimpleBroker in-memory
```

**Ventajas:**
- Sin infraestructura adicional
- Rápido y simple

**Limitación:**
- Solo funciona con 1 instancia

#### Producción Multi-Instancia
```yaml
websocket:
  broker:
    type: rabbitmq  # RabbitMQ STOMP relay

spring:
  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: 61613  # STOMP port
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
```

**Ventajas:**
- Escala a N instancias
- Los mensajes WebSocket se distribuyen entre todas las instancias
- Estándar de la industria para STOMP

**Cuándo habilitar RabbitMQ:**
- Cuando tengas múltiples instancias en producción (Cloud Run min-instances ≥ 2)
- Para testing de escalabilidad

**Proveedores recomendados:**
- **CloudAMQP** (https://www.cloudamqp.com/) - Free tier 20 conexiones
- **Google Cloud Marketplace** - RabbitMQ managed
- **Docker local** - Para desarrollo

---

### 3. **Google Cloud Pub/Sub** (Opcional - Deshabilitado)
**Propósito:** Eventos asíncronos entre servicios (Event-Driven Architecture)

**Estado actual:** 
- ✅ Dependencia en pom.xml
- ❌ `GCP_PUBSUB_ENABLED=false` (deshabilitado)

**Casos de uso FUTUROS:**

#### ❌ NO usar para:
- WebSocket/chat → Usa RabbitMQ STOMP
- Cache distribuido → Usa Caffeine o Redis

#### ✅ SÍ usar para:
1. **Envío de emails asíncrono**
   ```
   Evento: PARTIDO_CREADO
   → Pub/Sub → Email Service → SendGrid/SMTP
   ```

2. **Push notifications móviles**
   ```
   Evento: NUEVA_INSCRIPCION
   → Pub/Sub → FCM Service → Firebase Cloud Messaging
   ```

3. **Procesamiento batch**
   ```
   Evento: PARTIDO_COMPLETADO
   → Pub/Sub → Stats Service → Actualizar estadísticas
   ```

4. **Auditoria y Analytics**
   ```
   Evento: USUARIO_BAN
   → Pub/Sub → Audit Log Service → BigQuery
   ```

5. **Integración con Cloud Functions**
   ```
   Evento: FOTO_SUBIDA
   → Pub/Sub → Cloud Function → Image Resize
   ```

**Configuración (cuando se habilite):**
```yaml
gcp:
  pubsub:
    enabled: true
    topic: faltauno-events
    subscription: faltauno-events-sub
```

**Ventajas:**
- Escalable automáticamente
- Desacoplamiento entre servicios
- Retry automático
- Dead letter queue
- Integración nativa con GCP

**Cuándo habilitar:**
- Cuando necesites procesamiento asíncrono
- Cuando tengas múltiples servicios/microservicios
- Para workloads batch (no real-time)

**Costo:**
- Free tier: 10GB/mes
- Después: ~$40/TB

---

### 4. **Redis** (Configurado pero sin uso)

---

## 🎯 Decisiones Arquitectónicas

### Caching Strategy

| Tipo de Dato | Tecnología | Razón |
|--------------|-----------|-------|
| Perfil usuario | **Caffeine** | Lectura frecuente, cambios poco frecuentes |
| Stats partido | **Caffeine** | Calculado pesado, resultado cacheable |
| Ubicaciones/canchas | **Caffeine** | Datos casi estáticos |
| Session state | **JWT** (stateless) | Sin necesidad de session storage |

### Real-Time Communication

| Feature | Tecnología | Razón |
|---------|-----------|-------|
| Chat mensajes | **WebSocket** | Latencia <100ms requerida |
| Actualizaciones partido | **WebSocket** | Real-time critical |
| Typing indicators | **WebSocket** | Ephemeral, no persist |
| Message broker | **RabbitMQ** (prod) | STOMP estándar, multi-instance |
| Message broker | **SimpleBroker** (dev) | Sin infraestructura extra |

### Asynchronous Processing

| Feature | Tecnología | Estado | Razón |
|---------|-----------|--------|-------|
| Emails | **GCP Pub/Sub** | 🔜 Futuro | Desacoplamiento, retry automático |
| Push notifications | **GCP Pub/Sub** | 🔜 Futuro | Integración con FCM |
| Stats batch | **GCP Pub/Sub** | 🔜 Futuro | Procesamiento offline |
| Auditoria | **GCP Pub/Sub** | 🔜 Futuro | Analytics en BigQuery |

---

## 🚀 Roadmap de Infraestructura

### Fase 1: MVP (Actual) ✅
- ✅ PostgreSQL (Cloud SQL)
- ✅ Caffeine in-memory cache
- ✅ WebSocket con SimpleBroker
- ✅ JWT stateless auth

### Fase 2: Escalabilidad (Cuando >1 instancia)
- 🔄 RabbitMQ para WebSocket STOMP relay
- ⏭️ Cloud Run min-instances ≥ 2
- ⏭️ Load testing

### Fase 3: Async Processing (Cuando sea necesario)
- ⏭️ GCP Pub/Sub para emails
- ⏭️ Cloud Functions para image processing
- ⏭️ Firebase Cloud Messaging para push

### Fase 4: Advanced Features (Futuro)
- ⏭️ Redis Memorystore para distributed cache
- ⏭️ Rate limiting distribuido
- ⏭️ BigQuery para analytics

---

## 💰 Estimación de Costos

### Desarrollo Local
- **Total:** $0/mes
- PostgreSQL local o Cloud SQL dev
- SimpleBroker (in-memory)
- Caffeine (in-memory)

### Producción Small (<1000 usuarios activos)
- **Cloud Run:** ~$10-30/mes (CPU/memoria)
- **Cloud SQL:** ~$15-40/mes (db-f1-micro)
- **CloudAMQP Free:** $0/mes (hasta 20 conexiones)
- **GCP Pub/Sub:** $0/mes (free tier 10GB)
- **Total estimado:** ~$25-70/mes

### Producción Medium (1K-10K usuarios)
- **Cloud Run:** ~$50-150/mes
- **Cloud SQL:** ~$60-200/mes (db-n1-standard-1)
- **CloudAMQP Shared:** ~$19/mes
- **Memorystore Redis:** ~$50/mes (optional)
- **Total estimado:** ~$129-419/mes

---

## 📚 Referencias

- Spring Boot Cache: https://spring.io/guides/gs/caching/
- Spring WebSocket: https://docs.spring.io/spring-framework/reference/web/websocket.html
- RabbitMQ STOMP: https://www.rabbitmq.com/stomp.html
- GCP Pub/Sub: https://cloud.google.com/pubsub/docs
- CloudAMQP: https://www.cloudamqp.com/
- Google Cloud Memorystore: https://cloud.google.com/memorystore

---

## 🔄 Historial de Cambios

| Fecha | Cambio | Razón |
|-------|--------|-------|
| 2025-11-12 | Agregado RabbitMQ STOMP broker | Escalabilidad WebSocket |
| 2025-11-12 | Agregado Redis (sin usar) | Futuro distributed cache |
| 2025-11-12 | Pub/Sub deshabilitado | No necesario para MVP |
| 2025-11-12 | Caffeine como cache principal | Performance + simplicidad |
