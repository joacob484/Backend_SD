# 🔭 Observability Stack - Guía de Configuración

Stack híbrido: **Google Cloud Operations + Grafana Cloud + Prometheus**

## 📊 Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                      FALTA UNO APP                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Frontend (Next.js)          Backend (Spring Boot)         │
│  ├── Cloud Logger            ├── Logback JSON             │
│  ├── Metrics Collector       ├── Micrometer               │
│  └── ObservabilityProvider   └── Actuator                 │
│          │                           │                      │
│          ▼                           ▼                      │
└──────────┼───────────────────────────┼──────────────────────┘
           │                           │
           │    ┌──────────────────┐   │
           ├───▶│ Google Cloud     │◀──┤
           │    │ Logging          │   │
           │    └──────────────────┘   │
           │                           │
           │    ┌──────────────────┐   │
           └───▶│ Prometheus       │◀──┤
                │ (Scrapes)        │
                └────────┬─────────┘
                         │
                         ▼
                ┌──────────────────┐
                │ Grafana Cloud    │
                │ - Dashboards     │
                │ - Alertas        │
                │ - Visualización  │
                └──────────────────┘
```

---

## ✅ Ya Implementado

### Frontend
- ✅ Cloud Logger con logs estructurados JSON
- ✅ Metrics Collector con formato Prometheus
- ✅ Endpoint `/api/metrics` para scraping
- ✅ ObservabilityProvider para tracking automático
- ✅ Web Vitals (LCP, FID, CLS)
- ✅ Error tracking automático

### Backend
- ✅ Logback con JSON encoder
- ✅ Google Cloud Logging appender
- ✅ Micrometer Prometheus registry
- ✅ Actuator endpoints habilitados
- ✅ Métricas de HTTP, JVM, DB automáticas

---

## 🚀 Setup Paso a Paso

### 1. Google Cloud Logging (Ya funciona!)

**En Cloud Run, los logs JSON se capturan automáticamente.**

#### Verificar logs:
1. Ir a: https://console.cloud.google.com/logs
2. Filtrar por:
   ```
   resource.type="cloud_run_revision"
   resource.labels.service_name="falta-uno-backend"
   ```

#### Ejemplo de log estructurado:
```json
{
  "severity": "INFO",
  "message": "Usuario creó partido",
  "timestamp": "2025-11-15T10:30:00Z",
  "context": {
    "userId": "user123",
    "partidoId": "partido456"
  }
}
```

#### Crear alertas en Cloud Logging:
1. Ir a **Logging > Logs-based Metrics**
2. Crear métrica: Errores HTTP 500
3. Filtro: `severity="ERROR" AND httpRequest.status=500`
4. Ir a **Monitoring > Alerting**
5. Crear alerta cuando contador > 5 en 5 min

---

### 2. Configurar Grafana Cloud (GRATIS)

#### A. Crear cuenta
1. Ir a: https://grafana.com/auth/sign-up/create-user
2. Seleccionar **Free Plan** (14 días trial de Pro, luego Free permanente)
3. Confirmar email

#### B. Obtener credenciales
1. En Grafana Cloud, ir a **Connections > Add new connection**
2. Buscar **Prometheus**
3. Copiar:
   - **Remote Write URL**: `https://prometheus-prod-XX-prod-us-central-0.grafana.net/api/prom/push`
   - **Username**: `1234567`
   - **Password**: `glc_xxx...`

---

### 3. Configurar Prometheus para Scrape

#### Opción A: Prometheus en Cloud Run (Recomendado)

Crear un servicio de Prometheus que scrape tus endpoints:

**prometheus.yml**:
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

remote_write:
  - url: https://prometheus-prod-XX-prod-us-central-0.grafana.net/api/prom/push
    basic_auth:
      username: 1234567
      password: glc_xxx...

scrape_configs:
  # Backend metrics
  - job_name: 'falta-uno-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['https://falta-uno-backend-xxx.run.app']
    
  # Frontend metrics (si expones endpoint público)
  - job_name: 'falta-uno-frontend'
    metrics_path: '/api/metrics'
    static_configs:
      - targets: ['https://falta-uno-frontend-xxx.run.app']
```

Deploy Prometheus en Cloud Run:
```bash
# Crear Dockerfile
cat > Dockerfile.prometheus << 'EOF'
FROM prom/prometheus:latest
COPY prometheus.yml /etc/prometheus/
EXPOSE 9090
CMD ["--config.file=/etc/prometheus/prometheus.yml"]
EOF

