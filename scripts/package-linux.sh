#!/usr/bin/env bash
# Package Roleplay Client per Linux (Mint/Ubuntu/Debian e derivate).
#
#   1) Build mod Fabric (Gradle), se disponibile
#   2) Copia il JAR in assets/mods/roleplayclient.jar
#   3) electron-builder --linux  ->  dist/RoleplayClient-*-linux-*.deb  +  .AppImage
#
# Uso:
#   ./scripts/package-linux.sh              # build completa (mod + launcher)
#   ./scripts/package-linux.sh --skip-mod   # usa il jar già presente in assets/mods
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SKIP_MOD=0
for arg in "$@"; do
    case "$arg" in
        --skip-mod) SKIP_MOD=1 ;;
        *) echo "Argomento sconosciuto: $arg"; exit 2 ;;
    esac
done

step() { printf '\n\033[36m==> %s\033[0m\n' "$1"; }
fail() { printf '\n\033[31mERRORE: %s\033[0m\n' "$1"; exit 1; }

echo "Roleplay Client - package Linux (.deb + AppImage)"
echo "Root: $ROOT"

# ---- Tools ----
command -v npm >/dev/null 2>&1 || fail "npm non trovato. Installa Node.js 18+ (es. 'sudo apt install nodejs npm')."

# ---- Mod ----
if [ "$SKIP_MOD" -eq 0 ]; then
    if [ -x "loloclient-mod/gradlew" ]; then
        GRADLE_CMD="./gradlew"
    elif command -v gradle >/dev/null 2>&1; then
        GRADLE_CMD="gradle"
    else
        GRADLE_CMD=""
    fi

    if [ -n "$GRADLE_CMD" ]; then
        step "Build mod Fabric"
        (cd loloclient-mod && $GRADLE_CMD build --quiet) || fail "Build mod fallita"

        step "Deploy JAR in assets/mods"
        JAR="$(find loloclient-mod/build/libs -maxdepth 1 -name 'roleplayclient*.jar' \
              ! -name '*sources*' 2>/dev/null | sort -V | tail -n 1 || true)"
        [ -n "$JAR" ] || fail "Nessun roleplayclient*.jar in loloclient-mod/build/libs"
        mkdir -p assets/mods
        cp -f "$JAR" assets/mods/roleplayclient.jar
        echo "    $(basename "$JAR") -> assets/mods/roleplayclient.jar"
    else
        step "Gradle non trovato: uso assets/mods/roleplayclient.jar esistente"
        [ -f assets/mods/roleplayclient.jar ] || fail "Manca assets/mods/roleplayclient.jar e Gradle non è installato (serve Java 21 + Gradle)."
    fi
else
    step "Skip mod (--skip-mod). Uso assets/mods/roleplayclient.jar esistente."
    [ -f assets/mods/roleplayclient.jar ] || fail "Manca assets/mods/roleplayclient.jar. Rilancia senza --skip-mod."
fi

[ -f assets/icon.png ] || fail "Manca assets/icon.png (richiesto da electron-builder per Linux)."

# ---- npm deps ----
if [ ! -d node_modules ] || [ ! -d node_modules/electron-builder ]; then
    step "npm install"
    npm install || fail "npm install fallito"
fi

# ---- Electron build ----
step "electron-builder - pacchetti Linux (.deb + AppImage)"
export CSC_IDENTITY_AUTO_DISCOVERY="false"
npm run build:linux || fail "electron-builder fallito"

echo ""
printf '\033[32mOK - pacchetti pronti in dist/:\033[0m\n'
found=0
for f in dist/*.deb dist/*.AppImage; do
    [ -e "$f" ] || continue
    found=1
    printf '  %s (%s)\n' "$f" "$(du -h "$f" | cut -f1)"
done
[ "$found" -eq 1 ] || { echo "Nessun pacchetto trovato in dist/:"; ls -la dist/ || true; exit 1; }

echo ""
echo "Installazione del .deb:   sudo apt install ./dist/RoleplayClient-*-linux-amd64.deb"
echo "Oppure AppImage:          chmod +x dist/RoleplayClient-*.AppImage && ./dist/RoleplayClient-*.AppImage"
