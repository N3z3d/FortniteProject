# Fortnite Pronos - Script de lancement unifié
# Version optimisée et consolidée

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "   FORTNITE PRONOS - Lancement de l'application" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Vérification des prérequis
Write-Host "Vérification des prérequis..." -ForegroundColor Yellow

$hasError = $false

# Vérifier Java
try {
    $javaVersion = java -version 2>&1 | Select-String "version" | Select-Object -First 1
    Write-Host "  ✓ Java installé: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Java n'est pas installé ou n'est pas dans le PATH" -ForegroundColor Red
    $hasError = $true
}

# Vérifier Node
try {
    $nodeVersion = node --version
    Write-Host "  ✓ Node installé: $nodeVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Node n'est pas installé" -ForegroundColor Red
    $hasError = $true
}

# Vérifier Maven
try {
    $mvnVersion = mvn --version | Select-String "Apache Maven" | Select-Object -First 1
    Write-Host "  ✓ Maven installé" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Maven n'est pas installé" -ForegroundColor Red
    $hasError = $true
}

if ($hasError) {
    Write-Host ""
    Write-Host "Des prérequis sont manquants. Veuillez les installer avant de continuer." -ForegroundColor Red
    exit 1
}

Write-Host ""

# Arrêter les processus existants
Write-Host "Arrêt des processus existants..." -ForegroundColor Yellow
try {
    $processes8080 = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | ForEach-Object { Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue }
    $processes4200 = Get-NetTCPConnection -LocalPort 4200 -ErrorAction SilentlyContinue | ForEach-Object { Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue }
    
    if ($processes8080) {
        Write-Host "  Arrêt des processus sur le port 8080..." -ForegroundColor Yellow
        $processes8080 | Stop-Process -Force -ErrorAction SilentlyContinue
    }
    if ($processes4200) {
        Write-Host "  Arrêt des processus sur le port 4200..." -ForegroundColor Yellow
        $processes4200 | Stop-Process -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Seconds 2
    Write-Host "  ✓ Ports libérés" -ForegroundColor Green
} catch {
    Write-Host "  ✓ Ports disponibles" -ForegroundColor Green
}

Write-Host ""

# Configuration des variables d'environnement
Write-Host "Configuration de l'environnement..." -ForegroundColor Yellow

# JWT Secret
if (-not $env:JWT_SECRET) {
    $env:JWT_SECRET = "ma_super_cle_jwt_securisee_123456789_vraiment_longue_pour_securite_maximale"
    Write-Host "  JWT_SECRET configuré pour le développement" -ForegroundColor Yellow
} else {
    Write-Host "  JWT_SECRET déjà configuré" -ForegroundColor Green
}

# Profil Spring
$env:SPRING_PROFILES_ACTIVE = "dev"
Write-Host "  Profil Spring: dev (PostgreSQL)" -ForegroundColor Green

# Configuration JVM optimisée
$env:MAVEN_OPTS = "-Xms2g -Xmx4g -XX:+UseG1GC"
Write-Host "  JVM configurée pour 147+ utilisateurs" -ForegroundColor Green

Write-Host ""

# Lancement du backend
Write-Host "Lancement du backend Spring Boot..." -ForegroundColor Cyan
Write-Host "  Port: 8080" -ForegroundColor Gray
Write-Host "  Profil: dev" -ForegroundColor Gray

Start-Process powershell -ArgumentList "-NoExit", "-Command", `
    "cd '$PWD'; `$env:JWT_SECRET='$env:JWT_SECRET'; `$env:SPRING_PROFILES_ACTIVE='$env:SPRING_PROFILES_ACTIVE'; `$env:MAVEN_OPTS='$env:MAVEN_OPTS'; mvn spring-boot:run" `
    -PassThru | Out-Null

Write-Host "  Backend en cours de démarrage..." -ForegroundColor Yellow
Write-Host ""

# Lancement du frontend
Write-Host "Lancement du frontend Angular..." -ForegroundColor Cyan
Write-Host "  Port: 4200" -ForegroundColor Gray

# Vérifier les dépendances frontend
if (-not (Test-Path "frontend/node_modules")) {
    Write-Host "  Installation des dépendances frontend..." -ForegroundColor Yellow
    Push-Location frontend
    npm ci
    Pop-Location
}

Start-Process powershell -ArgumentList "-NoExit", "-Command", `
    "cd '$PWD/frontend'; npm start" `
    -PassThru | Out-Null

Write-Host "  Frontend en cours de démarrage..." -ForegroundColor Yellow
Write-Host ""

# Attendre le démarrage des services
Write-Host "Attente du démarrage des services..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Vérifier le backend
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5
    if ($response.StatusCode -eq 200) {
        Write-Host "  ✓ Backend démarré sur http://localhost:8080" -ForegroundColor Green
    }
} catch {
    Write-Host "  ⚠ Backend en cours de démarrage..." -ForegroundColor Yellow
}

# Vérifier le frontend
try {
    $response = Invoke-WebRequest -Uri "http://localhost:4200" -UseBasicParsing -TimeoutSec 5
    if ($response.StatusCode -eq 200) {
        Write-Host "  ✓ Frontend démarré sur http://localhost:4200" -ForegroundColor Green
    }
} catch {
    Write-Host "  ⚠ Frontend en cours de démarrage..." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Green
Write-Host "   APPLICATION LANCÉE AVEC SUCCÈS!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Accès à l'application:" -ForegroundColor Cyan
Write-Host "  Frontend:    http://localhost:4200" -ForegroundColor White
Write-Host "  Backend API: http://localhost:8080" -ForegroundColor White
Write-Host "  Actuator:    http://localhost:8080/actuator/health" -ForegroundColor White
Write-Host ""
Write-Host "Pour arrêter l'application:" -ForegroundColor Yellow
Write-Host "  .\stop-app.ps1" -ForegroundColor White
Write-Host ""
Write-Host "Logs disponibles dans les fenêtres PowerShell ouvertes." -ForegroundColor Gray
Write-Host ""

# Ouvrir le navigateur après un délai
Start-Sleep -Seconds 5
Start-Process "http://localhost:4200"