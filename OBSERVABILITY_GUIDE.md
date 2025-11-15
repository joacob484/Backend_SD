# Observability Guide - Where to View Your Data

## 🎯 Quick Access Links

### Grafana Cloud (Metrics & Dashboards)
- **URL**: https://faltauno.grafana.net
- **Login**: Use your Grafana Cloud credentials
- **What you'll see**: All metrics from frontend and backend

### Google Cloud Console (Logs & Infrastructure)
- **Logs Explorer**: https://console.cloud.google.com/logs/query?project=master-might-274420
- **Cloud Run Services**: https://console.cloud.google.com/run?project=master-might-274420
- **What you'll see**: Structured logs, service health, infrastructure metrics

---

## 📊 1. Metrics (Grafana Cloud)

### Access Grafana Cloud
1. Go to https://faltauno.grafana.net
2. Click **Explore** (compass icon in left sidebar)
3. Select **Prometheus** as data source

### Query Your Metrics

#### Backend Metrics (from Spring Boot)
```promql
# All backend metrics
{job="faltauno-backend"}

# Partido operations
partido_creados_total
partido_actualizados_total
partido_cancelados_total
partido_confirmados_total
partido_completados_total

# Inscripcion operations
inscripcion_creada_total
inscripcion_aceptada_total
inscripcion_rechazada_total
inscripcion_cancelada_total

# JVM metrics
jvm_memory_used_bytes{job="faltauno-backend"}
jvm_threads_live{job="faltauno-backend"}
```

#### Frontend Metrics (from Next.js)
```promql
# All frontend metrics
{__name__=~"faltauno_.*"}

# API calls
faltauno_api_calls_total
faltauno_api_calls_total{endpoint="/api/partidos"}
faltauno_api_duration_ms

# User activity
faltauno_user_logins_total
faltauno_user_registrations_total
faltauno_page_views_total
faltauno_page_views_total{path="/home"}

# WebSocket
faltauno_websocket_connected
faltauno_websocket_messages_total

# Business metrics
faltauno_partidos_created_total
faltauno_partidos_joined_total
faltauno_partidos_cancelled_total

# Errors
faltauno_errors_total
faltauno_errors_total{type="api_error"}

# Performance
faltauno_page_load_ms
faltauno_web_vitals
```

### Create Dashboards
1. In Grafana, click **Dashboards** → **New** → **New Dashboard**
2. Click **Add visualization**
3. Select **Prometheus** data source
4. Enter metric query (see examples above)
5. Choose visualization type (Time series, Gauge, Stat, etc.)
6. Click **Apply** and **Save dashboard**

### Recommended Dashboards

#### Dashboard 1: Application Overview
- **Panels**:
  - API Request Rate: `rate(faltauno_api_calls_total[5m])`
  - API Error Rate: `rate(faltauno_api_calls_total{status=~"5.."}[5m])`
  - Active Users: `faltauno_user_logins_total`
  - WebSocket Connections: `faltauno_websocket_connected`

#### Dashboard 2: Business Metrics
- **Panels**:
  - Partidos Created: `increase(faltauno_partidos_created_total[1h])`
  - Partido Types: `faltauno_partidos_created_total` (group by `tipo`)
  - User Registrations: `increase(faltauno_user_registrations_total[1h])`

#### Dashboard 3: Performance
- **Panels**:
  - API Latency p50: `histogram_quantile(0.5, faltauno_api_duration_ms)`
  - API Latency p95: `histogram_quantile(0.95, faltauno_api_duration_ms)`
  - Page Load Times: `faltauno_page_load_ms`
  - Web Vitals: `faltauno_web_vitals` (group by metric name)

---

## 📝 2. Logs (Google Cloud Logging)

### Access Logs Explorer
1. Go to https://console.cloud.google.com/logs/query?project=master-might-274420
2. Or: Cloud Console → **Logging** → **Logs Explorer**

### Query Your Logs

