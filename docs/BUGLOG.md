# Backlog bug — passate successive

Audit completo del 2026-08-02. I bug **critici** e **high** sono stati corretti nel branch
`fix/critical-high-batch` (v. git log). Qui restano i **medium** e **low** non ancora
affrontati, ordinati per priorità consigliata.

## Launcher — Medium

- **M1 — Path `%APPDATA%` hardcoded** (`src/main.js:14`, `lib/importer/detect.js`): su
  Linux/macOS crea `~/AppData/Roaming/LoloClient`. Usare `app.getPath('userData')` e
  path `.minecraft` per piattaforma nell'importer.
- **M2 — Superficie IPC filesystem inutilizzata** (`src/preload.js`): `readFile`,
  `writeFile`, `listFiles`, `copyFile`, ecc. non sono chiamati da nessuna parte del
  renderer ma restano attivi; `fsAllowedRoots()` include `assets/mods` (auto-copiata in
  ogni istanza). Rimuovere i canali morti.
- **M3 — Token decriptati nel renderer** (`get-config` + `sandbox:false` + CSP
  `unsafe-inline`): un'iniezione nel renderer = furto account. Tenere i token nel main,
  passare al renderer solo dati non sensibili; aggiungere `setWindowOpenHandler` /
  `will-navigate`.
- **M4 — `-Djava.library.path` punta all'istanza** senza natives estratti; funziona solo
  perché LWJGL3 si autoestrae dal classpath. Estrarre le natives o rimuovere il flag.
- **M5 — RAM < 1024 MB**: `-Xms1024M` hardcoded contro `-Xmx` da config → JVM non parte
  se l'utente imposta meno di 1 GB. Clampare `ramMB` al salvataggio e al lancio.
- **M6 — Sort lessicografico delle versioni** (`deployModJar`, `package-installer.ps1`):
  `1.9.0` batte `1.10.0`; filtrare anche `-dev.jar`/`-all.jar`. Confronto semver.
- **M7 — AppCDS inefficace**: `-Xshare:dump` senza `SharedClassListFile` non include il
  classpath; `rmSync` dell'archivio mappato dal gioco fallisce (EBUSY) e l'archivio
  resta stale; `-XX:SharedArchiveFile=` passato anche quando l'archivio non esiste.
- **M8 — (parzialmente risolto)** `downloadFile` ora ha idle-timeout e streaming; resta
  da aggiungere un pulsante "annulla" lato UI.
- **M9 — Asset pack**: errori per-oggetto solo loggati → "Pack pronto" anche con file
  mancanti; `event.sender.send` può lanciare se la finestra è chiusa a metà download.
- **M10 — Account con chiave = nome** (`renderer.js`): `getActive()` e la rimozione
  filtrano per `name`; due account omonimi sono indistinguibili. Chiave `uuid`+`type`.
  (Il caso offline duplicato è già stato mitigato.)
- **M11 — Cache icone mod**: `saveModIconsCache` non usa la scrittura atomica;
  `cache.ts` aggiornato anche su batch parzialmente fallito (icone mancanti per 24h).
- **M12 — Validazione shape** di `profiles`/`accounts` in `save-config` (il top-level è
  validato, il contenuto no).
- **M13 — package.json**: `electron-store` dipendenza mai usata; `adm-zip` require lazy
  dentro try silenziosi; nessun campo `repository`; `publish` non configurato (nessun
  canale auto-update malgrado la UI lo suggerisca).
- **M14 — package-installer.ps1**: la presenza di `node_modules/electron-builder` è
  trattata come "tutte le dipendenze installate"; dopo una modifica a package.json può
  impacchettare un'app rotta.

## Launcher — Low

- **L1** `lib/importer/apply.js`: il secondo import sovrascrive il `.bak` del primo
  (backup distrutto).
- **L2** Migrazione `.jar.disabled`: `renameSync` può clobberare un `.jar` omonimo.
- **L4** UI morta/fuorviante: card "Launcher aggiornato v1.0.0" senza handler né update
  check; `#stages` mai popolato; testo "verifica gli aggiornamenti" non corrispondente.
- **L5** Dead code in main.js: `instanceMetaDir`, array `mods` mai usato,
  `fabricMainClass`/`customArgs` assegnati e scartati, primo blocco `findJars` solo log.
- **L6** `net.fabricmc.loader.launch.knot.KnotClient` è il path pre-0.12 (shim
  deprecato); i loader attuali usano `net.fabricmc.loader.impl.launch.knot.KnotClient`.
  Verificare prima che lo shim sparisca.
- **L7** `open-external` permette `http:` e non ha allowlist di host.
- **L8** `skinCache` Map unbounded (main e renderer), mai svuotata.
- **L9** Possibile doppio lancio se `minecraft-closed` e il restart handler corrono
  sullo stesso exit.
- **L10** Typo in `package-installer.ps1`: "l exeexe generato".

## Mod — Medium

- **#11 — Regex sveglie**: `[01]?[0-9]` accetta `9:30` che non matcherà mai il formato
  `HH:mm` di `AlarmManager` → sveglia che non suona. Normalizzare al salvataggio.
