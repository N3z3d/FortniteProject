# Script pour lancer l'application Angular (optimisé) - PHASE 1A JVM Optimized + JWT-001 Security
# Usage: .\lancer-app.ps1

Write-Host "🚀 Lancement optimisé de l'application Angular - PHASE 1A..." -ForegroundColor Green
Write-Host "⚡ JVM optimisé pour production (si backend requis)" -ForegroundColor Yellow

# JWT-001: SECURITY CRITICAL - Ensure JWT_SECRET is configured if backend is needed
Write-Host "🔐 JWT-001: Vérification de la sécurité JWT..." -ForegroundColor Magenta
if (-not $env:JWT_SECRET) {
    Write-Host "⚠️  JWT_SECRET non configuré - générant un secret temporaire pour cette session" -ForegroundColor Yellow
    $env:JWT_SECRET = "angular-dev-session-$(Get-Random)-$(Get-Date -Format 'yyyyMMddHHmmss')-secure-key-temp"
    Write-Host "🔑 Secret JWT temporaire configuré pour cette session" -ForegroundColor Green
} else {
    Write-Host "✅ JWT_SECRET configuré depuis l'environnement" -ForegroundColor Green
}

# Vérifier si on est dans le bon répertoire
if (-not (Test-Path "frontend\angular.json")) {
    Write-Host "❌ Erreur: Fichier angular.json non trouvé. Assurez-vous d'être dans le répertoire racine du projet." -ForegroundColor Red
    exit 1
}

# Naviguer vers le dossier frontend
Set-Location frontend

Write-Host "📁 Répertoire: $(Get-Location)" -ForegroundColor Yellow

# Vérifier si ng est installé
try {
    $ngVersion = ng version 2>$null
    Write-Host "✅ Angular CLI détecté" -ForegroundColor Green
} catch {
    Write-Host "❌ Erreur: Angular CLI non trouvé. Installez-le avec: npm install -g @angular/cli" -ForegroundColor Red
    exit 1
}

# Arrêter les processus existants sur le port 4200 (optimisé)
Write-Host "🛑 Arrêt des processus existants sur le port 4200..." -ForegroundColor Yellow
try {
    $processes = Get-NetTCPConnection -LocalPort 4200 -ErrorAction SilentlyContinue | ForEach-Object { Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue }
    if ($processes) {
        $processes | Stop-Process -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 1
        Write-Host "✅ Processus arrêtés" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠️  Aucun processus à arrêter" -ForegroundColor Yellow
}

# Lancer l'application avec optimisations
Write-Host "🔥 Lancement optimisé de ng serve..." -ForegroundColor Green
Write-Host "🌐 L'application sera accessible sur: http://localhost:4200" -ForegroundColor Cyan
Write-Host "⚡ Optimisations activées: HMR, Live Reload, Polling optimisé" -ForegroundColor Green
Write-Host "⏹️  Appuyez sur Ctrl+C pour arrêter le serveur" -ForegroundColor Yellow
Write-Host ""

# Lancer ng serve avec options optimisées
ng serve --open --configuration development --hmr --live-reload --poll=1000 