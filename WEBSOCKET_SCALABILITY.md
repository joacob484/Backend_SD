# WebSocket Scalability - Multi-Instance Architecture

## 📋 Overview

Este documento explica cómo escalar la aplicación a múltiples instancias con WebSocket/STOMP usando **RabbitMQ** como message broker externo.

## 🏗️ Arquitectura

### Single Instance (Desarrollo)
```
┌─────────────────┐
│  Frontend       │
│  (Browser)      │
└────────┬────────┘
         │ WebSocket
         ▼
┌─────────────────┐
│  Backend        │
│  SimpleBroker   │ ← In-memory broker
│  (Single JVM)   │
└─────────────────┘
```

**Problema:** Mensajes solo se distribuyen dentro de la misma instancia JVM.

### Multi-Instance (Producción)
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Frontend 1     │     │  Frontend 2     │     │  Frontend 3     │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │ WS                    │ WS                    │ WS
         ▼                       ▼                       ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Backend Inst 1 │◄────┼───RabbitMQ──────┼────►│  Backend Inst 3 │
│  (Cloud Run)    │     │  Message Broker │     │  (Cloud Run)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                ▲
                                │
                        ┌───────┴────────┐
                        │  Backend Inst 2│
                        │  (Cloud Run)   │
                        └────────────────┘
```

**Solución:** RabbitMQ distribuye mensajes a todas las instancias conectadas.

## 🔧 Configuración

### 1. Desarrollo Local (SimpleBroker)

**application.yaml**
```yaml
websocket:
  broker:
    type: simple  # Default - in-memory broker
```

**Ventajas:**
- ✅ No requiere infraestructura adicional
- ✅ Rápido para desarrollo local
- ✅ Sin configuración extra

**Desventajas:**
- ❌ Solo funciona con una instancia
- ❌ No escalable

### 2. Producción (RabbitMQ Broker Relay)

#### Opción A: RabbitMQ Local (Docker)

**docker-compose.yml**
```yaml
version: '3.8'
services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: faltauno-rabbitmq
    ports:
      - "5672:5672"    # AMQP port
      - "15672:15672"  # Management UI
      - "61613:61613"  # STOMP port
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    command: >
      bash -c "
        rabbitmq-plugins enable rabbitmq_stomp &&
        rabbitmq-server
      "

volumes:
  rabbitmq_data:
```

**Iniciar:**
```bash
docker-compose up -d
```

**Verificar:**
- Management UI: http://localhost:15672 (guest/guest)
- STOMP plugin: debe estar habilitado

#### Opción B: Google Cloud Memorystore for Redis (Alternative)

**Nota:** Spring no soporta Redis directamente como STOMP broker relay. Para usar Redis necesitas implementación custom con Pub/Sub.

#### Opción C: Cloud RabbitMQ (CloudAMQP, etc.)

**Proveedores:**
- CloudAMQP (https://www.cloudamqp.com/) - Free tier disponible
- Google Cloud RabbitMQ (marketplace)
- AWS Amazon MQ for RabbitMQ

**application.yaml (producción)**
```yaml
websocket:
  broker:
    type: rabbitmq  # Habilitar RabbitMQ broker relay

spring:
  rabbitmq:
    host: ${RABBITMQ_HOST}  # e.g., elephant.rmq.cloudamqp.com
    port: ${RABBITMQ_PORT:61613}  # STOMP port
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
    virtual-host: ${RABBITMQ_VIRTUAL_HOST:/}
```

**Variables de entorno (.env o Cloud Run):**
```bash
WEBSOCKET_BROKER_TYPE=rabbitmq
RABBITMQ_HOST=your-rabbitmq-host.com
RABBITMQ_PORT=61613
RABBITMQ_USERNAME=your-username
RABBITMQ_PASSWORD=your-password
RABBITMQ_VIRTUAL_HOST=/
```

## 🚀 Deployment en Google Cloud Run

### Configuración Cloud Run

**1. Crear servicio RabbitMQ externo** (CloudAMQP free tier o Cloud Marketplace)

**2. Configurar variables de entorno en Cloud Run:**
```bash
gcloud run services update faltauno-backend \
  --set-env-vars "WEBSOCKET_BROKER_TYPE=rabbitmq" \
  --set-env-vars "RABBITMQ_HOST=your-host" \
  --set-env-vars "RABBITMQ_PORT=61613" \
  --set-env-vars "RABBITMQ_USERNAME=your-user" \
  --set-env-vars "RABBITMQ_PASSWORD=your-pass"