# Build y deploy
gcloud builds submit --tag gcr.io/PROJECT_ID/prometheus
gcloud run deploy prometheus \
  --image gcr.io/PROJECT_ID/prometheus \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

#### Opción B: Grafana Agent (Más simple)

Instalar Grafana Agent que scrape y envía a Grafana Cloud:

```yaml
# agent-config.yaml
server:
  log_level: info

prometheus:
  configs:
    - name: falta-uno
      scrape_configs:
        - job_name: backend
          metrics_path: /actuator/prometheus
          static_configs:
            - targets: ['backend-url:8080']
        
        - job_name: frontend
          metrics_path: /api/metrics
          static_configs:
            - targets: ['frontend-url:3000']
      
      remote_write:
        - url: https://prometheus-prod-XX.grafana.net/api/prom/push
          basic_auth:
            username: 1234567
            password: glc_xxx...
```

---

### 4. Importar Dashboards en Grafana

#### A. Dashboard de Spring Boot (Backend)

1. En Grafana Cloud, ir a **Dashboards > Import**
2. Usar ID: **4701** (JVM Micrometer)
3. Seleccionar data source: **grafanacloud-prom**
4. Import

**Métricas que verás:**
- JVM Memory (Heap, Non-Heap)
- CPU Usage
- HTTP requests por segundo
- Latencia de endpoints (p50, p95, p99)
- Connection pool (Hikari)
- Garbage Collection

#### B. Dashboard Custom para Falta Uno

Crear dashboard con estas métricas:

**Usuarios:**
```promql
# Registros por hora
rate(faltauno_user_registrations_total[1h])

# Logins por hora
rate(faltauno_user_logins_total[1h])
```

**Partidos:**
```promql
# Partidos creados por tipo
sum by (tipo) (faltauno_partidos_created_total)

# Inscripciones por hora
rate(faltauno_partidos_joined_total[1h])
```

**API Performance:**
```promql
# Latencia p95 por endpoint
histogram_quantile(0.95, 
  rate(faltauno_api_duration_ms_bucket[5m])
)

# Errores por minuto
rate(faltauno_errors_total[1m])
```

**WebSocket:**
```promql
# Conexiones activas
faltauno_websocket_connected

# Mensajes por segundo
rate(faltauno_websocket_messages_total[1m])
```

#### C. Dashboards recomendados adicionales

| Dashboard | ID | Descripción |
|-----------|----|-----------| 
| Node Exporter Full | 1860 | Métricas de sistema |
| Spring Boot 2.1 | 10280 | Métricas de Spring Boot |
| PostgreSQL Database | 9628 | Métricas de DB |

---

### 5. Configurar Alertas en Grafana

#### A. Alerta de Errores Altos

1. En dashboard, editar panel de errores
2. Click **Alert** tab
3. Configurar:

```
Name: Errores Altos en API
Evaluate every: 1m
For: 5m

Conditions:
WHEN max() OF query(A, 5m, now) IS ABOVE 10

Notifications:
- Email: tu@email.com
- Slack: #alerts
```

#### B. Alerta de Latencia Alta

```
Name: API Lenta
Evaluate every: 1m
For: 5m

Conditions:
WHEN avg() OF query(latencia_p95, 5m, now) IS ABOVE 1000

Message: API respondiendo lento (>1s p95)
```

#### C. Alerta de Partidos Sin Crear

```
Name: No hay partidos nuevos
Evaluate every: 1h
For: 2h

Conditions:
WHEN diff() OF query(partidos_created_total, 2h, now) IS BELOW 1

Message: No se crearon partidos en 2 horas
```

---

### 6. Integrar Slack (Opcional)

1. En Grafana, ir a **Alerting > Contact points**
2. Add contact point
3. Tipo: **Slack**
4. Webhook URL: (crear en Slack App)
5. Mensaje template:
```
{{ len .Alerts.Firing }} alertas activas

{{ range .Alerts.Firing }}
- {{ .Labels.alertname }}: {{ .Annotations.summary }}
{{ end }}
```

---

## 📈 Métricas Disponibles

### Frontend (Next.js)

