# Script simple para verificar builds de Cloud Build
$PROJECT_ID = "master-might-274420"

Write-Host "Verificando ultimos builds..." -ForegroundColor Cyan
Write-Host ""

# Listar ultimos 5 builds
Write-Host "Ultimos 5 builds:" -ForegroundColor Yellow
gcloud builds list --project=$PROJECT_ID --limit=5 --format="table(id,createTime,duration,status)"

Write-Host ""
Write-Host "Obteniendo ID del ultimo build..." -ForegroundColor Cyan

# Obtener ultimo build
$LAST_BUILD = gcloud builds list --project=$PROJECT_ID --limit=1 --format=json | ConvertFrom-Json

if ($null -eq $LAST_BUILD -or $LAST_BUILD.Count -eq 0) {
    Write-Host "No se encontraron builds" -ForegroundColor Red
    exit 1
}

$BUILD_ID = $LAST_BUILD[0].id
$STATUS = $LAST_BUILD[0].status
$CREATE_TIME = $LAST_BUILD[0].createTime

Write-Host ""
Write-Host "Build ID: $BUILD_ID" -ForegroundColor Green
Write-Host "Estado: $STATUS" -ForegroundColor Green
Write-Host "Creado: $CREATE_TIME" -ForegroundColor Green
Write-Host ""

# Analizar duracion
if ($LAST_BUILD[0].timing) {
    $totalTime = $LAST_BUILD[0].timing.total
    Write-Host "Duracion total: $totalTime" -ForegroundColor Cyan
    
    # Extraer segundos
    if ($totalTime -match '(\d+)s') {
        $seconds = [int]$matches[1]
        Write-Host "Segundos: $seconds" -ForegroundColor Cyan
        
        if ($seconds -lt 60) {
            Write-Host ""
            Write-Host "ALERTA: Build duro menos de 1 minuto!" -ForegroundColor Red
            Write-Host "Maven NO compilo el codigo" -ForegroundColor Red
        } elseif ($seconds -lt 300) {
            Write-Host ""
            Write-Host "ADVERTENCIA: Build duro solo $seconds segundos" -ForegroundColor Yellow
            Write-Host "Un build completo deberia tardar 5-10 minutos" -ForegroundColor Yellow
        } else {
            Write-Host ""
            Write-Host "OK: Duracion normal para un build completo" -ForegroundColor Green
        }
    }
}

Write-Host ""
Write-Host "Guardando logs del build..." -ForegroundColor Cyan

# Guardar logs
$logFile = "build-$BUILD_ID-logs.txt"
gcloud builds log $BUILD_ID --project=$PROJECT_ID > $logFile

Write-Host "Logs guardados en: $logFile" -ForegroundColor Green
Write-Host ""

# Buscar palabras clave en los logs
Write-Host "Analizando logs..." -ForegroundColor Cyan
$logs = Get-Content $logFile -Raw

Write-Host ""
Write-Host "Verificando evidencia de compilacion Maven:" -ForegroundColor Yellow
Write-Host ""

$foundDownloading = $logs -match "Downloading from central"
$foundCompiling = $logs -match "Compiling .+ source files"
$foundBuildSuccess = $logs -match "BUILD SUCCESS"
$foundBuildingJar = $logs -match "Building jar"

Write-Host "  [1] Descargando dependencias: " -NoNewline
if ($foundDownloading) { Write-Host "SI" -ForegroundColor Green } else { Write-Host "NO" -ForegroundColor Red }

Write-Host "  [2] Compilando codigo: " -NoNewline
if ($foundCompiling) { Write-Host "SI" -ForegroundColor Green } else { Write-Host "NO" -ForegroundColor Red }

Write-Host "  [3] Build exitoso: " -NoNewline
if ($foundBuildSuccess) { Write-Host "SI" -ForegroundColor Green } else { Write-Host "NO" -ForegroundColor Red }

Write-Host "  [4] Generando JAR: " -NoNewline
if ($foundBuildingJar) { Write-Host "SI" -ForegroundColor Green } else { Write-Host "NO" -ForegroundColor Red }

Write-Host ""
Write-Host "Resumen:" -ForegroundColor Yellow

if ($foundCompiling -and $foundBuildSuccess) {
    Write-Host "  Maven SI compilo la aplicacion correctamente" -ForegroundColor Green
} else {
    Write-Host "  Maven NO compilo la aplicacion" -ForegroundColor Red
    Write-Host ""
    Write-Host "  Posibles causas:" -ForegroundColor Yellow
    Write-Host "  - Docker usa cache antiguo" -ForegroundColor White
    Write-Host "  - Build fallo antes de compilar" -ForegroundColor White
    Write-Host "  - Problema en el Dockerfile" -ForegroundColor White
}

Write-Host ""
Write-Host "Ver logs completos:" -ForegroundColor Cyan
Write-Host "  https://console.cloud.google.com/cloud-build/builds/$BUILD_ID`?project=$PROJECT_ID"
Write-Host ""
Write-Host "O ejecuta: gcloud builds log $BUILD_ID --project=$PROJECT_ID" -ForegroundColor Cyan
Write-Host ""
