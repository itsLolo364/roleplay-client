@echo off
setlocal
cd /d "%~dp0"

echo.
echo  Roleplay Client — creazione installer Windows
echo  ==============================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\package-installer.ps1" %*
set ERR=%ERRORLEVEL%

echo.
if %ERR% neq 0 (
  echo  Fallito con codice %ERR%.
  pause
  exit /b %ERR%
)

echo  Fatto. Premi un tasto per chiudere.
pause
exit /b 0
