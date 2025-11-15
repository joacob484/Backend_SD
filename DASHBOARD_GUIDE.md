# Creating Grafana Dashboards - Step-by-Step Guide

## 🎯 Quick Start: Create Your First Dashboard in 5 Minutes

### Step 1: Access Grafana
1. Go to https://faltauno.grafana.net
2. Log in with your Grafana Cloud credentials
3. Click **Dashboards** in the left sidebar (4 squares icon)

### Step 2: Create New Dashboard
1. Click **New** button (top right) → **New Dashboard**
2. Click **+ Add visualization**
3. Select **Prometheus** as the data source

### Step 3: Add Your First Panel
1. In the query editor at the bottom, enter:
   ```promql
   rate(faltauno_api_calls_total[5m])
   ```
2. You'll see a graph appear showing API requests per second
3. On the right panel, configure:
   - **Panel title**: "API Request Rate"
   - **Description**: "Requests per second across all endpoints"
4. Click **Apply** (top right)

### Step 4: Save Dashboard
1. Click the **Save** icon (💾) at the top
2. Enter dashboard name: "FaltaUno - Application Overview"
3. Click **Save**

Congratulations! You've created your first dashboard. Now let's build comprehensive ones.

---

## 📊 Pre-built Dashboard Templates

### Dashboard 1: Application Health Overview

**Purpose**: High-level health monitoring - your "mission control"

#### Panels to Add:

**Row 1: Traffic Metrics**

1. **API Request Rate**
   - **Query**: `sum(rate(faltauno_api_calls_total[5m]))`
   - **Visualization**: Time series (line graph)
   - **Unit**: req/s
   - **Legend**: Total Requests/sec

2. **API Success Rate**
   - **Query**: 
     ```promql
     sum(rate(faltauno_api_calls_total{status=~"2.."}[5m])) 
     / 
     sum(rate(faltauno_api_calls_total[5m])) * 100
     ```
   - **Visualization**: Stat (big number)
   - **Unit**: percent (0-100)
   - **Thresholds**: Green > 95%, Yellow > 90%, Red < 90%

3. **API Error Rate**
   - **Query**: 
     ```promql
     sum(rate(faltauno_api_calls_total{status=~"[45].."}[5m]))
     ```
   - **Visualization**: Time series
   - **Unit**: req/s
   - **Color**: Red
   - **Alert**: Set threshold at 10 errors/sec

**Row 2: User Activity**

4. **Active WebSocket Connections**
   - **Query**: `faltauno_websocket_connected`
   - **Visualization**: Gauge
   - **Unit**: connections
   - **Max**: 1000 (adjust based on your scale)

5. **User Logins (Last Hour)**
   - **Query**: `increase(faltauno_user_logins_total[1h])`
   - **Visualization**: Stat
   - **Unit**: logins
   - **Color**: Blue

6. **New Registrations (Last 24h)**
   - **Query**: `increase(faltauno_user_registrations_total[24h])`
   - **Visualization**: Stat
   - **Unit**: registrations
   - **Color**: Green

**Row 3: Performance**

7. **API Response Time (p95)**
   - **Query**: `histogram_quantile(0.95, rate(faltauno_api_duration_ms_bucket[5m]))`
   - **Visualization**: Time series
   - **Unit**: milliseconds (ms)
   - **Thresholds**: Green < 500ms, Yellow < 1000ms, Red > 1000ms

8. **Page Load Time (p95)**
   - **Query**: `histogram_quantile(0.95, rate(faltauno_page_load_ms_bucket[5m]))`
   - **Visualization**: Time series
   - **Unit**: ms
   - **Thresholds**: Green < 2000ms, Yellow < 3000ms, Red > 3000ms

**Row 4: Errors & Health**

9. **Total Errors (Last Hour)**
   - **Query**: `increase(faltauno_errors_total[1h])`
   - **Visualization**: Stat
   - **Unit**: errors
   - **Color scheme**: Red scale