#### Frontend Logs
```
resource.type="cloud_run_revision"
resource.labels.service_name="faltauno-frontend"
```

**Filter by severity:**
```
resource.type="cloud_run_revision"
resource.labels.service_name="faltauno-frontend"
severity="ERROR"
```

**Filter by component:**
```
resource.type="cloud_run_revision"
resource.labels.service_name="faltauno-frontend"
jsonPayload.component="api"
```

**Search for specific user:**
```
resource.type="cloud_run_revision"
resource.labels.service_name="faltauno-frontend"
jsonPayload.userId="USER_ID_HERE"
```

#### Backend Logs
```
resource.type="cloud_run_revision"
resource.labels.service_name="faltauno-backend"
```

**Filter by log level:**
```
resource.type="cloud_run_revision"
resource.labels.service_name="faltauno-backend"
severity="ERROR"
```

**Search in log text:**
```
resource.type="cloud_run_revision"
resource.labels.service_name="faltauno-backend"
textPayload=~"partido"
```

#### Error Tracking
```
resource.type="cloud_run_revision"
severity="ERROR"
```

### Create Log-based Metrics
1. In Logs Explorer, run a query
2. Click **Actions** → **Create metric**
3. Configure:
   - Metric type: Counter or Distribution
   - Filter: Your log query
   - Labels: Extract from log fields
4. Click **Create metric**
5. Metric will be available in Metrics Explorer

---

## 🏃 3. Cloud Run Metrics (Infrastructure)

### Access Cloud Run Dashboard
1. Go to https://console.cloud.google.com/run?project=master-might-274420
2. Click on service name (**faltauno-frontend** or **faltauno-backend**)
3. Click **METRICS** tab

### Available Metrics
- **Request count**: Requests per second
- **Request latency**: p50, p95, p99 latencies
- **Container instance count**: Active instances
- **Container CPU utilization**: CPU usage %
- **Container memory utilization**: Memory usage
- **Billable container instance time**: Cost tracking
- **Container startup latency**: Cold start times

### View Logs from Cloud Run
1. In Cloud Run service page
2. Click **LOGS** tab
3. Logs are filtered automatically for that service

---

## 🔍 4. Performance Monitoring

### Web Vitals (in Grafana)
Query these metrics in Grafana Explore:
```promql
# Cumulative Layout Shift
faltauno_web_vitals{name="CLS"}

# First Input Delay
faltauno_web_vitals{name="FID"}

# Largest Contentful Paint
faltauno_web_vitals{name="LCP"}

# Time to First Byte
faltauno_web_vitals{name="TTFB"}
```

### API Performance
```promql
# Average API response time
avg(faltauno_api_duration_ms)

# Slowest endpoints
topk(10, avg by (endpoint) (faltauno_api_duration_ms))

# API calls by status code
sum by (status) (faltauno_api_calls_total)
```

---

## 🚨 5. Alerts (Set up in Grafana)

### Create Alerts
1. In Grafana, go to **Alerting** → **Alert rules**
2. Click **New alert rule**
3. Configure:

**Example: High Error Rate Alert**
- **Query**: `rate(faltauno_api_calls_total{status=~"5.."}[5m]) > 10`
- **Condition**: Alert if query returns value > 0
- **Evaluation**: Every 1m for 5m
- **Notification**: Email/Slack

**Example: Low WebSocket Connections**
- **Query**: `faltauno_websocket_connected < 1`
- **Condition**: Alert if no connections for 10m
- **Evaluation**: Every 1m for 10m

**Example: High Latency**
- **Query**: `histogram_quantile(0.95, faltauno_api_duration_ms) > 3000`
- **Condition**: p95 latency > 3 seconds
- **Evaluation**: Every 5m

---

## 📈 6. Database Monitoring

### Cloud SQL Metrics
1. Go to https://console.cloud.google.com/sql/instances?project=master-might-274420
2. Click on your database instance
3. Click **OBSERVABILITY** tab

