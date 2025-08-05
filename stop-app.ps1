# Script d'arrêt optimisé pour l'application Fortnite Pronos
# Auteur: Assistant IA - Optimisé pour performance
# Date: 2025

Write-Host "🛑 Arrêt rapide de l'application Fortnite Pronos..." -ForegroundColor Red
Write-Host ""

# Arrêter les processus par port (plus précis et rapide)
function Stop-ProcessByPort {
    param($Port, $ServiceName)
    
    Write-Host "🔧 Arrêt de $ServiceName (port $Port)..." -ForegroundColor Yellow
    
    try {
        $connections = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
        if ($connections) {
            $processIds = $connections | ForEach-Object { $_.OwningProcess } | Sort-Object -Unique
            
            foreach ($processId in $processIds) {
                $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
                if ($process) {
                    Write-Host "   - Arrêt du processus $($process.ProcessName) (PID: $processId)" -ForegroundColor White
                    try {
                        $process.CloseMainWindow()
                        Start-Sleep -Seconds 2
                        if (!$process.HasExited) {
                            $process.Kill()
                        }
                        Write-Host "   ✅ $ServiceName arrêté" -ForegroundColor Green
                    } catch {
                        Write-Host "   ❌ Erreur lors de l'arrêt de $ServiceName" -ForegroundColor Red
                    }
                }
            }
        } else {
            Write-Host "   ℹ️  Aucun processus sur le port $Port" -ForegroundColor Gray
        }
    } catch {
        Write-Host "   ⚠️  Erreur lors de la vérification du port $Port" -ForegroundColor Yellow
    }
}

# Arrêter les services en parallèle
Stop-ProcessByPort 8080 "Backend Spring Boot"

Write-Host ""
Stop-ProcessByPort 4200 "Frontend Angular"

# Attendre un peu pour que les processus se terminent (optimisé)
Write-Host ""
Write-Host "⏳ Vérification de la terminaison..." -ForegroundColor Yellow
Start-Sleep -Seconds 1

# Vérification rapide des ports
function Test-PortStatus {
    param($Port, $ServiceName)
    
    try {
        $connections = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
        if ($connections) {
            Write-Host "⚠️  Le port $Port ($ServiceName) est encore utilisé" -ForegroundColor Yellow
            return $false
        } else {
            Write-Host "✅ Le port $Port ($ServiceName) est libre" -ForegroundColor Green
            return $true
        }
    } catch {
        Write-Host "✅ Le port $Port ($ServiceName) est libre" -ForegroundColor Green
        return $true
    }
}

Write-Host ""
Write-Host "🔍 Vérification rapide des ports..." -ForegroundColor Cyan
$port8080Free = Test-PortStatus 8080 "Backend"
$port4200Free = Test-PortStatus 4200 "Frontend"

# Vérification finale optimisée
Write-Host ""
if ($port8080Free -and $port4200Free) {
    Write-Host "✅ Tous les services ont été arrêtés avec succès" -ForegroundColor Green
} else {
    Write-Host "⚠️  Certains services nécessitent un arrêt manuel" -ForegroundColor Yellow
    Write-Host "   Utilisez le Gestionnaire des tâches si nécessaire" -ForegroundColor White
}

Write-Host ""
Write-Host "🎉 Arrêt optimisé terminé !" -ForegroundColor Green
Write-Host ""
Write-Host "⚡ Améliorations appliquées:" -ForegroundColor Green
Write-Host "   🎯 Arrêt par port (plus précis)" -ForegroundColor White
Write-Host "   🚀 Terminaison propre des processus" -ForegroundColor White
Write-Host "   📊 Vérifications optimisées" -ForegroundColor White
Write-Host ""
Write-Host "💡 Pour redémarrer l'application:" -ForegroundColor Cyan
Write-Host "   .\start-app.ps1" -ForegroundColor White
Write-Host ""
Write-Host "✨ Services arrêtés efficacement !" -ForegroundColor Green 