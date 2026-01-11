# Script de demarrage du backend en mode dev
Write-Host "Demarrage du backend FortniteProject en mode dev..." -ForegroundColor Green

# Nettoyer les anciens processus Java (Spring Boot) si necessaire
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcesses) {
    Write-Host "Processus Java detectes. Nettoyage..." -ForegroundColor Yellow
    $javaProcesses | Where-Object { $_.MainWindowTitle -match "fortnite" } | Stop-Process -Force -ErrorAction SilentlyContinue
}

# Definir la variable d'environnement JWT
$env:JWT_SECRET = "ma_super_cle_jwt_securisee_123456789_vraiment_longue_pour_securite_maximale"

# Appliquer Spotless avant de demarrer
Write-Host "Application de Spotless..." -ForegroundColor Cyan
mvn spotless:apply

# Demarrer Maven Spring Boot avec profil dev (PostgreSQL persistante)
Write-Host "Lancement de Maven Spring Boot avec dev..." -ForegroundColor Cyan
mvn spring-boot:run "-Dspring-boot.run.profiles=dev" "-Dmaven.test.skip=true"