10. **Backend Service Health**
    - **Query**: `up{job="faltauno-backend"}`
    - **Visualization**: Stat
    - **Value mappings**: 1 = "Healthy", 0 = "Down"
    - **Thresholds**: Green = 1, Red = 0

---

### Dashboard 2: Business Metrics

**Purpose**: Track key business KPIs and user behavior

#### Panels to Add:

**Row 1: Partido Metrics**

1. **Partidos Created Today**
   - **Query**: `increase(faltauno_partidos_created_total[24h])`
   - **Visualization**: Stat (big number)
   - **Unit**: partidos
   - **Color**: Blue

2. **Partidos Created Over Time**
   - **Query**: `increase(faltauno_partidos_created_total[1h])`
   - **Visualization**: Time series
   - **Unit**: partidos/hour

3. **Partidos by Type**
   - **Query**: `sum by (tipo) (faltauno_partidos_created_total)`
   - **Visualization**: Pie chart
   - **Legend**: Show tipo labels

4. **Partido Conversion Funnel**
   - **Queries** (add multiple):
     - Created: `faltauno_partidos_created_total`
     - Joined: `faltauno_partidos_joined_total`
     - Cancelled: `faltauno_partidos_cancelled_total`
   - **Visualization**: Bar gauge (horizontal)
   - **Show as**: Compare metrics

**Row 2: User Engagement**

5. **Daily Active Users (Logins)**
   - **Query**: `increase(faltauno_user_logins_total[24h])`
   - **Visualization**: Time series
   - **Time range**: Last 7 days
   - **Interval**: 1 day

6. **User Registration Trend**
   - **Query**: `increase(faltauno_user_registrations_total[1d])`
   - **Visualization**: Time series
   - **Unit**: registrations/day

7. **Top Pages Visited**
   - **Query**: `topk(10, sum by (path) (increase(faltauno_page_views_total[1h])))`
   - **Visualization**: Bar chart
   - **Orientation**: Horizontal

**Row 3: Partido Operations (Backend)**

8. **Backend Partido Operations**
   - **Queries** (add multiple):
     - Created: `increase(partido_creados_total[1h])`
     - Updated: `increase(partido_actualizados_total[1h])`
     - Confirmed: `increase(partido_confirmados_total[1h])`
     - Cancelled: `increase(partido_cancelados_total[1h])`
   - **Visualization**: Time series (stacked)

9. **Inscripcion Activity**
   - **Queries**:
     - Created: `increase(inscripcion_creada_total[1h])`
     - Accepted: `increase(inscripcion_aceptada_total[1h])`
     - Rejected: `increase(inscripcion_rechazada_total[1h])`
   - **Visualization**: Time series (lines)

---

### Dashboard 3: Performance Deep Dive

**Purpose**: Detailed performance monitoring and optimization

#### Panels to Add:

**Row 1: API Performance**

1. **API Response Time Percentiles**
   - **Queries** (add multiple):
     - p50: `histogram_quantile(0.50, rate(faltauno_api_duration_ms_bucket[5m]))`
     - p95: `histogram_quantile(0.95, rate(faltauno_api_duration_ms_bucket[5m]))`
     - p99: `histogram_quantile(0.99, rate(faltauno_api_duration_ms_bucket[5m]))`
   - **Visualization**: Time series
   - **Legend**: p50, p95, p99

2. **Slowest Endpoints**
   - **Query**: 
     ```promql
     topk(10, 
       avg by (endpoint) (
         histogram_quantile(0.95, rate(faltauno_api_duration_ms_bucket[5m]))
       )
     )
     ```
   - **Visualization**: Bar chart (horizontal)
   - **Unit**: ms

3. **Requests by Endpoint**
   - **Query**: `sum by (endpoint) (rate(faltauno_api_calls_total[5m]))`
   - **Visualization**: Table
   - **Columns**: Endpoint, Requests/sec

**Row 2: Web Vitals**

4. **Largest Contentful Paint (LCP)**
   - **Query**: `faltauno_web_vitals{name="LCP"}`
   - **Visualization**: Stat
   - **Unit**: ms
   - **Thresholds**: Green < 2500ms, Yellow < 4000ms, Red > 4000ms

