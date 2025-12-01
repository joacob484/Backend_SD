# Setup script to add Java and Maven to system PATH permanently
# Run this script as Administrator if you want permanent environment variables

$JAVA_HOME = "C:\Users\augus\java\jdk-21.0.9+10"
$MAVEN_HOME = "C:\Users\augus\maven\apache-maven-3.9.10"

Write-Host "Setting up Java and Maven environment variables..." -ForegroundColor Cyan
Write-Host ""

# Set user environment variables (doesn't require admin)
try {
    [Environment]::SetEnvironmentVariable("JAVA_HOME", $JAVA_HOME, "User")
    [Environment]::SetEnvironmentVariable("MAVEN_HOME", $MAVEN_HOME, "User")
    
    # Get current user PATH
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
    
    # Add Java and Maven to PATH if not already there
    $javaPath = "$JAVA_HOME\bin"
    $mavenPath = "$MAVEN_HOME\bin"
    
    $pathsToAdd = @()
    if ($currentPath -notlike "*$javaPath*") {
        $pathsToAdd += $javaPath
    }
    if ($currentPath -notlike "*$mavenPath*") {
        $pathsToAdd += $mavenPath
    }
    
    if ($pathsToAdd.Count -gt 0) {
        $newPath = "$($pathsToAdd -join ';');$currentPath"
        [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
        Write-Host "Added to PATH:" -ForegroundColor Green
        foreach ($path in $pathsToAdd) {
            Write-Host "  - $path" -ForegroundColor White
        }
    } else {
        Write-Host "Paths already in environment" -ForegroundColor Green
    }
    
    Write-Host ""
    Write-Host "Environment variables set successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "JAVA_HOME = $JAVA_HOME" -ForegroundColor White
    Write-Host "MAVEN_HOME = $MAVEN_HOME" -ForegroundColor White
    Write-Host ""
    Write-Host "IMPORTANT: Close and reopen PowerShell for changes to take effect." -ForegroundColor Yellow
    Write-Host "After reopening, you can use 'mvn' and 'java' commands directly." -ForegroundColor Yellow
    
} catch {
    Write-Host "Error setting environment variables: $_" -ForegroundColor Red
    exit 1
}
