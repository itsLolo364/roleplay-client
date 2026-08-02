@echo off
:: Lavora sempre nella cartella dello script (non nella CWD del chiamante,
:: che con "Esegui come amministratore" sarebbe System32).
cd /d "%~dp0"

echo ========================================
echo        LoloClient - Minecraft Launcher
echo ========================================
echo.

:: Check if node_modules exists
if not exist "node_modules" (
    echo Installing dependencies...
    call npm install
    if errorlevel 1 (
        echo.
        echo ERRORE: npm install fallito. Controlla l'output qui sopra.
        pause
        exit /b 1
    )
    echo.
)

:: Start the launcher
echo Starting LoloClient...
call npm start

pause
