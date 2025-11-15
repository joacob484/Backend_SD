# Grafana Cloud Setup Guide - Falta Uno

## Overview
Hybrid observability stack combining Google Cloud Operations Suite with Grafana Cloud for comprehensive monitoring, dashboarding, and alerting.

**Architecture:**
- **Logs & Traces**: Google Cloud Logging + Cloud Trace (automatic for Cloud Run)
- **Metrics**: Micrometer/Prometheus → Grafana Cloud
- **Dashboards & Alerts**: Grafana Cloud (unified view)

---

## 1. Grafana Cloud Account Setup

### Create Free Account
1. Go to [grafana.com/auth/sign-up/create-user](https://grafana.com/auth/sign-up/create-user)
2. Sign up for free tier (includes 10k metrics, 50GB logs, 14-day retention)
3. Create your stack (choose region closest to your backend)
4. Note your stack details:
   - **Stack URL**: `https://[your-stack].grafana.net`
   - **Instance ID**: Found in stack settings
   - **API Key**: Generate in "Configuration → API Keys"

---

## 2. Prometheus Remote Write Configuration

Grafana Cloud provides a Prometheus-compatible remote write endpoint to receive metrics from your backend.

### Get Prometheus Credentials
1. In Grafana Cloud, go to **Connections → Add new connection**
2. Select **Hosted Prometheus**
3. Copy the Remote Write details:
   ```
   URL: https://prometheus-prod-XX-XX.grafana.net/api/prom/push
   Username: [instance-id]
   Password: [api-key]
   ```

### Configure Backend (Spring Boot)

Add to `application.yaml`:

```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
        pushgateway:
          enabled: true
          base-url: https://prometheus-prod-XX-XX.grafana.net/api/prom/push
          job: faltauno-backend
          username: ${GRAFANA_PROMETHEUS_USER:your-instance-id}
          password: ${GRAFANA_PROMETHEUS_PASSWORD:your-api-key}
          push-rate: 1m
```

**Alternative: Use Grafana Agent** (recommended for production)

Instead of push-gateway, use Grafana Agent to scrape metrics:

1. Install Grafana Agent on your Cloud Run instance or sidecar
2. Configure agent to scrape `/actuator/prometheus`:

```yaml
# agent-config.yaml
metrics:
  wal_directory: /tmp/agent/wal
  global:
    scrape_interval: 60s
    external_labels:
      cluster: faltauno-prod
      environment: production
    remote_write:
      - url: https://prometheus-prod-XX-XX.grafana.net/api/prom/push
        basic_auth:
          username: [instance-id]
          password: [api-key]
  configs:
    - name: faltauno
      scrape_configs:
        - job_name: 'faltauno-backend'
          static_configs:
            - targets: ['localhost:8080']
          metrics_path: '/actuator/prometheus'
```

---

## 3. Frontend Metrics Integration

### Option A: Push to Grafana Cloud via API

Update `lib/observability/metrics.ts`:

```typescript
async pushMetrics(): Promise<void> {
  if (typeof window === 'undefined' || process.env.NODE_ENV !== 'production') return;

  const metrics = this.exportPrometheus();
  
  try {
    // Push to Grafana Cloud Prometheus
    await fetch('https://prometheus-prod-XX-XX.grafana.net/api/prom/push', {
      method: 'POST',
      headers: {
        'Content-Type': 'text/plain',
        'Authorization': 'Basic ' + btoa(`${process.env.NEXT_PUBLIC_GRAFANA_USER}:${process.env.NEXT_PUBLIC_GRAFANA_PASSWORD}`)
      },
      body: metrics
    });
  } catch (error) {
    console.error('[Metrics] Failed to push to Grafana:', error);
  }
}
```

### Option B: Proxy via Backend

Keep current implementation (push to `/api/metrics`), then configure backend to forward to Grafana.

---

## 4. Google Cloud Logging Integration

Grafana Cloud can ingest logs from Google Cloud Logging.

### Setup Log Streaming

1. **Create Pub/Sub Topic** (for log export):
   ```bash
   gcloud pubsub topics create grafana-logs-export --project=faltauno-449020
   ```

2. **Create Log Sink**:
   ```bash
   gcloud logging sinks create grafana-cloud-sink \
     pubsub.googleapis.com/projects/faltauno-449020/topics/grafana-logs-export \
     --log-filter='resource.type="cloud_run_revision" AND severity>=WARNING'
   ```

3. **Grant Pub/Sub Publisher Role**:
   ```bash
   gcloud pubsub topics add-iam-policy-binding grafana-logs-export \
     --member="serviceAccount:cloud-logs@system.gserviceaccount.com" \
     --role="roles/pubsub.publisher"
   ```

4. **Configure Grafana Loki** (in Grafana Cloud):
   - Go to **Connections → Add data source → Google Cloud Pub/Sub**
   - Enter your GCP project ID and credentials
   - Select the `grafana-logs-export` topic
   - Logs will stream to Grafana Loki

**Alternative:** Use Google Cloud Logging directly as data source in Grafana (requires BigQuery export).

---

## 5. Import Pre-Built Dashboards

### Spring Boot Dashboard
1. In Grafana, go to **Dashboards → Import**
2. Use Dashboard ID: **4701** (JVM Micrometer)
3. Or **12900** (Spring Boot 2.1+ System Monitor)
4. Select your Prometheus data source

### Custom Falta Uno Dashboard

Create a new dashboard with panels:

#### Panel 1: Partido Operations Rate
```promql
rate(faltauno_partidos_created_total[5m])
rate(faltauno_partidos_updated_total[5m])
rate(faltauno_partidos_cancelled_total[5m])
rate(faltauno_partidos_confirmed_total[5m])
```

#### Panel 2: Inscripcion Operations Rate
```promql
rate(faltauno_inscripciones_created_total[5m])
rate(faltauno_inscripciones_accepted_total[5m])
rate(faltauno_inscripciones_rejected_total[5m])
```

#### Panel 3: Operation Duration (p95)
```promql
histogram_quantile(0.95, rate(faltauno_partido_create_duration_seconds_bucket[5m]))
histogram_quantile(0.95, rate(faltauno_partido_update_duration_seconds_bucket[5m]))
```

#### Panel 4: Frontend Page Load Times
```promql
faltauno_page_load_time{page=~".*"}
```

#### Panel 5: Error Rate
```promql
rate(faltauno_errors_total[5m])
rate(faltauno_api_errors_total[5m])
```

#### Panel 6: WebSocket Connections
```promql
faltauno_websocket_connections
faltauno_websocket_messages_sent_total
faltauno_websocket_messages_received_total
```

---

## 6. Configure Alerts

### Alert 1: High Error Rate
```yaml
Metric: rate(faltauno_errors_total[5m])
Condition: > 1 (more than 1 error per second)
For: 5m
Notification: Email/Slack
```

### Alert 2: Slow API Response
```yaml
Metric: histogram_quantile(0.95, rate(faltauno_partido_update_duration_seconds_bucket[5m]))
Condition: > 2 (p95 duration > 2 seconds)
For: 10m
Notification: Email/Slack
```

### Alert 3: Database Connection Pool Exhausted
```yaml
Metric: hikaricp_connections_active / hikaricp_connections_max
Condition: > 0.9 (90% pool utilization)
For: 5m
Notification: Email/Slack/PagerDuty
```

### Alert 4: Partido Confirmation Failures
```yaml
Metric: rate(faltauno_partidos_confirmed_total[1h])
Condition: < 0.5 AND > 0 (low confirmation rate during active hours)
For: 1h
Notification: Email
```

### Configure Notification Channels

1. Go to **Alerting → Notification channels**
2. Add channels:
   - **Email**: Your team email
   - **Slack**: Create webhook in Slack workspace
   - **PagerDuty**: For critical production alerts

---

## 7. Cloud Trace Integration

Google Cloud Trace is automatically enabled for Cloud Run.

### View Traces in Grafana
1. Add **Google Cloud Trace** data source:
   - Go to **Connections → Data sources → Add data source**
   - Search for "Google Cloud Trace"
   - Enter GCP project ID: `faltauno-449020`
   - Upload service account JSON key (with Trace Viewer role)

2. Create trace dashboard panels:
   - Latency distribution by endpoint
   - Request count by service
   - Error rate by trace

### Link Logs to Traces
Ensure backend logs include trace context:

```java
// Add to logback-spring.xml
<appender name="CLOUD_LOGGING" class="com.google.cloud.logging.logback.LoggingAppender">
    <enhancer>com.google.cloud.logging.logback.TraceLoggingEnhancer</enhancer>
    <!-- Automatically adds trace_id and span_id to logs -->
</appender>
```

---

## 8. Environment Variables

Add to Cloud Run deployment:

```bash
# Grafana Cloud
GRAFANA_PROMETHEUS_USER=your-instance-id
GRAFANA_PROMETHEUS_PASSWORD=your-api-key

# Google Cloud (already set)
GOOGLE_CLOUD_PROJECT=faltauno-449020
```

Add to frontend `.env.production`:

```bash
NEXT_PUBLIC_GRAFANA_USER=your-instance-id
NEXT_PUBLIC_GRAFANA_PASSWORD=your-api-key
```

**Security Note:** For frontend, consider creating a write-only token with limited permissions.

---

## 9. Testing & Validation

### Backend Metrics
1. Start backend locally
2. Access `http://localhost:8080/actuator/prometheus`
3. Verify metrics appear:
   ```
   faltauno_partidos_created_total
   faltauno_inscripciones_accepted_total
   ```

### Frontend Metrics
1. Open app in browser
2. Navigate to `http://localhost:3000/api/metrics`
3. Verify Prometheus-formatted metrics

### Grafana Cloud
1. Go to **Explore** in Grafana
2. Select Prometheus data source
3. Query: `faltauno_partidos_created_total`
4. Should see data points (may take 1-2 minutes for first scrape)

---

## 10. Production Deployment Checklist

- [ ] Grafana Cloud account created
- [ ] Prometheus remote write configured in backend
- [ ] Grafana Agent installed (or push-gateway enabled)
- [ ] Google Cloud Logging sink created
- [ ] Loki data source configured for logs
- [ ] Cloud Trace data source configured
- [ ] Pre-built Spring Boot dashboard imported
- [ ] Custom Falta Uno dashboard created
- [ ] Alerts configured (error rate, latency, pool exhaustion)
- [ ] Notification channels set up (email, Slack)
- [ ] Environment variables added to Cloud Run
- [ ] Metrics tested in staging environment
- [ ] Team trained on Grafana interface

---

## 11. Grafana Dashboard JSON (Quick Import)

Save as `faltauno-dashboard.json` and import:

```json
{
  "dashboard": {
    "title": "Falta Uno - Production Overview",
    "panels": [
      {
        "title": "Partido Operations Rate",
        "targets": [
          {
            "expr": "rate(faltauno_partidos_created_total[5m])",
            "legendFormat": "Created"
          },
          {
            "expr": "rate(faltauno_partidos_updated_total[5m])",
            "legendFormat": "Updated"
          },
          {
            "expr": "rate(faltauno_partidos_cancelled_total[5m])",
            "legendFormat": "Cancelled"
          }
        ],
        "type": "graph"
      },
      {
        "title": "API Error Rate",
        "targets": [
          {
            "expr": "rate(faltauno_errors_total[5m])",
            "legendFormat": "Errors/sec"
          }
        ],
        "type": "graph",
        "alert": {
          "conditions": [
            {
              "evaluator": {
                "params": [1],
                "type": "gt"
              },
              "operator": {
                "type": "and"
              },
              "query": {
                "params": ["A", "5m", "now"]
              },
              "reducer": {
                "params": [],
                "type": "avg"
              },
              "type": "query"
            }
          ],
          "executionErrorState": "alerting",
          "for": "5m",
          "frequency": "1m",
          "message": "High error rate detected in Falta Uno backend",
          "name": "High Error Rate Alert",
          "noDataState": "no_data",
          "notifications": []
        }
      }
    ],
    "timezone": "America/Montevideo",
    "schemaVersion": 16,
    "version": 0
  }
}
```

---

## 12. Cost Optimization

### Grafana Cloud Free Tier Limits
- **Metrics**: 10,000 series
- **Logs**: 50 GB/month
- **Traces**: 50 GB/month
- **Retention**: 14 days

### Tips to Stay Within Free Tier
1. **Filter high-cardinality metrics**: Avoid labels with many unique values (e.g., user IDs)
2. **Sample frontend metrics**: Only push metrics for 10% of users
3. **Reduce scrape frequency**: Set to 60s instead of 15s
4. **Use log sampling**: Only send ERROR and CRITICAL logs to Grafana
5. **Set up log aggregation**: Pre-aggregate in Cloud Logging before exporting

### Upgrade Path
If you exceed free tier:
- **Grafana Cloud Pro**: $49/month (100k metrics, 100 GB logs)
- **Self-hosted Grafana**: Free, but requires infrastructure management

---

## 13. Useful PromQL Queries

### Business Metrics
```promql
# Daily partido creation rate
rate(faltauno_partidos_created_total[24h])

# Inscripcion acceptance ratio
rate(faltauno_inscripciones_accepted_total[1h]) / rate(faltauno_inscripciones_created_total[1h])

# Average partido confirmation time
avg(faltauno_partido_confirm_duration_seconds)
```

### Performance Metrics
```promql
# p99 latency for partido updates
histogram_quantile(0.99, rate(faltauno_partido_update_duration_seconds_bucket[5m]))

# Request rate by endpoint
sum(rate(http_server_requests_seconds_count[5m])) by (uri)

# Error percentage
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) * 100
```

### Infrastructure Metrics
```promql
# JVM memory usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# Database connection pool usage
hikaricp_connections_active / hikaricp_connections_max * 100

# CPU usage (from Cloud Run metrics)
container_cpu_usage_seconds_total
```

---

## 14. Support & Resources

- **Grafana Docs**: [grafana.com/docs](https://grafana.com/docs)
- **Prometheus Docs**: [prometheus.io/docs](https://prometheus.io/docs)
- **Spring Boot Actuator**: [docs.spring.io/spring-boot/docs/current/reference/html/actuator.html](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- **Grafana Community**: [community.grafana.com](https://community.grafana.com)

---

## Summary

You now have a complete hybrid observability stack:

1. **Structured Logging**: JSON logs → Cloud Logging (dev/prod profiles)
2. **Metrics**: Micrometer → Prometheus → Grafana Cloud
3. **Traces**: Cloud Trace (automatic for Cloud Run)
4. **Dashboards**: Grafana Cloud (unified view)
5. **Alerts**: Grafana Alerting → Email/Slack

**Next Steps:**
1. Create Grafana Cloud account
2. Configure Prometheus remote write or install Grafana Agent
3. Import dashboards
4. Set up alerts
5. Deploy and monitor! 🚀
