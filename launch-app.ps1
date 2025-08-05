# Script de lancement simple pour l'application Fortnite Pronos
Write-Host "Demarrage de l'application..." -ForegroundColor Green

# Arreter les processus existants
$processes8080 = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | ForEach-Object { Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue }
$processes8081 = Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue | ForEach-Object { Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue }
$processes4200 = Get-NetTCPConnection -LocalPort 4200 -ErrorAction SilentlyContinue | ForEach-Object { Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue }

if ($processes8080) { $processes8080 | Stop-Process -Force -ErrorAction SilentlyContinue }
if ($processes8081) { $processes8081 | Stop-Process -Force -ErrorAction SilentlyContinue }
if ($processes4200) { $processes4200 | Stop-Process -Force -ErrorAction SilentlyContinue }

Start-Sleep -Seconds 2

# Configuration JVM
$env:MAVEN_OPTS = "-Xms2g -Xmx4g -XX:+UseG1GC"

Write-Host "Demarrage du backend sur port 8080..." -ForegroundColor Yellow
Start-Process -FilePath "cmd" -ArgumentList "/c", "mvn spring-boot:run -Dserver.port=8080" -WindowStyle Minimized

Start-Sleep -Seconds 5

Write-Host "Demarrage du frontend sur port 4200..." -ForegroundColor Yellow
Set-Location "frontend"
Start-Process -FilePath "cmd" -ArgumentList "/c", "npm start" -WindowStyle Minimized
Set-Location ".."

Write-Host "Application en cours de demarrage..." -ForegroundColor Green
Write-Host "Frontend: http://localhost:4200" -ForegroundColor Cyan
Write-Host "Backend: http://localhost:8080" -ForegroundColor Cyan

# Ouvrir le navigateur
Start-Sleep -Seconds 10
Start-Process "http://localhost:4200"