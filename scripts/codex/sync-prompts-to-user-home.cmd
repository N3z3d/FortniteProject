@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0sync-prompts-to-user-home.ps1" %*
exit /b %errorlevel%
