# 🚀 Implementación de Redis y RabbitMQ

Este documento describe la implementación de **Redis (caché)** y **RabbitMQ (mensajería asíncrona)** en la aplicación Falta Uno.

---

## 💾 **Redis - Sistema de Caché**

### **Propósito**
Optimizar el rendimiento de la aplicación almacenando en memoria datos frecuentemente consultados, reduciendo la carga en PostgreSQL.

### **Configuración**

**Archivo:** `src/main/java/uy/um/faltauno/config/CacheConfig.java`

```java
@Configuration
@EnableCaching
public class CacheConfig {
    // TTL: 10 minutos por defecto
    // Serialización: JSON con Jackson
}
```

**Variables de entorno:**
```properties
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
```

### **Cachés Implementados**

#### 1️⃣ **Cache de Usuarios** (`usuarios`)
- **TTL:** 10 minutos
- **Clave:** `UUID` del usuario
- **Métodos:**

```java
@Cacheable(value = "usuarios", key = "#id")
public UsuarioDTO getUsuario(UUID id)

@CacheEvict(value = "usuarios", key = "#usuarioId")
public Usuario actualizarPerfil(UUID usuarioId, PerfilDTO perfilDTO)

@CacheEvict(value = "usuarios", key = "#usuarioId")
public void subirFoto(UUID usuarioId, MultipartFile file)
```

**Beneficio:** Reducir consultas SQL en endpoints como `/api/usuarios/{id}`, `/api/usuarios/current`, perfiles de jugadores.

---

#### 2️⃣ **Cache de Sugerencias de Amistades** (`sugerencias`)
- **TTL:** 10 minutos
- **Clave:** `UUID` del usuario solicitante
- **Métodos:**

```java
@Cacheable(value = "sugerencias", key = "#usuarioId")
private List<UsuarioDTO> obtenerSugerenciasInteligentes(UUID usuarioId)

// Se invalida al actualizar perfil (pueden cambiar los criterios de sugerencia)
@CacheEvict(value = "sugerencias", allEntries = true)
public Usuario actualizarPerfil(...)
```

**Beneficio:** Algoritmo de sugerencias es costoso (compara partidos, amigos, nivel). Cache evita recalcular constantemente.

---

#### 3️⃣ **Cache de Partidos** (`partidos`)
- **TTL:** 10 minutos
- **Clave:** `UUID` del partido
- **Métodos:**

```java
@Cacheable(value = "partidos", key = "#id")
public PartidoDTO obtenerPartidoCompleto(UUID id)

@CacheEvict(value = "partidos", allEntries = true)
public void cancelarPartido(UUID id, ...)
```

**Beneficio:** Detalles de partido consultados múltiples veces (listado, jugadores, chat).

---

#### 4️⃣ **Cache de Partidos Disponibles** (`partidos-disponibles`)
- **TTL:** 10 minutos
- **Métodos:**

```java
@CacheEvict(value = "partidos-disponibles", allEntries = true)
public PartidoDTO crearPartido(PartidoDTO dto)

@CacheEvict(value = {"partidos", "partidos-disponibles"}, allEntries = true)
public void cancelarPartido(...)
```

**Beneficio:** Listado de partidos disponibles es consultado frecuentemente en el home.

---

### **Monitoreo de Redis**

```bash
# Conectar a Redis CLI
docker exec -it backend_sd-redis-1 redis-cli

# Ver todas las claves
KEYS *

# Ver una clave específica
GET usuarios::550e8400-e29b-41d4-a716-446655440000

# Ver estadísticas de caché
INFO stats

# Ver memoria usada
INFO memory

# Limpiar toda la caché (desarrollo)
FLUSHALL
```

---

## 🐰 **RabbitMQ - Mensajería Asíncrona**

### **Propósito**
Desacoplar operaciones secundarias (emails, notificaciones push, analytics) de las operaciones principales, mejorando el tiempo de respuesta de la API.

### **Configuración**

**Archivo:** `src/main/java/uy/um/faltauno/config/RabbitConfig.java`

