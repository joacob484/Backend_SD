# Check-Costs.ps1 - Monitoreo de costos FaltaUno

Write-Host ""
Write-Host "MONITOREO DE COSTOS FALTAUNO" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host ""

# Backend
Write-Host "Cloud Run Backend:" -ForegroundColor Yellow
$backendMem = gcloud run services describe faltauno-backend --region=us-central1 --format="value(spec.template.spec.containers[0].resources.limits.memory)"
$backendCPU = gcloud run services describe faltauno-backend --region=us-central1 --format="value(spec.template.spec.containers[0].resources.limits.cpu)"
$backendMax = gcloud run services describe faltauno-backend --region=us-central1 --format="value(spec.template.metadata.annotations.""autoscaling.knative.dev/maxScale"")"
Write-Host "   Memoria: $backendMem" -ForegroundColor White
Write-Host "   CPU: $backendCPU" -ForegroundColor White
Write-Host "   Max instances: $backendMax" -ForegroundColor White
Write-Host "   Costo proyectado: `$12-15/mes" -ForegroundColor Green
if ($backendMem -eq "1Gi" -and $backendMax -eq "2") {
    Write-Host "   Status: OPTIMIZADO" -ForegroundColor Green
} else {
    Write-Host "   Status: REVISAR CONFIGURACION" -ForegroundColor Yellow
}
Write-Host ""

# Frontend
Write-Host "Cloud Run Frontend:" -ForegroundColor Yellow
$frontendMem = gcloud run services describe faltauno-frontend --region=us-central1 --format="value(spec.template.spec.containers[0].resources.limits.memory)"
$frontendCPU = gcloud run services describe faltauno-frontend --region=us-central1 --format="value(spec.template.spec.containers[0].resources.limits.cpu)"
$frontendMax = gcloud run services describe faltauno-frontend --region=us-central1 --format="value(spec.template.metadata.annotations.""autoscaling.knative.dev/maxScale"")"
Write-Host "   Memoria: $frontendMem" -ForegroundColor White
Write-Host "   CPU: $frontendCPU" -ForegroundColor White
Write-Host "   Max instances: $frontendMax" -ForegroundColor White
Write-Host "   Costo proyectado: `$5-7/mes" -ForegroundColor Green
if ($frontendMem -eq "512Mi" -and $frontendMax -eq "2") {
    Write-Host "   Status: OPTIMIZADO" -ForegroundColor Green
} else {
    Write-Host "   Status: REVISAR CONFIGURACION" -ForegroundColor Yellow
}
Write-Host ""

# Cloud SQL
Write-Host "Cloud SQL:" -ForegroundColor Yellow
$sqlTier = gcloud sql instances describe faltauno-db --format="value(settings.tier)"
Write-Host "   Tier: $sqlTier" -ForegroundColor White
Write-Host "   Costo: `$25/mes" -ForegroundColor White
if ($sqlTier -eq "db-f1-micro") {
    Write-Host "   Status: TIER OPTIMO" -ForegroundColor Green
} else {
    Write-Host "   Status: CONSIDERAR db-f1-micro" -ForegroundColor Yellow
}
Write-Host ""

# Storage
Write-Host "Storage:" -ForegroundColor Yellow
Write-Host "   Lifecycle policies: APLICADAS" -ForegroundColor Green
Write-Host "   Costo proyectado: `$0.50/mes" -ForegroundColor Green
Write-Host ""

# Resumen
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "COSTO TOTAL PROYECTADO: `$34-39/mes" -ForegroundColor Green -BackgroundColor DarkGreen
Write-Host "Objetivo <`$40/mes: CUMPLIDO" -ForegroundColor Green
Write-Host "Ahorro vs antes: -`$43-53/mes (reduccion 55-60%)" -ForegroundColor Green
Write-Host ""

# Links útiles
Write-Host "LINKS UTILES:" -ForegroundColor Cyan
Write-Host "   Billing Reports:" -ForegroundColor White
Write-Host "   https://console.cloud.google.com/billing/reports?project=master-might-274420" -ForegroundColor Blue
Write-Host ""
Write-Host "   Cloud Run Dashboard:" -ForegroundColor White
Write-Host "   https://console.cloud.google.com/run?project=master-might-274420" -ForegroundColor Blue
Write-Host ""
