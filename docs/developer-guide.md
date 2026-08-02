# Developer Guide - Roleplay Client

> Panoramica completa e architettura: **[PROJECT_PRIMER.md](PROJECT_PRIMER.md)**.

## Panoramica

Roleplay Client è un progetto composto da due parti:

1. **Launcher Electron** (`/`) - Gestisce account, mod, versioni e avvio di Minecraft
2. **Mod Fabric** (`loloclient-mod/`) - Mod client-side per Minecraft 1.21.8 con HUD, GUI e QoL features

La fonte stilistica è `public/index.html` (tema dark). Le GUI in-game usano `GlassUi` con gli stessi token.

---

## Struttura del Progetto

```
LoloClient/
├── src/                          # Launcher Electron
│   ├── main.js                   # Main process (IPC, launch, auth)
│   ├── preload.js                # Context bridge per il renderer
│   └── public/
│       ├── index.html            # UI principale
│       ├── renderer.js           # Renderer process (logica UI)
│       └── avatar.js             # Rendering skin/avatar 3D
├── loloclient-mod/               # Mod Fabric
│   ├── src/main/java/net/roleplayclient/
│   │   ├── RoleplayClientMod.java        # Entry point mod
│   │   ├── RoleplayConfig.java           # Config persistente (JSON)
│   │   ├── Packages.java                 # Definizione pacchetti/moduli
│   │   ├── ModManager.java               # Scanning mod installate
│   │   ├── gui/
│   │   │   ├── GlassUi.java              # UI framework "Liquid Glass"
│   │   │   └── Icons.java                # Icone procedurali SDF
│   │   ├── hud/
│   │   │   └── HudRenderer.java          # Rendering HUD modulare
│   │   ├── screen/                       # Schermate GUI in-game
│   │   │   ├── ModMenuScreen.java
│   │   │   ├── HudEditorScreen.java
│   │   │   ├── KeybindsScreen.java
│   │   │   ├── FacesScreen.java
│   │   │   ├── AlarmsScreen.java
│   │   │   ├── WaypointsScreen.java
│   │   │   └── QuickMessagesScreen.java
│   │   └── mixin/                        # Mixin per codice vanilla
│   │       ├── GameRendererMixin.java
│   │       ├── ChatHudMixin.java
│   │       ├── PlayerListHudMixin.java
│   │       └── BetterTabTabEntryMixin.java
│   ├── build.gradle
│   └── gradle.properties
├── assets/
│   └── mods/                      # Mod bundlate (deploy automatico)
└── lib/
    └── fabric-installer.js        # Fabric installer
```

---

## Setup Ambiente di Sviluppo

### Prerequisiti

- **Node.js** 18+ e npm
- **Java 21** (JDK)
- **Git**
- **Gradle** (opzionale, il wrapper è incluso)

### Launcher

```bash
# Installa dipendenze
npm install

# Avvia in development
npm start
```

### Mod Fabric

```bash
cd loloclient-mod

# Build della mod
./gradlew build

# Oppure su Windows
gradlew.bat build
```

---

## Import da altri launcher

Modulo: `lib/importer/` (`detect.js`, `apply.js`).

- IPC: `import:detect`, `import:scan-folder`, `import:run` (preload: `electronAPI.importer`)
- UI: Impostazioni → “Importa da altro launcher”; al primo avvio modal se ci sono sorgenti (`settings.importPromptDone`)
- Categorie: opzioni (`options.txt` + correlati), `servers.dat`, mods Fabric, cartella `config/`, resourcepacks, shaderpacks
- Supportati: `.minecraft`, Prism, PolyMC, GDLauncher, Modrinth App, CurseForge (+ cartella manuale)

## Build & Deploy

### Installer Windows (consigliato)

Un solo comando crea l’app installabile (wizard NSIS) con mod aggiornata:

```bat
Package-Installer.bat
```

oppure:

```bash
npm run package:installer
```

Cosa fa:
1. Builda la mod Fabric (`gradle build`)
2. Copia il JAR in `assets/mods/roleplayclient.jar`
3. Genera l’installer con electron-builder

Output: `dist/RoleplayClient-Setup-1.0.0.exe`  
Puoi reinstallarlo per aggiornare o inviarlo così com’è.

Solo launcher (mod già in `assets/mods`):

```bash
npm run package:installer:quick
```

### Build Launcher (senza pipeline completa)

```bash
npm run build:win      # Solo electron-builder NSIS
npm run pack           # Cartella unpacked, senza installer
```

I file prodotti finiscono in `dist/`.

### Build Mod

```bash
cd loloclient-mod
gradle build
```

Il JAR finisce in `loloclient-mod/build/libs/roleplayclient-*.jar`.

### Deploy Automatico (dev)

In `npm start` il launcher copia il JAR buildato in `assets/mods/roleplayclient.jar` tramite `deployModJar()`. L’installer invece include già la copia fatta dallo script di packaging.

---

## Architettura della Mod

### Entry Point

`RoleplayClientMod.java` implementa `ClientModInitializer`:
- Registra keybindings
- Inietta il bottone "Roleplay Client Mods" in ControlsOptionsScreen
- Registra HUD render callback
- Registra eventi chat e tick

### Configurazione