**Available metrics:**
- CPU utilization
- Memory utilization
- Connection count
- Query execution time
- Transaction count

### Connection Pool Metrics (in Grafana)
```promql
# HikariCP metrics from backend
hikaricp_connections_active{job="faltauno-backend"}
hikaricp_connections_idle{job="faltauno-backend"}
hikaricp_connections_pending{job="faltauno-backend"}
```

---

## 🎨 7. Sample Queries for Common Use Cases

### "How many users logged in today?"
**Grafana:**
```promql
increase(faltauno_user_logins_total[24h])
```

### "What's my API error rate?"
**Grafana:**
```promql
sum(rate(faltauno_api_calls_total{status=~"5.."}[5m])) 
/ 
sum(rate(faltauno_api_calls_total[5m]))
```

### "Show me all errors in the last hour"
**Cloud Logging:**
```
resource.type="cloud_run_revision"
severity="ERROR"
timestamp>="2024-01-01T10:00:00Z"
```

### "Which endpoints are slowest?"
**Grafana:**
```promql
topk(5, 
  histogram_quantile(0.95, 
    rate(faltauno_api_duration_ms[5m])
  )
)
```

### "How many partidos were created this week?"
**Grafana:**
```promql
increase(faltauno_partidos_created_total[7d])
```

### "What's my page load performance?"
**Grafana:**
```promql
histogram_quantile(0.95, faltauno_page_load_ms)
```

---

## 🔧 8. Troubleshooting

### "I don't see metrics in Grafana"
1. Wait 1-2 minutes after deployment (metrics push every 60s)
2. Check query syntax: `{job="faltauno-backend"}` or `{__name__=~"faltauno_.*"}`
3. Verify time range in Grafana (top-right corner)
4. Check Cloud Run logs for push errors:
   ```
   resource.type="cloud_run_revision"
   "metrics" OR "grafana"
   ```

### "Logs aren't showing up"
1. Check service name filter is correct
2. Verify timestamp range
3. Ensure structured logging is working (check `jsonPayload` in logs)
4. Frontend logs only appear in production (not in local dev)

### "Metrics show no data for specific time"
1. Check if service had traffic during that time
2. Verify Cloud Run instances were running
3. Check for cold starts or instance scaling to zero

---

## 📚 Best Practices

### Regular Monitoring
- Check Grafana dashboards daily
- Review error logs weekly
- Monitor performance trends monthly
- Set up critical alerts for immediate issues

### Custom Dashboards
- Create role-specific dashboards (DevOps, Product, Business)
- Use templating for filtering (environment, service, endpoint)
- Add links between dashboards for navigation
- Document dashboard panels with descriptions

### Log Management
- Use structured logging (already implemented)
- Include context in all logs (userId, requestId, component)
- Set appropriate log retention (default: 30 days)
- Create log-based metrics for important patterns

### Performance Optimization
- Monitor Web Vitals regularly
- Track API latency trends
- Identify and optimize slow endpoints
- Monitor cache hit rates

---

## 🎯 Quick Start Checklist

- [ ] Log in to Grafana Cloud: https://faltauno.grafana.net
- [ ] Run a test query: `{job="faltauno-backend"}`
- [ ] Create your first dashboard with API metrics
- [ ] Open Cloud Logging: https://console.cloud.google.com/logs
- [ ] Run a test log query for your services
- [ ] Check Cloud Run metrics for both services
- [ ] Set up at least one alert for critical errors
- [ ] Test triggering a metric by logging in to your app
- [ ] Verify metrics appear in Grafana within 1-2 minutes
- [ ] Create a bookmark folder with all monitoring URLs

---

## 🆘 Support

If metrics or logs aren't appearing:
1. Check Cloud Run service logs for errors
2. Verify environment variables are set correctly
3. Confirm Grafana API key is valid
4. Check network connectivity from Cloud Run to Grafana Cloud
5. Review this guide's troubleshooting section

Your observability stack is fully operational! 🚀
