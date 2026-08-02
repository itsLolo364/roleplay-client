@echo off
echo ========================================
echo        LoloClient - Minecraft Launcher
echo ========================================
echo.

:: Check if node_modules exists
if not exist "node_modules" (
    echo Installing dependencies...
    call npm install
    echo.
)

:: Start the launcher
echo Starting LoloClient...
call npm start

pause
