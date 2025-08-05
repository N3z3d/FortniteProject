# JWT-001: Script de génération de secret JWT sécurisé pour la production
# Usage: .\generate-jwt-secret.ps1
# Génère un secret JWT 256-bit cryptographiquement sécurisé

Write-Host "🔐 JWT-001: Générateur de secret JWT sécurisé pour production" -ForegroundColor Green
Write-Host ""

# Vérifier PowerShell version pour la compatibilité crypto
if ($PSVersionTable.PSVersion.Major -lt 5) {
    Write-Host "❌ PowerShell 5.0+ requis pour la génération cryptographique sécurisée" -ForegroundColor Red
    exit 1
}

Write-Host "🔑 Génération d'un secret JWT 256-bit cryptographiquement sécurisé..." -ForegroundColor Yellow

try {
    # Utiliser System.Security.Cryptography pour générer un secret cryptographiquement sécurisé
    Add-Type -AssemblyName System.Security
    $rng = [System.Security.Cryptography.RNGCryptoServiceProvider]::new()
    $bytes = New-Object byte[] 64  # 64 bytes = 512 bits (double de la taille minimale pour sécurité renforcée)
    $rng.GetBytes($bytes)
    
    # Convertir en base64 pour faciliter la manipulation
    $base64Secret = [System.Convert]::ToBase64String($bytes)
    
    # Créer un secret hybride avec caractères alphanumériques pour robustesse
    $timestamp = Get-Date -Format "yyyyMMddHHmmss"
    $randomSuffix = -join ((0..15) | ForEach-Object { Get-Random -InputObject ([char[]]"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789") })
    
    $jwtSecret = "prod-fortnite-pronos-$timestamp-$base64Secret-$randomSuffix"
    
    Write-Host "✅ Secret JWT généré avec succès!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📋 INSTRUCTIONS DE DÉPLOIEMENT PRODUCTION:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. Copiez le secret suivant (longueur: $($jwtSecret.Length) caractères):" -ForegroundColor White
    Write-Host ""
    Write-Host $jwtSecret -ForegroundColor Yellow
    Write-Host ""
    Write-Host "2. Configurez la variable d'environnement sur votre serveur de production:" -ForegroundColor White
    Write-Host "   export JWT_SECRET='$jwtSecret'" -ForegroundColor Green
    Write-Host ""
    Write-Host "3. Ou pour Docker:" -ForegroundColor White
    Write-Host "   -e JWT_SECRET='$jwtSecret'" -ForegroundColor Green
    Write-Host ""
    Write-Host "4. Ou pour Windows Server:" -ForegroundColor White
    Write-Host "   set JWT_SECRET=$jwtSecret" -ForegroundColor Green
    Write-Host ""
    Write-Host "⚠️  SÉCURITÉ CRITIQUE:" -ForegroundColor Red
    Write-Host "   - Ne JAMAIS committer ce secret dans Git" -ForegroundColor White
    Write-Host "   - Stockez-le de manière sécurisée (vault, gestionnaire de secrets)" -ForegroundColor White
    Write-Host "   - Utilisez un secret différent pour chaque environnement" -ForegroundColor White
    Write-Host "   - Changez régulièrement le secret en production" -ForegroundColor White
    Write-Host ""
    Write-Host "✅ L'application Spring Boot refusera de démarrer en production sans ce secret" -ForegroundColor Green
    
    # Optionnel: Sauvegarder dans un fichier temporaire (NON COMMITÉ)
    $secretFile = "jwt-secret-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
    $jwtSecret | Out-File -FilePath $secretFile -Encoding UTF8
    Write-Host ""
    Write-Host "💾 Secret sauvegardé temporairement dans: $secretFile" -ForegroundColor Magenta
    Write-Host "⚠️  Supprimez ce fichier après avoir configuré la production!" -ForegroundColor Red

} catch {
    Write-Host "❌ Erreur lors de la génération du secret: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
} finally {
    # Nettoyer les objets cryptographiques
    if ($rng) { $rng.Dispose() }
}

Write-Host ""
Write-Host "🎯 Configuration terminée! Votre application est maintenant sécurisée." -ForegroundColor Green