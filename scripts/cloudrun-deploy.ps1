# PowerShell equivalent of cloudrun-deploy.sh

param (
    [string]$EncodedSpringPass,
    [string]$RedisHost,
    [string]$PostgresHost,
    [string]$CloudSqlInstance,
    [string]$SpringUrl,
    [string]$SpringUser = "app"
)

# Section 1: Decode password
$SpringPass = ""
if ($EncodedSpringPass) {
    try {
        $SpringPass = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($EncodedSpringPass))
    } catch {
        Write-Host "Failed to decode password, proceeding without decoding."
    }
}

# Fallback to Secret Manager if password is empty
if (-not $SpringPass) {
    Write-Host "Decoded password empty or invalid base64 — attempting to read from Secret Manager 'db-password'"
    $ProjectId = gcloud config get-value project 2>$null
    if ($ProjectId) {
        $SpringPass = gcloud secrets versions access latest --secret=db-password --project=$ProjectId 2>$null
    } else {
        $SpringPass = gcloud secrets versions access latest --secret=db-password 2>$null
    }
}

# Section 2: Synthesize SPRING_DATASOURCE_URL
if ($CloudSqlInstance) {
    $SpringUrl = "jdbc:postgresql://localhost:5432/faltauno_db?cloudSqlInstance=$CloudSqlInstance`&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
} else {
    $PHost = if ($PostgresHost) { $PostgresHost } else { "localhost" }
    $SpringUrl = "jdbc:postgresql://$PHost:5432/faltauno_db"
}
Write-Host "SPRING_DATASOURCE_URL: $SpringUrl"

# Section 3: Write environment variables to a file
$EnvFile = "cloudrun-envs.yaml"
@{
    SPRING_DATASOURCE_PASSWORD = $SpringPass
    REDIS_HOST = $RedisHost
    POSTGRES_HOST = $PostgresHost
    SPRING_DATASOURCE_USERNAME = $SpringUser
    SPRING_DATASOURCE_URL = $SpringUrl
} | ForEach-Object {
    "`"$($_.Key)`": `"$($_.Value)`""
} | Out-File -Encoding utf8 $EnvFile

Write-Host "Environment variables written to $EnvFile"

# Section 4: Deploy to Cloud Run
$ImageTag = if ($Env:IMAGE_TAG) { $Env:IMAGE_TAG } elseif ($Env:BUILD_ID) { $Env:BUILD_ID } elseif ($Env:SHORT_SHA) { $Env:SHORT_SHA } else { "" }
if (-not $ImageTag) {
    Write-Error "ERROR: Could not determine image tag (IMAGE_TAG, BUILD_ID, and SHORT_SHA are empty)"
    exit 1
}

$CloudSqlFlag = if ($CloudSqlInstance) { "--add-cloudsql-instances $CloudSqlInstance" } else { "" }

Write-Host "Deploying revision with no-traffic to allow health checks"
$ServiceExists = gcloud run services describe faltauno-backend --region us-central1 --platform managed 2>$null
if ($ServiceExists) {
    Write-Host "Service exists — deploying revision with --no-traffic"
    $Revision = gcloud run deploy faltauno-backend `
        --image gcr.io/$ProjectId/faltauno-backend:$ImageTag `
        --region us-central1 `
        --platform managed `
        --no-traffic `
        $CloudSqlFlag `
        --env-vars-file $EnvFile `
        --format="value(status.latestCreatedRevisionName)"
} else {
    Write-Host "Service does not exist — creating service (initial deploy will receive traffic)"
    $Revision = gcloud run deploy faltauno-backend `
        --image gcr.io/$ProjectId/faltauno-backend:$ImageTag `
        --region us-central1 `
        --platform managed `
        $CloudSqlFlag `
        --env-vars-file $EnvFile `
        --format="value(status.latestCreatedRevisionName)"
}

Write-Host "Created revision: $Revision"
Write-Host "Deployment script completed successfully."