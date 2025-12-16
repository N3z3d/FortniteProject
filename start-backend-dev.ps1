# Script de démarrage du backend en mode dev
Write-Host "Demarrage du backend FortniteProject en mode dev..." -ForegroundColor Green

# Nettoyer les anciens processus Java (Spring Boot) si nécessaire
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcesses) {
    Write-Host "Processus Java detectes. Nettoyage..." -ForegroundColor Yellow
    $javaProcesses | Where-Object { $_.MainWindowTitle -match "fortnite" } | Stop-Process -Force -ErrorAction SilentlyContinue
}

# Définir la variable d'environnement JWT
$env:JWT_SECRET = "ma_super_cle_jwt_securisee_123456789_vraiment_longue_pour_securite_maximale"

# Démarrer Maven Spring Boot avec profil H2 (base de données en mémoire)
Write-Host "Lancement de Maven Spring Boot avec H2..." -ForegroundColor Cyan
mvn spring-boot:run "-Dspring-boot.run.profiles=h2" "-Dmaven.test.skip=true"
