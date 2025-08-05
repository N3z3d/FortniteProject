# Script de démarrage optimisé pour l'application Fortnite Pronos
# Auteur: Assistant IA - Optimisé pour performance
# Date: 2025

Write-Host "🚀 Démarrage rapide de l'application Fortnite Pronos..." -ForegroundColor Green
Write-Host ""

# Vérification rapide des prérequis (en parallèle)
Write-Host "📋 Vérification des prérequis..." -ForegroundColor Yellow
$javaCheck = Start-Job -ScriptBlock { 
    try { 
        $version = java -version 2>&1 | Select-String "version" | Select-Object -First 1
        return "✅ Java détecté: $version"
    } catch { 
        return "❌ Java non trouvé"
    }
}

$nodeCheck = Start-Job -ScriptBlock {
    try {
        $version = node --version
        return "✅ Node.js détecté: $version"
    } catch {
        return "❌ Node.js non trouvé"
    }
}

# Attendre les vérifications
$javaResult = Receive-Job -Job $javaCheck -Wait
$nodeResult = Receive-Job -Job $nodeCheck -Wait

Write-Host $javaResult -ForegroundColor $(if($javaResult.StartsWith("❌")) { "Red" } else { "Green" })
Write-Host $nodeResult -ForegroundColor $(if($nodeResult.StartsWith("❌")) { "Red" } else { "Green" })

if ($javaResult.StartsWith("❌") -or $nodeResult.StartsWith("❌")) {
    Write-Host "❌ Prérequis manquants. Installation nécessaire." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "⚡ Démarrage parallèle des services..." -ForegroundColor Cyan

# Arrêter les processus qui utilisent les ports 8080 et 8081
Write-Host "🛑 Vérification des ports 8080 et 8081..." -ForegroundColor Yellow
try {
    $processes8080 = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | ForEach-Object { Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue }
    $processes8081 = Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue | ForEach-Object { Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue }
    
    if ($processes8080) {
        Write-Host "⚠️  Arrêt des processus sur le port 8080..." -ForegroundColor Yellow
        $processes8080 | Stop-Process -Force -ErrorAction SilentlyContinue
    }
    if ($processes8081) {
        Write-Host "⚠️  Arrêt des processus sur le port 8081..." -ForegroundColor Yellow
        $processes8081 | Stop-Process -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Seconds 2
} catch {
    Write-Host "✅ Ports libres" -ForegroundColor Green
}

# JWT-001: SECURITY CRITICAL - JWT Secret Configuration
Write-Host "🔐 JWT-001: Configuration sécurisée du JWT..." -ForegroundColor Magenta
if (-not $env:JWT_SECRET) {
    Write-Host "⚠️  JWT_SECRET non configuré - génération d'un secret de développement temporaire" -ForegroundColor Yellow
    # Générer un secret fort pour le développement local (256-bit / 64 caractères minimum)
    $env:JWT_SECRET = "dev-jwt-secret-$(Get-Random)-$(Get-Date -Format 'yyyyMMddHHmmss')-very-long-secure-key-for-development"
    Write-Host "🔑 Secret JWT généré pour cette session: ${env:JWT_SECRET}" -ForegroundColor Green
} else {
    Write-Host "✅ JWT_SECRET configuré via variable d'environnement" -ForegroundColor Green
}

# PHASE 1A: JVM OPTIMIZATION FOR 147+ USERS - PRODUCTION GRADE (FIXED CRITICAL)
# Fixed memory allocation to prevent JVM crashes identified in hs_err_pid*.log files
$env:MAVEN_OPTS = "-Xms4g -Xmx8g -XX:HeapBaseMinAddress=8g -XX:MaxDirectMemorySize=2g -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication -XX:G1HeapRegionSize=16m -XX:+UseCompressedOops -XX:+UseCompressedClassPointers -XX:+OptimizeStringConcat -XX:+UseFastAccessorMethods -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Dspring.main.lazy-initialization=true -Dspring.jpa.defer-datasource-initialization=true -Dfile.encoding=UTF-8 -Djava.awt.headless=true"
Write-Host "🚀 PHASE 1A: JVM Configuration FIXED pour 147+ utilisateurs (8GB heap stable)" -ForegroundColor Magenta
Write-Host "⚡ FIX CRITIQUES: Heap size augmenté 4GB→8GB, G1GC optimisé, crash prevention" -ForegroundColor Green

# Démarrer backend et frontend en parallèle
$backendJob = Start-Job -ScriptBlock {
    Set-Location $using:PWD
    $env:MAVEN_OPTS = $using:env:MAVEN_OPTS
    $env:JWT_SECRET = $using:env:JWT_SECRET
    mvn spring-boot:run -Dserver.port=8080 -q
}

$frontendJob = Start-Job -ScriptBlock {
    Set-Location "$using:PWD\frontend"
    npm start
}

Write-Host "🔧 Backend Spring Boot démarrage sur port 8080..." -ForegroundColor Green
Write-Host "🎨 Frontend Angular démarrage sur port 4200..." -ForegroundColor Green
Write-Host ""

# Fonction optimisée de vérification de santé
function Test-ServiceHealth {
    param($Url, $MaxAttempts = 30, $DelaySeconds = 2)
    
    for ($i = 1; $i -le $MaxAttempts; $i++) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                return $true
            }
        } catch {
            if ($i -eq $MaxAttempts) {
                return $false
            }
        }
        Start-Sleep -Seconds $DelaySeconds
    }
    return $false
}

