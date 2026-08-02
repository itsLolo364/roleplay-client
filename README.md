# Roleplay Client

Launcher Electron + mod Fabric per Minecraft **1.21.8**, con UI Liquid Glass condivisa.
Funziona su **Windows** e **Linux** (Mint/Ubuntu/Debian e derivate).

## Caratteristiche

- Account Microsoft e offline
- Deploy automatico della mod `roleplay-client`
- HUD e moduli RP (volti, messaggi rapidi, waypoint, cinema, zoom, …)
- Menu in-game (Title / Pausa / Opzioni / Moduli) nello stile del launcher

## Requisiti

- Node.js 18+
- Java 21 (JDK)
- Gradle (per buildare la mod)

## Installazione su Linux (Mint/Ubuntu/Debian)

Pacchetto `.deb` (consigliato — crea voce di menu e icona):

```bash
npm run package:linux
sudo apt install ./dist/RoleplayClient-*-linux-amd64.deb
```

Poi avvia da menu applicazioni ("Roleplay Client") o da terminale:

```bash
roleplay-client
```

In alternativa **AppImage** (nessuna installazione, un solo file portabile):

```bash
chmod +x dist/RoleplayClient-*.AppImage
./dist/RoleplayClient-*.AppImage
```

Avvio rapido da sorgenti senza installare nulla:

```bash
./start.sh
```

I dati (config, istanze, mod) vanno in `~/.config/LoloClient`.

Per disinstallare il pacchetto: `sudo apt remove roleplay-client`.

## Avvio sviluppo

```bash
npm install
npm start
```

(Su Windows: `start.bat`; su Linux/macOS: `./start.sh`.)

Build mod:

```bash
cd loloclient-mod
gradle build
```

Il launcher copia il JAR in `assets/mods/` all’avvio.

## Documentazione

- [Project Primer](docs/PROJECT_PRIMER.md) — architettura e convenzioni
- [Developer Guide](docs/developer-guide.md) — setup e estensioni
- [AGENTS.md](AGENTS.md) — note per agent AI

## Build installer

Windows (NSIS):

```bash
npm run build:win
```

Linux (`.deb` + AppImage, con build della mod se Gradle è disponibile):

```bash
npm run package:linux
```

Output in `dist/`.
