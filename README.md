# Roleplay Client

Launcher Electron + mod Fabric per Minecraft **1.21.8**, con UI Liquid Glass condivisa.

## Caratteristiche

- Account Microsoft e offline
- Deploy automatico della mod `roleplay-client`
- HUD e moduli RP (volti, messaggi rapidi, waypoint, cinema, zoom, …)
- Menu in-game (Title / Pausa / Opzioni / Moduli) nello stile del launcher

## Requisiti

- Node.js 18+
- Java 21 (JDK)
- Gradle (per buildare la mod)

## Avvio sviluppo

```bash
npm install
npm start
```

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

```bash
npm run build:win
```

Output in `dist/`.