5. **First Input Delay (FID)**
   - **Query**: `faltauno_web_vitals{name="FID"}`
   - **Visualization**: Stat
   - **Unit**: ms
   - **Thresholds**: Green < 100ms, Yellow < 300ms, Red > 300ms

6. **Cumulative Layout Shift (CLS)**
   - **Query**: `faltauno_web_vitals{name="CLS"}`
   - **Visualization**: Stat
   - **Unit**: score
   - **Thresholds**: Green < 0.1, Yellow < 0.25, Red > 0.25

7. **All Web Vitals Over Time**
   - **Query**: `faltauno_web_vitals`
   - **Visualization**: Time series
   - **Legend**: Group by name (LCP, FID, CLS, TTFB, etc.)

**Row 3: Infrastructure**

8. **Backend Memory Usage**
   - **Query**: `jvm_memory_used_bytes{job="faltauno-backend"} / 1024 / 1024`
   - **Visualization**: Time series
   - **Unit**: MB

9. **Backend Thread Count**
   - **Query**: `jvm_threads_live{job="faltauno-backend"}`
   - **Visualization**: Time series
   - **Unit**: threads

10. **Database Connections**
    - **Queries**:
      - Active: `hikaricp_connections_active{job="faltauno-backend"}`
      - Idle: `hikaricp_connections_idle{job="faltauno-backend"}`
      - Total: `hikaricp_connections{job="faltauno-backend"}`
    - **Visualization**: Time series (stacked area)

---

### Dashboard 4: Error Tracking & Debugging

**Purpose**: Identify and diagnose issues quickly

#### Panels to Add:

**Row 1: Error Overview**

1. **Total Errors (Last Hour)**
   - **Query**: `sum(increase(faltauno_errors_total[1h]))`
   - **Visualization**: Stat (big number)
   - **Color**: Red scale

2. **Error Rate Over Time**
   - **Query**: `sum(rate(faltauno_errors_total[5m]))`
   - **Visualization**: Time series
   - **Unit**: errors/sec
   - **Fill**: Under line

3. **Errors by Type**
   - **Query**: `sum by (type) (increase(faltauno_errors_total[1h]))`
   - **Visualization**: Pie chart
   - **Legend**: Show type labels

4. **Errors by Component**
   - **Query**: `sum by (component) (increase(faltauno_errors_total[1h]))`
   - **Visualization**: Bar chart

**Row 2: API Errors**

5. **API Errors by Status Code**
   - **Query**: `sum by (status) (increase(faltauno_api_calls_total{status=~"[45].."}[1h]))`
   - **Visualization**: Table
   - **Columns**: Status Code, Count
   - **Sort**: By count descending

6. **API Error Rate by Endpoint**
   - **Query**: 
     ```promql
     sum by (endpoint) (
       rate(faltauno_api_calls_total{status=~"[45].."}[5m])
     )
     ```
   - **Visualization**: Bar chart (horizontal)
   - **Show**: Top 10

7. **4xx vs 5xx Errors**
   - **Queries**:
     - 4xx: `sum(rate(faltauno_api_calls_total{status=~"4.."}[5m]))`
     - 5xx: `sum(rate(faltauno_api_calls_total{status=~"5.."}[5m]))`
   - **Visualization**: Time series
   - **Colors**: Yellow (4xx), Red (5xx)

**Row 3: WebSocket Issues**

8. **WebSocket Connection Status**
   - **Query**: `faltauno_websocket_connected`
   - **Visualization**: Time series
   - **Expected**: Should be stable > 0

9. **WebSocket Message Rate**
   - **Query**: `sum(rate(faltauno_websocket_messages_total[5m]))`
   - **Visualization**: Time series
   - **Unit**: messages/sec

---

### Dashboard 5: Real-time Monitoring (Live View)

**Purpose**: Live operations monitoring with auto-refresh

**Dashboard Settings:**
- **Auto-refresh**: 5 seconds
- **Time range**: Last 15 minutes

#### Panels to Add:

1. **Live API Traffic**
   - **Query**: `sum(rate(faltauno_api_calls_total[30s]))`
   - **Visualization**: Time series
   - **Refresh**: 5s

2. **Live Error Count**
   - **Query**: `sum(increase(faltauno_errors_total[1m]))`
   - **Visualization**: Stat
   - **Refresh**: 5s

3. **Current Response Time**
   - **Query**: `avg(rate(faltauno_api_duration_ms_sum[30s]) / rate(faltauno_api_duration_ms_count[30s]))`
   - **Visualization**: Gauge
   - **Refresh**: 5s

4. **Active Users Now**
   - **Query**: `increase(faltauno_user_logins_total[5m])`
   - **Visualization**: Stat
   - **Refresh**: 5s

---

## 🎨 Dashboard Creation Best Practices

### 1. Organization
- **Group related panels in rows** (use "Add row" option)
- **Name rows clearly**: "Traffic Metrics", "User Activity", etc.
- **Order by importance**: Most critical metrics at the top

### 2. Visual Design
- **Use consistent colors**: Green = good, Yellow = warning, Red = critical
- **Set thresholds** on Stat/Gauge panels for color coding
- **Choose appropriate visualizations**:
  - Time series: Trends over time
  - Stat: Single number (current value or total)
  - Gauge: Current value with min/max
  - Bar chart: Compare multiple items
  - Pie chart: Proportions/distribution
  - Table: Detailed breakdown

### 3. Panel Configuration
- **Always add units** (ms, req/s, %, etc.)
- **Add descriptions** (click panel → Edit → Panel options → Description)
- **Set meaningful titles**
- **Configure legends** (show/hide, position, values)

### 4. Time Ranges
- **Use appropriate intervals**:
  - Real-time: Last 15m with 5s refresh
  - Operational: Last 6h
  - Daily review: Last 24h
  - Weekly trends: Last 7d
- **Add time range override** to specific panels if needed

### 5. Variables (Advanced)
Create dashboard variables for filtering:

1. Click ⚙️ (Dashboard settings) → **Variables** → **Add variable**
2. Examples:
   - **Environment**: Query = `label_values(environment)`
   - **Endpoint**: Query = `label_values(faltauno_api_calls_total, endpoint)`
   - **Tipo Partido**: Query = `label_values(faltauno_partidos_created_total, tipo)`

Then use in queries: `faltauno_api_calls_total{endpoint="$endpoint"}`

---

## 🚀 Step-by-Step: Create Application Health Dashboard

Let me walk you through creating the most important dashboard:

### Step 1: Create Dashboard
1. Go to https://faltauno.grafana.net
2. Click **Dashboards** → **New** → **New Dashboard**
3. Click gear icon (⚙️) → **Settings**
4. Set **Name**: "FaltaUno - Application Health"
5. Set **Auto-refresh**: 30s
6. Click **Save** → **Apply**

### Step 2: Add Row 1 - Traffic
1. Click **Add** → **Row**
2. Name it: "Traffic Metrics"
3. Click **Add** → **Visualization**

**Panel A: API Request Rate**
- Query: `sum(rate(faltauno_api_calls_total[5m]))`
- Title: "API Request Rate"
- Unit: "req/s"
- Visualization: Time series
- Click **Apply**

**Panel B: Success Rate**
- Click **Add** → **Visualization**
- Query: `sum(rate(faltauno_api_calls_total{status=~"2.."}[5m])) / sum(rate(faltauno_api_calls_total[5m])) * 100`
- Title: "API Success Rate"
- Unit: Percent (0-100)
- Visualization: Stat
- **Thresholds**:
  - Base: Red
  - 90: Yellow
  - 95: Green
- Click **Apply**

**Panel C: Error Rate**
- Click **Add** → **Visualization**
- Query: `sum(rate(faltauno_api_calls_total{status=~"[45].."}[5m]))`
- Title: "API Errors"
- Unit: req/s
- Visualization: Time series
- Color: Red
- Click **Apply**

### Step 3: Add Row 2 - Performance
1. Click **Add** → **Row**
2. Name it: "Performance"