```java
@Configuration
public class RabbitConfig {
    public static final String EXCHANGE_PARTIDOS = "exchange.partidos";
    public static final String QUEUE_NOTIFICATIONS = "notificaciones.queue";
    
    // Routing Keys:
    // - partidos.created
    // - partidos.cancelado
    // - partidos.completado
    // - partidos.* (wildcard para todos)
}
```

**Variables de entorno:**
```properties
SPRING_RABBITMQ_HOST=rabbitmq
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest
```

### **Eventos Publicados**

#### 1️⃣ **Partido Creado** (`partidos.created`)

**Publisher:** `PartidoService.crearPartido()`

**Payload:**
```json
{
  "event": "PARTIDO_CREADO",
  "partidoId": "uuid",
  "organizadorId": "uuid",
  "tipoPartido": "FUTBOL_5",
  "fecha": "2025-10-25",
  "ubicacion": "Complejo Deportivo Central"
}
```

**Casos de uso:**
- ✉️ Enviar email de confirmación al organizador
- 📱 Notificar a usuarios cercanos según ubicación
- 📊 Registrar en sistema de analytics

---

#### 2️⃣ **Partido Cancelado** (`partidos.cancelado`)

**Publisher:** `PartidoService.cancelarPartido()`

**Payload:**
```json
{
  "event": "PARTIDO_CANCELADO",
  "partidoId": "uuid",
  "organizadorId": "uuid",
  "motivo": "Mal clima",
  "jugadoresAfectados": 8
}
```

**Casos de uso:**
- ✉️ Enviar emails masivos de cancelación
- 📱 Push notifications urgentes
- 💰 Registrar en sistema de reembolsos
- 📊 Métricas de cancelaciones

---

#### 3️⃣ **Partido Completado** (`partidos.completado`)

**Publisher:** `PartidoService.completarPartido()`

**Payload:**
```json
{
  "event": "PARTIDO_COMPLETADO",
  "partidoId": "uuid",
  "organizadorId": "uuid",
  "jugadoresParticipantes": 10
}
```

**Casos de uso:**
- ✉️ Recordatorio para calificar jugadores
- 📊 Actualizar estadísticas globales
- 🏆 Generar reportes de actividad

---

### **Listener de Eventos**

**Archivo:** `src/main/java/uy/um/faltauno/events/NotificationListener.java`

```java
@Component
@RabbitListener(queues = "notificaciones.queue")
public class NotificationListener {
    
    public void onEvent(Map<String, Object> event) {
        String type = (String) event.get("event");
        
        switch (type) {
            case "PARTIDO_CREADO":
                procesarPartidoCreado(event);
                break;
            case "PARTIDO_CANCELADO":
                procesarPartidoCancelado(event);
                break;
            case "PARTIDO_COMPLETADO":
                procesarPartidoCompletado(event);
                break;
        }
    }
}
```

**TODO:** Implementar procesadores reales con servicios de email (SendGrid, AWS SES) y push notifications (Firebase Cloud Messaging).

---

### **Monitoreo de RabbitMQ**

**Management UI:** http://localhost:15672
- **Usuario:** guest
- **Contraseña:** guest

**Comandos útiles:**
```bash
# Ver estado de colas
docker exec backend_sd-rabbitmq-1 rabbitmqctl list_queues

# Ver exchanges
docker exec backend_sd-rabbitmq-1 rabbitmqctl list_exchanges

# Ver bindings
docker exec backend_sd-rabbitmq-1 rabbitmqctl list_bindings

# Ver consumidores
docker exec backend_sd-rabbitmq-1 rabbitmqctl list_consumers
```

---

## 📊 **Métricas de Rendimiento Esperadas**

### **Sin Redis:**
- `GET /api/usuarios/{id}`: ~150ms (query SQL + join)
- `GET /api/partidos/{id}`: ~200ms (query SQL + inscripciones)
- `GET /api/usuarios/sugerencias`: ~500ms (algoritmo complejo)