- **#12 — `AlarmManager` de-dup per indice**: eliminare/riordinare sveglie nello stesso
  minuto sopprime o rifà suonare quella sbagliata; inoltre le sveglie non suonano nei
  menu (`client.player == null` ritorna subito).
- **#14 — `WaypointManager.nearest()`** restituisce una vista live della cache mutabile
  (`subList`) → CME potenziale. Restituire `List.copyOf`.
- **#15 — HUD editor**: drag salva coordinate non clampate mentre il box disegnato è
  clampato → moduli fuori schermo / "salto" visivo. Clampare in `movePosition`.
- **#16 — `ScreenMixin`** matcha gli screen per substring del nome classe ("stat",
  "pack", "world"…) e cancella `renderBackground` anche per GUI di altre mod. Usare
  `instanceof` sulle classi vanilla.
- **#17 — `MinecraftClientMixin`** scarta il flag di `GameMenuScreen(pauseOnly)` e
  `TitleScreen(doBackgroundFade)` e ricrea lo screen a ogni `setScreen`.
- **#18 — `RcPauseScreen.disconnect()`** non imposta lo screen dopo il disconnect in
  singleplayer (possibile blocco su "Saving world…"). Copiare la sequenza vanilla.
- **#19 — `SceneLogManager`**: write sincrona sul client thread per ogni riga RP →
  microstutter. Buffer + executor.
- **#21 — `WindowIcons`**: leak di `NativeImage`/`memAlloc` su eccezione.
  try-with-resources + finally.
- **#22 — `addControlsButton`** muta la lista `children()` di un EntryListWidget via
  cast unchecked (fragile tra versioni MC).
- **#23 — `FaceEditScreen.rebuild()`** chiamato dentro un callback widget mentre
  `mouseClicked` itera i children → focus stale. Differire con `client.execute`.
  Inoltre `faces.indexOf(existing)` può sovrascrivere il volto sbagliato e un salvataggio
  con `idx < 0` viene scartato in silenzio.
- **#24 — `QuickMessagesManager`** polla i tasti raw senza il sistema keybinding di MC:
  niente conflict detection (bindare `W` invia camminando), fire con overlay di altre
  mod, `lastDown` mai ripulita. Migrare a `KeyBindingHelper`.
- **#25 — `KeybindsScreen`** scrive le options a ogni tasto e non segnala conflitti con
  bind vanilla/moddati.

## Mod — Low

- **#26** `ModManager` è dead code (e apre un JarFile per mod a ogni `list()`).
- **#27/#28/#29** `Icons`: espressione morta nel foro del CPS, parametro `alarm`
  ignorato in `clock()`, possibile NaN in `sdSeg` con segmento degenere.
- **#30** `BetterTabTabEntryMixin`: `@Shadow` su classe terza senza null-guard; il
  mixin config `mixins-bettertab.json` logga errore a ogni avvio senza BetterTab
  (serve un `MixinConfigPlugin`).
- **#31** `PlayerListHudMixin`: `isEncrypted()` usato come proxy per l'offset del badge;
  tooltip disegnato a metà lista (coperto dalle righe successive).
- **#32/#33** `RcTitleScreen`: versione "1.21.8" hardcoded; nav sempre su "Home".
- **#34** `FaceEditScreen`: pulsante chiudi disegnato a `width-48` ma hit-test a
  `width-40`.
- **#35–#38** `ModMenuScreen`/liste: rebuild dei bottoni dentro `render()`, doppio
  dispatch del click sulla search, `setScreen(this)` che resetta scroll/ricerca,
  `mouseScrolled` che ritorna sempre true.
- **#39** `wasPressed()` drenato con `if` invece di `while`.
- **#40** `CinemaManager.toggle()` senza null-guard e clobbera lo stato F1 dell'utente.
- **#41** `positions()` espone la mappa mutabile live; chiavi sconosciute persistono.
- **#42** `GlassUi`: nessun re-init dopo resource reload; ~40 texture mai liberate.
- **#43** `cleanCodes` mangia "&x" anche in testo legittimo (es. "AT&T") nel matcher.
- **#44** FPS counter congelato quando l'HUD è nascosto (incluso cinema mode).

## Mod — Metadati di build

- `fabric.mod.json` dichiara `"fabricloader": ">=0.14.0"` ma si compila con 0.19.3:
  alzare il floor (≥0.16). Manca `"java": ">=21"` nei depends. `"fabric-api": "*"`
  senza floor malgrado l'uso di message-api-v1/HudRenderCallback.
- `build.gradle`: versione Loom `1.17.17` sospetta (verificare che esista; le release
  note per MC 1.21.8 sono nella serie 1.10/1.11). Nessun gradle wrapper nel repo →
  build non riproducibile (aggiungere `gradlew`).
- `libs/bettertab.jar` vendorizzato con licenza ignota e integrazione non dichiarata
  in `fabric.mod.json`.
- TwelveMonkeys WebP via jar-in-jar: la discovery `ServiceLoader` sotto Knot è
  inaffidabile; chiamare `ImageIO.scanForPlugins()` all'init.
