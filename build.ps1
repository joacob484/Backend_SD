# Build script for Falta Uno Backend
# This script sets up Java and Maven environment and builds the project

$JAVA_HOME = "C:\Users\augus\java\jdk-21.0.9+10"
$MAVEN_HOME = "C:\Users\augus\maven\apache-maven-3.9.10"

# Set environment variables for this session
$env:JAVA_HOME = $JAVA_HOME
$env:MAVEN_HOME = $MAVEN_HOME
$env:PATH = "$JAVA_HOME\bin;$MAVEN_HOME\bin;$env:PATH"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Falta Uno Backend - Build Script" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Java version:" -ForegroundColor Yellow
& "$JAVA_HOME\bin\java.exe" -version
Write-Host ""

Write-Host "Maven version:" -ForegroundColor Yellow
& "$MAVEN_HOME\bin\mvn.cmd" -version
Write-Host ""

Write-Host "Building project..." -ForegroundColor Yellow
Write-Host ""

& "$MAVEN_HOME\bin\mvn.cmd" clean install -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "======================================" -ForegroundColor Green
    Write-Host "BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "======================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "JAR file location:" -ForegroundColor Cyan
    Write-Host "  target\falta-uno-0.0.1-SNAPSHOT.jar" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "======================================" -ForegroundColor Red
    Write-Host "BUILD FAILED!" -ForegroundColor Red
    Write-Host "======================================" -ForegroundColor Red
    exit 1
}