```

**3. Escalar a múltiples instancias:**
```bash
gcloud run services update faltauno-backend \
  --min-instances=2 \
  --max-instances=10
```

### Verificación

**Test con múltiples instancias:**
1. Abrir 2+ tabs del frontend en navegadores diferentes
2. Conectarse a un partido (mismo partido en ambas tabs)
3. Realizar acción (inscripción, mensaje, etc.) en tab 1
4. Verificar que tab 2 recibe actualización instantánea

**Logs para debug:**
```bash
# Ver logs de conexiones WebSocket
gcloud run services logs read faltauno-backend \
  --filter="WebSocket" \
  --limit=100
```

## 📊 Comparación de Opciones

| Feature | SimpleBroker | RabbitMQ Broker Relay |
|---------|-------------|---------------------|
| **Escalabilidad** | ❌ Single instance | ✅ Multi-instance |
| **Infraestructura** | ✅ No extra | ❌ Requiere RabbitMQ |
| **Performance** | ✅ Muy rápido | ⚠️ Network latency |
| **Complejidad** | ✅ Simple | ⚠️ Media |
| **Costo** | ✅ Gratis | ⚠️ Hosting RabbitMQ |
| **Dev Local** | ✅ Ideal | ⚠️ Requiere Docker |
| **Producción** | ❌ No escalable | ✅ Recomendado |

## 🔍 Debugging

### Ver mensajes en RabbitMQ Management UI

1. Abrir http://localhost:15672 (o tu host cloud)
2. Login: guest/guest (o tus credenciales)
3. Ir a "Queues" tab
4. Ver exchanges: `amq.topic`, `amq.direct`
5. Monitor de mensajes en tiempo real

### Logs de conexión STOMP

**Backend logs:**
```
StompBrokerRelayMessageHandler : Connecting to STOMP broker at rabbitmq:61613
StompBrokerRelayMessageHandler : Successfully connected to STOMP broker
```

### Troubleshooting

**❌ Error: "Connection refused" al conectar a RabbitMQ**
- Verificar que RabbitMQ está corriendo: `docker ps`
- Verificar puerto STOMP (61613) expuesto
- Verificar plugin STOMP habilitado: `rabbitmq-plugins list`

**❌ Error: "Relay failed to connect"**
- Revisar credenciales (username/password)
- Verificar virtual-host (default: `/`)
- Revisar firewall/security groups en cloud

**❌ Mensajes no llegan a otras instancias**
- Verificar `WEBSOCKET_BROKER_TYPE=rabbitmq`
- Revisar logs de RabbitMQ Management UI
- Verificar que todas las instancias están conectadas al mismo RabbitMQ

## 💡 Recomendaciones

### Desarrollo Local
- Usar `websocket.broker.type=simple`
- No requiere configuración adicional
- Rápido y eficiente

### Staging/Testing
- Usar RabbitMQ en Docker
- Simular múltiples instancias localmente
- Validar escalabilidad antes de producción

### Producción
- Usar `websocket.broker.type=rabbitmq`
- CloudAMQP free tier (hasta 20 conexiones) o paid plan
- Monitorear métricas de RabbitMQ
- Configurar health checks para RabbitMQ
- Considerar HA (High Availability) RabbitMQ cluster

## 📚 Referencias

- Spring WebSocket Docs: https://docs.spring.io/spring-framework/reference/web/websocket.html
- RabbitMQ STOMP Plugin: https://www.rabbitmq.com/stomp.html
- CloudAMQP: https://www.cloudamqp.com/
- Google Cloud Run Multi-Instance: https://cloud.google.com/run/docs/about-instance-autoscaling

## 🎯 Next Steps

1. ✅ Desarrollo local con SimpleBroker (actual)
2. 🔄 Testing con RabbitMQ local (Docker)
3. ⏭️ Deploy a Cloud Run con CloudAMQP
4. ⏭️ Monitoreo y métricas de WebSocket
5. ⏭️ Load testing con múltiples instancias