| Métrica | Tipo | Descripción |
|---------|------|-------------|
| `faltauno_page_views_total` | Counter | Page views por ruta |
| `faltauno_page_load_ms` | Histogram | Tiempo de carga |
| `faltauno_user_logins_total` | Counter | Logins exitosos |
| `faltauno_user_registrations_total` | Counter | Registros |
| `faltauno_partidos_created_total` | Counter | Partidos creados |
| `faltauno_partidos_joined_total` | Counter | Inscripciones |
| `faltauno_api_calls_total` | Counter | Llamadas API |
| `faltauno_api_duration_ms` | Histogram | Latencia API |
| `faltauno_errors_total` | Counter | Errores JavaScript |
| `faltauno_websocket_connected` | Gauge | Estado WebSocket |
| `faltauno_websocket_messages_total` | Counter | Mensajes WS |

### Backend (Spring Boot)

| Métrica | Tipo | Descripción |
|---------|------|-------------|
| `http_server_requests_seconds` | Histogram | Latencia HTTP |
| `jvm_memory_used_bytes` | Gauge | Memoria JVM |
| `jvm_gc_pause_seconds` | Histogram | Garbage Collection |
| `hikaricp_connections_active` | Gauge | Conexiones DB activas |
| `hikaricp_connections_pending` | Gauge | Conexiones DB pending |
| `process_cpu_usage` | Gauge | CPU usage |
| `system_cpu_usage` | Gauge | System CPU |

---

## 🎨 Dashboards de Ejemplo

### Dashboard Principal

**Row 1: Overview**
- Total usuarios registrados (gauge)
- Partidos activos (gauge)
- Usuarios online (gauge)
- Tasa de error (stat)

**Row 2: Tráfico**
- Requests por segundo (graph)
- Latencia p50/p95/p99 (graph)
- Error rate (graph)

**Row 3: Partidos**
- Partidos creados por día (bar chart)
- Inscripciones por hora (graph)
- Partidos por tipo (pie chart)

**Row 4: Performance**
- JVM Heap usage (graph)
- DB connections (graph)
- GC time (graph)

---

## 🔍 Queries Útiles

### En Cloud Logging

**Errores en últimas 24h:**
```
severity="ERROR"
timestamp>="2025-11-14T00:00:00Z"
```

**Requests lentos (>1s):**
```
jsonPayload.duration > 1000
```

**Usuarios específicos:**
```
jsonPayload.context.userId="user123"
```

### En Grafana/Prometheus

**Top endpoints más lentos:**
```promql
topk(10, 
  histogram_quantile(0.95, 
    rate(http_server_requests_seconds_bucket[5m])
  )
)
```

**Tasa de error por endpoint:**
```promql
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
/
rate(http_server_requests_seconds_count[5m])
```

**Usuarios activos (aprox):**
```promql
count(count by (userId) (faltauno_page_views_total))
```

---

## 💰 Costos

### Google Cloud Logging
- **Free Tier**: 50 GB/mes
- **Precio**: $0.50/GB después del free tier
- **Estimado Falta Uno**: < 5 GB/mes = **GRATIS**

### Grafana Cloud
- **Free Plan**:
  - 10,000 series métricas
  - 50 GB logs
  - 50 GB traces
  - 14 días retention
- **Estimado Falta Uno**: Cabe en free tier = **GRATIS**

### Prometheus en Cloud Run (si usas)
- **Cloud Run**: ~$5/mes (mínimo)
- **Alternativa**: Grafana Agent (gratis)

**Total estimado: $0 - $5/mes**

---

## 🎯 Próximos Pasos

1. ✅ Deploy del código (ya tiene observabilidad integrada)
2. ⏳ Configurar Grafana Cloud account
3. ⏳ Configurar Prometheus scraping (Opción A o B)
4. ⏳ Importar dashboards
5. ⏳ Configurar alertas básicas
6. ⏳ (Opcional) Integrar Slack

---

## 📚 Recursos

- **Grafana Cloud**: https://grafana.com/products/cloud/
- **Prometheus**: https://prometheus.io/docs/
- **Micrometer**: https://micrometer.io/docs
- **Cloud Logging**: https://cloud.google.com/logging/docs
- **Dashboards**: https://grafana.com/grafana/dashboards/

---

## 🆘 Troubleshooting

**No veo métricas en Grafana:**
1. Verificar que Prometheus está scraping: `/targets`
2. Check credentials de remote_write
3. Verificar que endpoints responden: `/actuator/prometheus`, `/api/metrics`

**Logs no aparecen en Cloud Logging:**
1. Verificar que logs sean JSON en producción
2. Check profile activo: `spring.profiles.active=prod`
3. Verificar permisos de servicio account en Cloud Run

**Alertas no llegan:**
1. Verificar contact points configurados
2. Check notification policies
3. Test notification manualmente

---

Implementación completa! 🎉