`RoleplayConfig.java` gestisce la persistenza in `config/roleplay-client.json`:
- `modules` - stato on/off pacchetti
- `positions` - posizioni HUD normalizzate (0..1)
- `faces` - volti conosciuti
- `quickMessages` / `quickKeys` - messaggi rapidi
- `waypoints` - waypoints salvati
- `alarms` - sveglie

Tutta la config è caricata all'avvio e salvata automaticamente ad ogni modifica.

### Pacchetti (Packages)

`Packages.java` definisce i moduli interni del client come record `Pkg`:
- HUD packages (FPS, CPS, Coordinate, etc.)
- RP packages (Volti, Messaggi rapidi, Waypoint, etc.)

Per aggiungere un nuovo pacchetto, aggiungi una riga nel blocco `static {}`.

### GUI Framework

`GlassUi.java` implementa lo stile "Liquid Glass":
- Texture procedurali generate all'avvio (aurora, vetro, chip, dischi)
- Texture UI senza mipmap (`setFilter(..., false)`) — altrimenti cubi bianchi 9-slice
- Metodi: `panel()`, `card()`, `chip()`, `disc()`, `toggle()`, `statCard()`, `navRow()`, `border()`
- Layout shell responsivo: `RcShell` (header / sidebar / content / right) per Title, Pause, Options, liste

**Non modificare le palette colori senza aggiornare `CHIP_COLORS` e `DISC_COLORS`.**

### HUD

`HudRenderer.java` renderizza tutti gli HUD attivi. Ogni modulo HUD ha una posizione configurabile tramite `HudEditorScreen`.

### Mixin

I mixin estendono il codice vanilla:
- `GameRendererMixin` - effetto cinema
- `ChatHudMixin` - menzioni e volti in chat
- `PlayerListHudMixin` - badge "i" nella tab
- `BetterTabTabEntryMixin` - opzionale, solo se BetterTab è installato

---

## Convenzioni di Codice

### Java

- Package: `net.roleplayclient`
- Classi pubbliche in camelCase
- Metodi e variabili in camelCase
- Costanti in UPPER_SNAKE_CASE
- Nessun commento Javadoc obbligatorio (ma incoraggiato per API pubbliche)
- Gestione errori: logga sempre con `System.err.println` invece di `catch (Exception ignored)`

### JavaScript

- `main.js`: main process Electron
- `public/renderer.js`: renderer process
- `src/preload.js`: context bridge
- Funzioni in camelCase
- Costanti in UPPER_SNAKE_CASE
- Gestione errori: sempre `console.error` con messaggio descrittivo

### Stile UI

- Palette colori definita in `GlassUi.java`
- Tutte le superfici arrotondate usano `chip()` o `panel()`
- I toggle usano `toggle()`
- Testo: usa `ctx.drawText()` con colori da `GlassUi` (TEXT, MUTED, DIM)

---

## Estensione: Aggiungere un Nuovo Modulo HUD

1. Aggiungi il record in `Packages.java`:
   ```java
   add(new Pkg("mio-modulo", "Nome", "Descrizione", true, false, false));
   ```

2. Crea la classe in `hud/` o aggiungi la logica in `HudRenderer.java`

3. Aggiungi la posizione di default in `RoleplayConfig.java`:
   ```java
   positions.put("mio-modulo", new HudPos(0.005f, 0.19f));
   ```

4. Il modulo apparirà automaticamente nel Mod Menu

---

## Estensione: Aggiungere una Nuova Schermata

1. Crea la classe che estende `Screen` in `screen/`
2. Aggiungi il case in `ModMenuScreen.screenFor()` se è raggiungibile dal Mod Menu
3. Usa `GlassUi` per il layout

---

## Testing

### Mod

Non c'è un framework di test automatizzato. Per testare:
1. Builda la mod: `./gradlew build`
2. Copia il JAR in `assets/mods/` (o lascia che lo faccia il launcher)
3. Avvia Minecraft tramite il launcher

### Launcher

```bash
npm start
```

Verifica che:
- La UI carichi correttamente
- L'autenticazione funzioni
- Il launch di Minecraft completi senza errori

---

## Troubleshooting

### Mod non si aggiorna

Controlla che `assets/mods/roleplayclient.jar` sia stato aggiornato. Se no, verifica che `loloclient-mod/build/libs/` contenga il JAR buildato.

### Errori di compilazione Java

Assicurati di avere Java 21 e che `JAVA_HOME` sia impostato correttamente.

### Errori di compilazione Electron

Esegui `npm install` dopo aver modificato `package.json`.

### White squares in GUI

Causa tipica: **mipmap** abilitate con `tex.setFilter(true, true)` su texture 9-slice traslucide.
Fix: `setFilter(false, false)` su glass/chip/disc; aurora può usare bilinear **senza** mipmap (`true, false`).
Inoltre `disc`/`chip` devono generare texture on-demand (niente `fill` bianco di fallback).
Vedi anche attenuazione edge in `makeGlassTile` / `makeChip` e cutoff alpha in `makeDisc`.

---

## Workflow di Sviluppo Consigliato

1. Modifica il codice della mod o del launcher
2. Builda la mod con `./gradlew build`
3. Avvia il launcher con `npm start` - il deploy è automatico
4. Testa in-game
5. Commit con messaggio descrittivo