### **Con Redis (caché hit):**
- `GET /api/usuarios/{id}`: ~5-10ms (memoria)
- `GET /api/partidos/{id}`: ~5-10ms (memoria)
- `GET /api/usuarios/sugerencias`: ~10ms (memoria)

**Ganancia:** **95% reducción en latencia** para datos cacheados.

---

### **Sin RabbitMQ:**
- `POST /api/partidos/cancelar`: ~2000ms (DB + enviar 10 emails sincrónicamente)

### **Con RabbitMQ:**
- `POST /api/partidos/cancelar`: ~100ms (DB + publicar evento)
- **Emails enviados en background** sin bloquear la respuesta

**Ganancia:** **95% reducción en tiempo de respuesta** + mejor UX.

---

## 🧪 **Testing de Redis y RabbitMQ**

### **Probar Redis:**

```bash
# 1. Crear usuario
POST /api/auth/register

# 2. Obtener usuario (primera vez - sin caché)
GET /api/usuarios/{id}
# ⏱️ ~150ms

# 3. Obtener usuario (segunda vez - con caché)
GET /api/usuarios/{id}
# ⏱️ ~5ms ✅

# 4. Verificar en Redis
docker exec -it backend_sd-redis-1 redis-cli
> KEYS usuarios::*
```

---

### **Probar RabbitMQ:**

```bash
# 1. Crear partido
POST /api/partidos

# 2. Verificar logs del backend
# Debe aparecer: "📩 Evento recibido: PARTIDO_CREADO"

# 3. Verificar RabbitMQ UI
# http://localhost:15672 → Queues → notificaciones.queue
# Debería mostrar mensajes procesados

# 4. Cancelar partido
POST /api/partidos/{id}/cancelar

# 5. Verificar logs
# Debe aparecer: "❌ Partido cancelado: ... - Jugadores afectados: X"
```

---

## 🔧 **Troubleshooting**

### **Redis no conecta**
```bash
# Verificar que Redis esté corriendo
docker ps | grep redis

# Ver logs
docker logs backend_sd-redis-1

# Verificar conectividad desde backend
docker exec -it backend_sd-backend-1 ping redis
```

### **RabbitMQ no procesa eventos**
```bash
# Verificar que RabbitMQ esté corriendo
docker ps | grep rabbitmq

# Ver logs
docker logs backend_sd-rabbitmq-1

# Verificar queues en Management UI
http://localhost:15672/#/queues
```

### **Caché no se invalida**
```bash
# Limpiar Redis manualmente
docker exec -it backend_sd-redis-1 redis-cli FLUSHALL
```

---

## ✅ **Checklist de Implementación**

- [x] Redis configurado con TTL de 10 minutos
- [x] Caché de usuarios (`@Cacheable`, `@CacheEvict`)
- [x] Caché de sugerencias de amistades
- [x] Caché de partidos
- [x] RabbitMQ configurado con exchange y queue
- [x] Eventos publicados: CREADO, CANCELADO, COMPLETADO
- [x] Listener procesando eventos
- [ ] **TODO:** Integrar servicio de emails (SendGrid/AWS SES)
- [ ] **TODO:** Integrar push notifications (Firebase)
- [ ] **TODO:** Tests unitarios para caché
- [ ] **TODO:** Tests de integración para RabbitMQ

---

## 🚀 **Próximos Pasos**

1. **Implementar servicio de emails:**
   ```java
   @Service
   public class EmailService {
       public void enviarCancelacion(String email, String partidoNombre) {
           // SendGrid o AWS SES
       }
   }
   ```

2. **Implementar push notifications:**
   ```java
   @Service
   public class PushNotificationService {
       public void enviarPush(UUID usuarioId, String mensaje) {
           // Firebase Cloud Messaging
       }
   }
   ```

3. **Métricas con Actuator:**
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,metrics,caches
   ```

4. **Monitoreo con Prometheus + Grafana**

---

**Documentación creada:** 21 de octubre de 2025  
**Versión:** 1.0  
**Autor:** Equipo Falta Uno
