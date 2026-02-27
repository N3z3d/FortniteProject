@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-bmad-prompts.ps1" %*
exit /b %errorlevel%