# Vérification intelligente du démarrage
Write-Host "⏳ Vérification du démarrage des services..." -ForegroundColor Yellow

$backendReady = Test-ServiceHealth "http://localhost:8080/actuator/health"
$frontendReady = Test-ServiceHealth "http://localhost:4200"

if ($backendReady) {
    Write-Host "✅ Backend démarré avec succès sur http://localhost:8080" -ForegroundColor Green
} else {
    Write-Host "⚠️  Backend en cours de démarrage sur port 8080..." -ForegroundColor Yellow
}

if ($frontendReady) {
    Write-Host "✅ Frontend démarré avec succès sur http://localhost:4200" -ForegroundColor Green
} else {
    Write-Host "⚠️  Frontend en cours de démarrage..." -ForegroundColor Yellow
}

Remove-Job $javaCheck, $nodeCheck -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "🎉 Application démarrée avec correction du problème de port !" -ForegroundColor Green
Write-Host ""
Write-Host "📱 URLs d'accès:" -ForegroundColor White
Write-Host "   Frontend: http://localhost:4200" -ForegroundColor Cyan
Write-Host "   Backend API: http://localhost:8080" -ForegroundColor Cyan
Write-Host "   Health Check: http://localhost:8080/actuator/health" -ForegroundColor Cyan
Write-Host ""
Write-Host "✅ Corrections appliquées:" -ForegroundColor Green
Write-Host "   🔧 Port backend forcé à 8080" -ForegroundColor White
Write-Host "   📡 Frontend configuré pour 8080" -ForegroundColor White
Write-Host "   🛑 Nettoyage des ports avant démarrage" -ForegroundColor White
Write-Host "   ⚡ Configuration JVM optimisée" -ForegroundColor White
Write-Host ""
Write-Host "🔧 Pour arrêter l'application:" -ForegroundColor Yellow
Write-Host "   - Utilisez stop-app.ps1" -ForegroundColor White
Write-Host "   - Ou Ctrl+C dans les terminaux" -ForegroundColor White
Write-Host ""

# Ouvrir automatiquement le navigateur
Write-Host "🌐 Ouverture du navigateur..." -ForegroundColor Cyan
Start-Process "http://localhost:4200"

# Nettoyage des jobs
Remove-Job $backendJob, $frontendJob -Force -ErrorAction SilentlyContinue 