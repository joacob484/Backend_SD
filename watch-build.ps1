#!/usr/bin/env pwsh
# Quick build watcher for Cloud Build

Write-Host "🔍 Monitoring Cloud Build..." -ForegroundColor Cyan
Write-Host ""

$lastBuildId = ""

while ($true) {
    $builds = gcloud builds list --project=master-might-274420 --limit=1 --format="value(id,status)" 2>$null
    
    if ($builds) {
        $parts = $builds -split '\s+'
        $buildId = $parts[0]
        $status = $parts[1]
        
        if ($buildId -ne $lastBuildId) {
            Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
            Write-Host "🆕 NEW BUILD DETECTED!" -ForegroundColor Green
            Write-Host "Build ID: $buildId" -ForegroundColor White
            Write-Host "Status: $status" -ForegroundColor $(if ($status -eq "WORKING") { "Cyan" } elseif ($status -eq "SUCCESS") { "Green" } else { "Red" })
            Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
            Write-Host ""
            
            if ($status -eq "WORKING") {
                Write-Host "📊 Streaming logs..." -ForegroundColor Cyan
                gcloud builds log $buildId --project=master-might-274420 --stream
            }
            
            $lastBuildId = $buildId
        }
        elseif ($status -eq "WORKING") {
            Write-Host "⏳ Build $buildId still running... ($status)" -ForegroundColor Cyan
        }
    }
    
    Start-Sleep -Seconds 5
}
