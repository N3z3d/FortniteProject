@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-codex-with-bmad.ps1" %*
exit /b %errorlevel%