**Panel D: Response Time**
- Click **Add** → **Visualization**
- Add 3 queries:
  - Legend "p50": `histogram_quantile(0.50, rate(faltauno_api_duration_ms_bucket[5m]))`
  - Legend "p95": `histogram_quantile(0.95, rate(faltauno_api_duration_ms_bucket[5m]))`
  - Legend "p99": `histogram_quantile(0.99, rate(faltauno_api_duration_ms_bucket[5m]))`
- Title: "API Response Time (Percentiles)"
- Unit: ms
- Click **Apply**

### Step 4: Add Row 3 - Users
1. Click **Add** → **Row**
2. Name it: "User Activity"

**Panel E: WebSocket Connections**
- Query: `faltauno_websocket_connected`
- Title: "Active WebSocket Connections"
- Visualization: Gauge
- Max: 1000

**Panel F: Logins**
- Query: `increase(faltauno_user_logins_total[1h])`
- Title: "Logins (Last Hour)"
- Visualization: Stat

### Step 5: Save & Share
1. Click **Save** (💾) icon
2. Add description: "Main application health monitoring dashboard"
3. Click **Save**
4. Click **Share** icon to get shareable link

---

## 📱 Mobile & TV Display

### Create TV Dashboard
1. Create simplified dashboard with large fonts
2. Use fewer panels (6-8 max)
3. Set auto-refresh: 30s
4. Use **Kiosk mode** URL: Add `?kiosk` to dashboard URL
   - Example: `https://faltauno.grafana.net/d/abc123?kiosk`

### Mobile Optimization
- Use Grafana mobile app
- Create mobile-specific dashboards with fewer panels
- Focus on Stat/Gauge visualizations (easier to read on small screens)

---

## 🔗 Dashboard Links & Navigation

### Add Links Between Dashboards
1. Edit dashboard → Settings (⚙️)
2. Click **Links** → **Add dashboard link**
3. Add your other dashboards:
   - Application Health
   - Business Metrics
   - Performance Deep Dive
   - Error Tracking
4. Links appear at top of dashboard for easy navigation

---

## 💾 Export & Backup

### Export Dashboard JSON
1. Open dashboard
2. Click **Share** icon → **Export**
3. Click **Save to file**
4. Keep JSON backup in your repository

### Import Dashboard
1. **Dashboards** → **New** → **Import**
2. Upload JSON file or paste JSON
3. Click **Load**

---

## ⚡ Quick Import: Pre-configured Dashboard

Here's a complete dashboard JSON you can import immediately:

1. Go to **Dashboards** → **Import**
2. Paste this JSON:

```json
{
  "dashboard": {
    "title": "FaltaUno - Quick Start",
    "panels": [
      {
        "title": "API Request Rate",
        "targets": [
          {
            "expr": "sum(rate(faltauno_api_calls_total[5m]))",
            "legendFormat": "Requests/sec"
          }
        ],
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0}
      },
      {
        "title": "Success Rate",
        "targets": [
          {
            "expr": "sum(rate(faltauno_api_calls_total{status=~\"2..\"}[5m])) / sum(rate(faltauno_api_calls_total[5m])) * 100"
          }
        ],
        "type": "stat",
        "gridPos": {"h": 8, "w": 6, "x": 12, "y": 0}
      },
      {
        "title": "Active Users",
        "targets": [
          {
            "expr": "increase(faltauno_user_logins_total[1h])"
          }
        ],
        "type": "stat",
        "gridPos": {"h": 8, "w": 6, "x": 18, "y": 0}
      }
    ]
  }
}
```

3. Click **Load** → **Import**

---

## 🎓 Next Steps

1. **Create your first dashboard** using the Application Health template above
2. **Test it**: Log in to your app, create some partidos, trigger metrics
3. **Verify data appears** in Grafana within 1-2 minutes
4. **Add more panels** based on what's important to you
5. **Set up alerts** on critical metrics (covered in OBSERVABILITY_GUIDE.md)
6. **Share dashboards** with your team

Your dashboards will now automatically update as users interact with your application! 📊✨
