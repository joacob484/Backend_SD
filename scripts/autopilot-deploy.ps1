<#
Autopilot deploy helper (PowerShell)

Usage (from Cloud Shell or any machine with gcloud authenticated):

# Example:

Notes:
- Run this in Cloud Shell to avoid local gcloud client workspace substitution scanning problems.
- The script assumes your Artifact Registry repository is: us-central1-docker.pkg.dev/<PROJECT>/faltauno/faltauno-backend
- The script runs the in-repo Cloud Build retag config: Back/Backend_SD/cloudbuild-retag.yaml with --no-source. Make sure that file is present and correct.
- This script does not modify secrets; it only references them in the deployment command.
#>

param(
    [Parameter(Mandatory=$true)] [string]$Project,
    [Parameter(Mandatory=$true)] [string]$ImageDigest,
    [Parameter(Mandatory=$true)] [string]$TargetTag,
    [Parameter(Mandatory=$true)] [string]$ServiceName,
    [string]$Region = "us-central1",
    [string]$ConfigPath = "Back/Backend_SD/cloudbuild-retag.yaml",
    [string[]]$EnvVars = @(),
    [string[]]$Secrets = @(),
    [switch]$PromoteAfterSmokeTest
n)

function Join-FlagList {
    param([string[]]$arr, [string]$sep)
    if ($arr.Count -eq 0) { return "" }
    return ($arr -join $sep)
}

Write-Host "Project: $Project"
Write-Host "Image digest: $ImageDigest"
Write-Host "Target tag: $TargetTag"
Write-Host "Service: $ServiceName (region: $Region)"
Write-Host "Cloud Build config path: $ConfigPath"

# 1) Run server-side retag build (no-source)
$substitutions = "_IMAGE_DIGEST=$ImageDigest,_TARGET_TAG=$TargetTag,PROJECT_ID=$Project"
Write-Host "Submitting Cloud Build to retag digest into Artifact Registry (server-side)..."
$cmd = "gcloud builds submit --project=$Project --config=$ConfigPath --no-source --substitutions=$substitutions"
Write-Host "Running: $cmd"
$sb = Start-Process -FilePath pwsh -ArgumentList "-NoProfile","-Command","$cmd" -Wait -NoNewWindow -PassThru
if ($sb.ExitCode -ne 0) {
    Write-Error "Cloud Build retag failed (exit code $($sb.ExitCode)). Check Cloud Build logs in the Console."
    exit $sb.ExitCode
}
Write-Host "Cloud Build retag succeeded."

# 2) Deploy Cloud Run revision with no-traffic
$repoImage = "us-central1-docker.pkg.dev/$Project/faltauno/faltauno-backend:$TargetTag"

$envArg = ""
if ($EnvVars.Count -gt 0) {
    $envArg = "--set-env-vars " + (Join-FlagList -arr $EnvVars -sep ",")
}

$secretsArg = ""
if ($Secrets.Count -gt 0) {
    # Build --update-secrets arg: key1=projects/...:version,key2=...
    $secretsArg = "--update-secrets " + (Join-FlagList -arr $Secrets -sep ",")
}

$deployCmd = "gcloud run deploy $ServiceName --project=$Project --region=$Region --platform=managed --image=$repoImage --no-traffic $envArg $secretsArg --quiet"
Write-Host "Deploying no-traffic revision with image: $repoImage"
Write-Host "Running: $deployCmd"
$sb2 = Start-Process -FilePath pwsh -ArgumentList "-NoProfile","-Command","$deployCmd" -Wait -NoNewWindow -PassThru
if ($sb2.ExitCode -ne 0) {
    Write-Error "gcloud run deploy failed (exit code $($sb2.ExitCode)). Check Cloud Run Console and logs."
    exit $sb2.ExitCode
}

Write-Host "No-traffic Cloud Run revision created."

# 3) Tail logs for the new revision for a short while (optional)
Write-Host "Tailing logs for 60s to observe startup. Press Ctrl+C to stop early."
$logCmd = "gcloud logging read 'resource.type=cloud_run_revision AND resource.labels.service_name=$ServiceName' --project=$Project --limit=50 --format='value(timestamp, textPayload)'"
Write-Host "Running a brief log check: $logCmd"
Start-Process -FilePath pwsh -ArgumentList "-NoProfile","-Command","$logCmd" -Wait -NoNewWindow

# 4) Run actuator smoke test if ci script exists
$smokeScriptPath = "Back/Backend_SD/ci/smoke-actuator.sh"
if (Test-Path $smokeScriptPath) {
    Write-Host "Found smoke test script at $smokeScriptPath. Running it..."
    # Ensure script is executable and run it (shell in Cloud Shell supports bash)
    $smokeCmd = "bash $smokeScriptPath $ServiceName $Project $Region"
    Write-Host "Running: $smokeCmd"
    $sb3 = Start-Process -FilePath pwsh -ArgumentList "-NoProfile","-Command","$smokeCmd" -Wait -NoNewWindow -PassThru
    if ($sb3.ExitCode -ne 0) {
        Write-Error "Smoke test failed (exit code $($sb3.ExitCode)). Not promoting."
        exit $sb3.ExitCode
    }
    Write-Host "Smoke test passed."

    if ($PromoteAfterSmokeTest) {
        Write-Host "Promoting revision to 100% traffic..."
        $promoteCmd = "gcloud run services update-traffic $ServiceName --project=$Project --to-latest --region=$Region --platform=managed --quiet"
        Write-Host "Running: $promoteCmd"
        $sb4 = Start-Process -FilePath pwsh -ArgumentList "-NoProfile","-Command","$promoteCmd" -Wait -NoNewWindow -PassThru
        if ($sb4.ExitCode -ne 0) {
            Write-Error "Promotion failed (exit code $($sb4.ExitCode))."
            exit $sb4.ExitCode
        }
        Write-Host "Promotion complete."
    } else {
        Write-Host "Promotion step skipped (PromoteAfterSmokeTest not provided)."
    }
} else {
    Write-Host "Smoke test script not found at $smokeScriptPath. Please run your smoke tests manually or add the script."
}

Write-Host "Autopilot flow finished. Check Cloud Run service and logs for final verification."
