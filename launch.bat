@echo off
echo ================================================
echo    FORTNITE PRONOS - Lancement de l'application
echo ================================================
echo.

REM Configuration des variables d'environnement
set JWT_SECRET=ma_super_cle_jwt_securisee_123456789_vraiment_longue_pour_securite_maximale
set SPRING_PROFILES_ACTIVE=dev
set MAVEN_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC

echo Configuration:
echo   JWT_SECRET configure
echo   Profil Spring: dev
echo   JVM optimisee pour 147+ utilisateurs
echo.

echo Lancement du backend Spring Boot (port 8080)...
start cmd /k "cd /d %cd% && mvn spring-boot:run"

echo Attente du demarrage du backend...
timeout /t 10 /nobreak >nul

echo Lancement du frontend Angular (port 4200)...
start cmd /k "cd /d %cd%\frontend && npm start"

echo.
echo ================================================
echo    APPLICATION LANCEE!
echo ================================================
echo.
echo Acces:
echo   Frontend:    http://localhost:4200
echo   Backend API: http://localhost:8080
echo   Actuator:    http://localhost:8080/actuator/health
echo.
echo Ouverture du navigateur dans 10 secondes...
timeout /t 10 /nobreak >nul

start http://localhost:4200

echo.
echo Pour arreter l'application, fermez les fenetres CMD.
pause