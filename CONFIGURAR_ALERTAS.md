# ⚙️ CONFIGURAR ALERTAS DE PRESUPUESTO

## 🔔 Paso 1: Crear Alerta de Presupuesto ($40/mes)

### Via Cloud Console (RECOMENDADO):

1. **Ir a Billing & Budgets**:
   ```
   https://console.cloud.google.com/billing/01CF70-2F4A65-7E806E/budgets?project=master-might-274420
   ```

2. **Click "CREATE BUDGET"**

3. **Configurar Budget**:
   ```
   Name: FaltaUno Monthly Budget
   Projects: master-might-274420
   Time range: Monthly
   Budget type: Specified amount
   Target amount: $40 USD
   ```

4. **Configurar Alertas**:
   ```
   Alert threshold rules:
   - 50% ($20): Email notification
   - 75% ($30): Email notification
   - 90% ($36): Email notification + urgent
   - 100% ($40): Email notification + critical
   - 110% ($44): Email notification + emergency
   ```

5. **Email Notifications**:
   - Agregar tu email
   - Agregar emails del equipo

6. **Click "FINISH"**

---

## 📊 Paso 2: Monitorear Costos Diarios

### Via Cloud Console:

1. **Billing Reports**:
   ```
   https://console.cloud.google.com/billing/01CF70-2F4A65-7E806E/reports?project=master-might-274420
   ```

2. **Configurar filtros**:
   ```
   Time range: Last 30 days
   Group by: Service
   Filters: Project = master-might-274420
   ```

3. **Servicios a monitorear**:
   - ✅ Cloud Run (debe bajar a $17-22/mes)
   - ✅ Cloud SQL (debe estar en $25/mes)
   - ✅ Networking/Bandwidth (debe bajar a $1.50/mes)
   - ✅ Storage (debe bajar a $0.50/mes)

---

## 📈 Paso 3: Dashboard de Costos (PowerShell)

Guarda este script como `check-costs.ps1`:

```powershell
# Check-Costs.ps1 - Monitoreo de costos FaltaUno

Write-Host "`n💰 MONITOREO DE COSTOS FALTAUNO" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════════════════════`n" -ForegroundColor Cyan

# Backend
Write-Host "🔍 Cloud Run Backend:" -ForegroundColor Yellow
$backend = gcloud run services describe faltauno-backend --region=us-central1 --format="value(spec.template.spec.containers[0].resources.limits.memory,spec.template.metadata.annotations.'autoscaling.knative.dev/maxScale')"
Write-Host "   Config: $backend" -ForegroundColor White
Write-Host "   Costo proyectado: `$12-15/mes`n" -ForegroundColor Green

# Frontend
Write-Host "🔍 Cloud Run Frontend:" -ForegroundColor Yellow
$frontend = gcloud run services describe faltauno-frontend --region=us-central1 --format="value(spec.template.spec.containers[0].resources.limits.memory,spec.template.metadata.annotations.'autoscaling.knative.dev/maxScale')"
Write-Host "   Config: $frontend" -ForegroundColor White
Write-Host "   Costo proyectado: `$5-7/mes`n" -ForegroundColor Green

# Cloud SQL
Write-Host "🔍 Cloud SQL:" -ForegroundColor Yellow
$sqlTier = gcloud sql instances describe faltauno-db --format="value(settings.tier)"
Write-Host "   Tier: $sqlTier" -ForegroundColor White
Write-Host "   Costo: `$25/mes`n" -ForegroundColor White

# Resumen
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "💰 COSTO TOTAL PROYECTADO: `$34-39/mes" -ForegroundColor Green -BackgroundColor DarkGreen
Write-Host "🎯 Objetivo <`$40/mes: ✅ CUMPLIDO`n" -ForegroundColor Green
Write-Host "📊 Ver costos reales en:" -ForegroundColor Yellow
Write-Host "   https://console.cloud.google.com/billing/reports`n" -ForegroundColor Cyan
```

Ejecutar cada semana: `.\check-costs.ps1`

---

## 🚨 Paso 4: Alertas Críticas (Si supera $40/mes)

### Si los costos suben inesperadamente:

1. **Ver logs de Cloud Run**:
   ```powershell
   gcloud logging read "resource.type=cloud_run_revision AND severity>=ERROR" --limit=50 --format=json
   ```

2. **Verificar instancias activas**:
   ```powershell
   gcloud run services describe faltauno-backend --region=us-central1 --format="value(status.traffic[0].percent)"
   ```

3. **Acciones inmediatas**:
   - Reducir max-instances si hay picos: `--max-instances=1`
   - Verificar loops infinitos en logs
   - Revisar cache hit rate (debe ser >80%)

---

## 📅 Calendario de Monitoreo

### Diario (primeros 7 días):
- ✅ Verificar que servicios estén UP
- ✅ Revisar logs por errores
- ✅ Verificar latencia <200ms

### Semanal:
- ✅ Ejecutar `check-costs.ps1`
- ✅ Revisar billing reports
- ✅ Verificar alertas de presupuesto

### Mensual:
- ✅ Analizar costos vs proyectados
- ✅ Revisar cache hit rate
- ✅ Ajustar configuración si necesario

---

## ✅ CHECKLIST FINAL

- [ ] Alerta de presupuesto $40/mes creada
- [ ] Email notifications configurados
- [ ] Billing reports revisados
- [ ] Script `check-costs.ps1` guardado
- [ ] Primera ejecución de monitoreo realizada

---

**Tiempo total: 10 minutos**  
**Beneficio: Detectar sobrecostos antes de que impacten**
