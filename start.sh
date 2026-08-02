#!/usr/bin/env bash
# Avvio in modalità sviluppo su Linux/macOS (equivalente di start.bat).
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

echo "========================================"
echo "       LoloClient - Minecraft Launcher"
echo "========================================"
echo ""

command -v npm >/dev/null 2>&1 || { echo "ERRORE: npm non trovato. Installa Node.js 18+ (es. 'sudo apt install nodejs npm')."; exit 1; }

if [ ! -d node_modules ]; then
    echo "Installing dependencies..."
    npm install || { echo ""; echo "ERRORE: npm install fallito. Controlla l'output qui sopra."; exit 1; }
    echo ""
fi

echo "Starting LoloClient..."
npm start
