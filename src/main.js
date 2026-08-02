const { app, BrowserWindow, ipcMain, dialog, shell } = require('electron');
const { safeStorage } = require('electron');
const path = require('path');
const fs = require('fs');
const os = require('os');

// Disable hardware acceleration for better compatibility
app.disableHardwareAcceleration();

let mainWindow;
const isDev = process.env.NODE_ENV === 'development';

// App data path
const APP_DATA_PATH = path.join(os.homedir(), 'AppData', 'Roaming', 'LoloClient');

function encryptSensitive(data) {
    try {
        const str = JSON.stringify(data);
        if (safeStorage && safeStorage.isEncryptionAvailable()) {
            return safeStorage.encryptString(str).toString('base64');
        }
        return Buffer.from(str).toString('base64');
    } catch (e) {
        return Buffer.from(JSON.stringify(data)).toString('base64');
    }
}

function decryptSensitive(encrypted) {
    try {
        const buf = Buffer.from(encrypted, 'base64');
        if (safeStorage && safeStorage.isEncryptionAvailable()) {
            return JSON.parse(safeStorage.decryptString(buf).toString());
        }
        return JSON.parse(buf.toString());
    } catch (e) {
        return null;
    }
}

// Create app data directory if it doesn't exist
function ensureAppDataPath() {
    if (!fs.existsSync(APP_DATA_PATH)) {
        fs.mkdirSync(APP_DATA_PATH, { recursive: true });
    }
    const instancesPath = path.join(APP_DATA_PATH, 'instances');
    if (!fs.existsSync(instancesPath)) {
        fs.mkdirSync(instancesPath, { recursive: true });
    }
}

// Config management
function getConfig() {
    const configPath = path.join(APP_DATA_PATH, 'config.json');
    if (fs.existsSync(configPath)) {
        try {
            const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
            if (config.accounts && Array.isArray(config.accounts)) {
                config.accounts = config.accounts.map(acc => {
                    if (acc.encrypted) {
                        const decrypted = decryptSensitive(acc.encrypted);
                        if (decrypted) return { ...decrypted, encrypted: undefined };
                    }
                    return acc;
                });
            }
            return config;
        } catch (e) {
            console.error('Error parsing config:', e);
        }
    }
    return {
        settings: {
            ramMB: 4096,
            jvmArgs: '-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M',
            closeOnLaunch: false,
            mcVersionOverride: '',
            fabricLoaderOverride: '',
            showSnapshots: false,
            javaPath: '',
            minecraftDir: '',
            telemetry: false,
            optimizeFps: true,
            openLogsOnLaunch: false,
            importPromptDone: false
        },
        accounts: [],
        profiles: [],
        lastUsername: '',
        lastAccountType: null,
        lastProfileId: null
    };
}

function atomicWriteJson(filePath, data) {
    const dir = path.dirname(filePath);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    const tmp = filePath + '.tmp';
    fs.writeFileSync(tmp, JSON.stringify(data, null, 2));
    fs.renameSync(tmp, filePath);
}

function saveConfig(config) {
    const configPath = path.join(APP_DATA_PATH, 'config.json');
    const toSave = JSON.parse(JSON.stringify(config));
    if (toSave.accounts && Array.isArray(toSave.accounts)) {
        toSave.accounts = toSave.accounts.map(acc => {
            const sensitiveFields = ['mcTokenSerialized', 'mclcToken', 'profile', 'accessToken', 'mcToken', 'refresh_token', 'access_token'];
            const sensitiveData = {};
            let hasSensitive = false;
            for (const key of sensitiveFields) {
                if (acc[key]) {
                    sensitiveData[key] = acc[key];
                    hasSensitive = true;
                }
            }
            if (hasSensitive) {
                const cleaned = { ...acc };
                for (const key of sensitiveFields) delete cleaned[key];
                cleaned.encrypted = encryptSensitive({ ...acc, ...sensitiveData });
                return cleaned;
            }
            return acc;
        });
    }
    atomicWriteJson(configPath, toSave);
}

// Instances management
function getInstances() {
    const instancesPath = path.join(APP_DATA_PATH, 'instances.json');
    if (fs.existsSync(instancesPath)) {
        try {
            return JSON.parse(fs.readFileSync(instancesPath, 'utf8'));
        } catch (e) {
            console.error('Error parsing instances:', e);
        }
    }
    return { instances: [], activeId: null };
}

function saveInstances(instances) {
    const instancesPath = path.join(APP_DATA_PATH, 'instances.json');
    atomicWriteJson(instancesPath, instances);
}

// ===== Mods helpers =====
function getModsDir(instanceId) {
    const id = safeInstanceId(instanceId);
    return path.join(APP_DATA_PATH, 'instances', id, 'mods');
}

function instanceMetaDir(instanceId) {
    const id = String(instanceId || 'default').replace(/[^a-zA-Z0-9_-]/g, '');
    return path.join(APP_DATA_PATH, 'instances', id, '.roleplay-client');
}

function safeInstanceId(instanceId) {
    const id = String(instanceId || 'default');
    if (id.includes('..') || id.includes('/') || id.includes('\\') || id.includes('\0')) {
        throw new Error('instanceId non valido');
    }
    return id.replace(/[^a-zA-Z0-9_-]/g, '') || 'default';
}

// Deploy the latest built mod JAR into assets/mods so ensureBundledMods copies it.
function deployModJar() {
    const bundledDir = path.join(__dirname, '..', 'assets', 'mods');
    if (!fs.existsSync(bundledDir)) fs.mkdirSync(bundledDir, { recursive: true });
    const buildDir = path.join(__dirname, '..', 'loloclient-mod', 'build', 'libs');
    if (!fs.existsSync(buildDir)) return;
    const candidates = fs.readdirSync(buildDir)
        .filter(f => f === 'roleplayclient.jar' || (f.startsWith('roleplayclient-') && f.endsWith('.jar') && !f.endsWith('-sources.jar')))
        .sort((a, b) => b.localeCompare(a));
    if (!candidates.length) return;
    const src = path.join(buildDir, candidates[0]);
    const dest = path.join(bundledDir, 'roleplayclient.jar');
    try {
        fs.copyFileSync(src, dest);
        console.log('[Deploy] Mod aggiornata:', dest);
    } catch (e) {
        console.error('[Deploy] Errore copia mod:', e.message);
    }
}

// Copy bundled mods (assets/mods) into the instance on first run.
// roleplayclient.jar is always refreshed (it's the client's own mod).
// fabric-api.jar is only copied if no fabric-api jar is already installed
// (avoids duplicate mod ids when the user installs it via Modrinth).
function ensureBundledMods(instanceId) {
    deployModJar();
    const modsDir = getModsDir(instanceId);
    if (!fs.existsSync(modsDir)) fs.mkdirSync(modsDir, { recursive: true });
    const bundledDir = path.join(__dirname, '..', 'assets', 'mods');
    if (!fs.existsSync(bundledDir)) return;

    // Rimuove i jar del vecchio client (id "loloclient" / nome "loloclientmod")
    // e le eventuali vecchie copie di "roleplayclient" già copiate: altrimenti
    // caricando due mod con lo stesso scopo compaiono pulsanti/HUD duplicati.
    for (const f of fs.readdirSync(modsDir)) {
        const lower = f.toLowerCase();
        if (!lower.endsWith('.jar')) continue;
        const p = path.join(modsDir, f);
        const id = readModId(p);
        const base = lower.replace(/\.jar$/i, '');
        const stripped = stripVersionSuffix(base);
        const isLegacy = id === 'loloclient' || stripped === 'loloclientmod';
        const isOwnOld = (id === 'roleplay-client' || stripped === 'roleplayclient') && !(f.toLowerCase() === 'roleplayclient.jar');
        if (isLegacy || isOwnOld) {
            try { fs.rmSync(p, { force: true }); } catch (e) {}
        }
    }

    const existing = fs.readdirSync(modsDir);
    for (const f of fs.readdirSync(bundledDir)) {
        if (!f.toLowerCase().endsWith('.jar')) continue;
        const dest = path.join(modsDir, f);
        const isClientMod = f.toLowerCase() === 'roleplayclient.jar';
        const isFabricApi = f.toLowerCase().startsWith('fabric-api');
        if (isClientMod) {
            try { fs.copyFileSync(path.join(bundledDir, f), dest); } catch (e) {}
        } else if (isFabricApi) {
            const hasFabricApi = existing.some(x => x.toLowerCase().startsWith('fabric-api'));
            if (!hasFabricApi) {
                try { fs.copyFileSync(path.join(bundledDir, f), dest); } catch (e) {}
            }
        } else if (!fs.existsSync(dest)) {
            try { fs.copyFileSync(path.join(bundledDir, f), dest); } catch (e) {}
        }
    }
}

function friendlyModName(fileName) {
    const base = fileName.replace(/\.jar$/i, '');
    const stripped = base.replace(/(?:^|[_-])(?:v?\d[\w.+-]*)$/i, '');
    const clean = (stripped || base).replace(/[-_]+/g, ' ').trim();
    return clean.replace(/\b\w/g, c => c.toUpperCase());
}

async function fetchJson(url, timeoutMs = 15000) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
        const res = await fetch(url, {
            headers: { 'User-Agent': 'RoleplayClient/1.0.0 (Minecraft launcher)', 'Accept': 'application/json' },
            signal: controller.signal
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return await res.json();
    } finally {
        clearTimeout(timer);
    }
}

async function fetchJsonRetry(url, attempts = 2) {
    let lastErr;
    for (let i = 0; i < attempts; i++) {
        try {
            return await fetchJson(url);
        } catch (e) {
            lastErr = e;
        }
    }
    throw lastErr;
}

// Check if a project has a compatible version (used as fallback when the
// search version-facet is unavailable, e.g. for brand new MC versions)
async function projectHasVersion(projectId, mcVersion, loader) {
    try {
        const versions = await fetchJson(`https://api.modrinth.com/v2/project/${projectId}/version?game_versions=${encodeURIComponent(JSON.stringify([mcVersion]))}&loaders=${encodeURIComponent(JSON.stringify([loader]))}`, 10000);
        return Array.isArray(versions) && versions.length > 0;
    } catch (e) {
        return false;
    }
}

async function downloadFile(url, dest) {
    const res = await fetch(url, { headers: { 'User-Agent': 'RoleplayClient/1.0.0 (Minecraft launcher)' } });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const buf = Buffer.from(await res.arrayBuffer());
    const dir = path.dirname(dest);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(dest, buf);
    return dest;
}

// Assicura che il pack Minecraft di default (suoni, texture, font) sia
// disponibile all'avvio. Prima riusa gli asset del .minecraft ufficiale se
// presenti; altrimenti li scarica nella cartella dell'istanza.
async function ensureDefaultPack(gameDir, mcVersion, onProgress = () => {}) {
    const versionJsonPath = path.join(gameDir, 'versions', mcVersion, `${mcVersion}.json`);
    if (!fs.existsSync(versionJsonPath)) {
        console.log('[Default pack] Version JSON non trovato, salto verifica asset');
        return null;
    }
    let version;
    try { version = JSON.parse(fs.readFileSync(versionJsonPath, 'utf8')); } catch (e) { return null; }
    const assetIndex = version.assetIndex;
    if (!assetIndex || !assetIndex.id || !assetIndex.url) {
        console.log('[Default pack] Nessun assetIndex nel version JSON');
        return null;
    }
    const id = assetIndex.id;
    const officialAssets = path.join(app.getPath('appData'), '.minecraft', 'assets');
    if (fs.existsSync(path.join(officialAssets, 'indexes', `${id}.json`)) &&
        fs.existsSync(path.join(officialAssets, 'objects')) &&
        fs.readdirSync(path.join(officialAssets, 'objects')).length > 0) {
        console.log(`[Default pack] Asset riutilizzati dal .minecraft ufficiale (index ${id})`);
        return { assetsDir: officialAssets, assetIndexId: id };
    }

    onProgress({ msg: 'Download del pack Minecraft di default...', pct: 0 });
    const assetsDir = path.join(gameDir, 'assets');
    const indexPath = path.join(assetsDir, 'indexes', `${id}.json`);
    if (!fs.existsSync(indexPath)) {
        await downloadFile(assetIndex.url, indexPath);
    }
    let index;
    try { index = JSON.parse(fs.readFileSync(indexPath, 'utf8')); } catch (e) { return null; }
    const objects = Object.values(index.objects || {});
    if (objects.length === 0) return { assetsDir, assetIndexId: id };
    const objectsDir = path.join(assetsDir, 'objects');
    let done = 0;
    let cursor = 0;
    const concurrency = 8;
    const worker = async () => {
        while (true) {
            const i = cursor++;
            if (i >= objects.length) return;
            const obj = objects[i];
            const hash = obj.hash;
            const dest = path.join(objectsDir, hash.substring(0, 2), hash);
            if (!fs.existsSync(dest) || fs.statSync(dest).size !== obj.size) {
                try {
                    await downloadFile(`https://resources.download.minecraft.net/${hash.substring(0, 2)}/${hash}`, dest);
                } catch (e) {
                    console.error(`[Default pack] Errore su ${hash}: ${e.message}`);
                }
            }
            done++;
            onProgress({ msg: 'Download del pack Minecraft di default...', pct: done / objects.length });
        }
    };
    await Promise.all(Array.from({ length: Math.min(concurrency, objects.length) }, worker));
    onProgress({ msg: 'Pack Minecraft di default pronto', pct: 1 });
    return { assetsDir, assetIndexId: id };
}

// ===== Modrinth dependencies =====
// Legge l'id reale della mod dal fabric.mod.json dentro il jar (robusto:
// i nomi file Modrinth variano, es. "sodium-fabric-0.7.3+mc1.21.8.jar").
function readModId(jarPath) {
    try {
        const AdmZip = require('adm-zip');
        const zip = new AdmZip(jarPath);
        const entry = zip.getEntry('fabric.mod.json');
        if (entry) {
            const meta = JSON.parse(zip.readAsText(entry));
            if (meta && typeof meta.id === 'string') return meta.id.toLowerCase();
        }
    } catch (e) {
        // jar non valido o senza fabric.mod.json
    }
    return null;
}

function stripVersionSuffix(base) {
    return base.replace(/(?:^|[-_.])(?:v?[\d][\w.+-]*)$/i, '').replace(/[-_.]+$/, '');
}

// Set di "chiavi" per le mod installate: id reali (da fabric.mod.json)
// + base del nome file senza versione (fallback).
function installedModKeys(modsDir) {
    const keys = new Set();
    if (!fs.existsSync(modsDir)) return keys;
    for (const f of fs.readdirSync(modsDir)) {
        const lower = f.toLowerCase();
        if (!lower.endsWith('.jar')) continue;
        const id = readModId(path.join(modsDir, f));
        if (id) keys.add(id);
        const base = lower.replace(/\.jar$/i, '');
        const stripped = stripVersionSuffix(base);
        if (stripped) keys.add(stripped);
    }
    return keys;
}

// Un progetto è considerato già installato se il suo slug (o slug+piattaforma)
// è presente tra le chiavi delle mod installate.
function isProjectInstalled(keys, info) {
    const slug = (info.slug || '').toLowerCase();
    if (!slug) return false;
    if (keys.has(slug)) return true;
    for (const tag of ['fabric', 'forge', 'neoforge', 'quilt', 'fapi']) {
        if (keys.has(slug + '-' + tag) || keys.has(slug + '_' + tag) || keys.has(slug + '.' + tag)) return true;
    }
    return false;
}

// Rimuove versioni precedenti della stessa mod (match per id reale o per
// base del file senza suffisso di versione).
function removeOlderVersions(modsDir, slug) {
    if (!slug || !fs.existsSync(modsDir)) return;
    const s = slug.toLowerCase();
    for (const existing of fs.readdirSync(modsDir)) {
        const lower = existing.toLowerCase();
        if (!lower.endsWith('.jar')) continue;
        const p = path.join(modsDir, existing);
        const id = readModId(p);
        if (id === s) { try { fs.rmSync(p, { force: true }); } catch (e) {} continue; }
        const base = lower.replace(/\.jar$/i, '');
        if (stripVersionSuffix(base) === s) {
            try { fs.rmSync(p, { force: true }); } catch (e) {}
        }
    }
}

async function getProjectInfo(projectId) {
    return await fetchJson(`https://api.modrinth.com/v2/project/${projectId}`, 10000);
}

async function getCompatibleVersions(projectId, mcVersion, loader) {
    const url = `https://api.modrinth.com/v2/project/${projectId}/version?game_versions=${encodeURIComponent(JSON.stringify([mcVersion]))}&loaders=${encodeURIComponent(JSON.stringify([loader]))}`;
    const versions = await fetchJson(url);
    return Array.isArray(versions) ? versions : [];
}

/**
 * Installa un progetto e (ricorsivamente) tutte le sue dipendenze richieste.
 * Salta le dipendenze già presenti nella cartella mods e i cicli.
 */
async function installProjectWithDeps(projectId, mcVersion, loader, modsDir, installedKeys, visited, depth = 0) {
    const results = [];
    if (visited.has(projectId)) return results;
    visited.add(projectId);

    let info;
    try {
        info = await getProjectInfo(projectId);
    } catch (e) {
        throw new Error('Impossibile risolvere il progetto ' + projectId + ': ' + e.message);
    }
    const slug = (info.slug || '').toLowerCase();
    if (isProjectInstalled(installedKeys, info)) return results; // già installata

    const versions = await getCompatibleVersions(projectId, mcVersion, loader);
    if (!versions.length) {
        if (depth === 0) throw new Error(`Nessuna versione di "${info.title || projectId}" compatibile con ${mcVersion} + ${loader}`);
        console.log(`[Modrinth] Dipendenza "${info.title}" non compatibile, saltata`);
        return results;
    }
    const version = versions[0];
    const file = (version.files || []).find(f => f.primary) || (version.files || [])[0];
    if (!file) {
        if (depth === 0) throw new Error(`Nessun file scaricabile per "${info.title || projectId}"`);
        return results;
    }

    // Dipendenze richieste prima del progetto
    for (const dep of (version.dependencies || [])) {
        if (dep.dependency_type !== 'required' || !dep.project_id) continue;
        const depResults = await installProjectWithDeps(dep.project_id, mcVersion, loader, modsDir, installedKeys, visited, depth + 1);
        results.push(...depResults);
    }

    // Se la dipendenza è stata già scaricata nel frattempo la saltiamo
    const dest = path.join(modsDir, file.filename);
    if (!fs.existsSync(dest)) {
        console.log(`[Modrinth] Download ${info.title}: ${file.filename}`);
        await downloadFile(file.url, dest);
    }
    if (slug) installedKeys.add(slug);
    results.push({ fileName: file.filename, title: info.title || slug || projectId, projectId });
    return results;
}

// ===== Fast restart & AppCDS =====
function roleplayClientDir(instancePath) {
    return path.join(instancePath, '.roleplay-client');
}

function restartSignalPath(instancePath) {
    return path.join(roleplayClientDir(instancePath), 'restart.json');
}

// La mod scrive .roleplay-client/restart.json e chiude il gioco:
// il launcher rileva il segnale e riavvia in automatico.
function consumeRestartSignal(instancePath) {
    const p = restartSignalPath(instancePath);
    if (fs.existsSync(p)) {
        try { fs.rmSync(p, { force: true }); } catch (e) {}
        return true;
    }
    return false;
}

// Flag AppCDS: carica l'archivio classi precompilate -> avvio JVM più veloce
function cdsLaunchArgs(instancePath) {
    return ['-Xshare:auto', `-XX:SharedArchiveFile=${path.join(roleplayClientDir(instancePath), 'app.jsa')}`];
}

function findJavaExec(preferred) {
    if (preferred && preferred.trim()) return preferred.trim();
    const candidates = [
        'java', 'javaw',
        process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', 'java.exe') : null
    ].filter(Boolean);
    for (const p of candidates) {
        if (p === 'java' || p === 'javaw') return p;
        if (fs.existsSync(p)) return p;
    }
    return 'java';
}

// Rigenera in background l'archivio AppCDS quando la classpath cambia:
// i riavvii successivi del gioco caricano le classi dall'archivio (molto più veloce).
function ensureCdsArchive(instancePath, classpath, javaExec) {
    try {
        const dir = roleplayClientDir(instancePath);
        if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
        const archive = path.join(dir, 'app.jsa');
        const tmpArchive = path.join(dir, 'app-new.jsa');
        const hashPath = path.join(dir, 'app.jsa.md5');
        const crypto = require('crypto');
        const key = crypto.createHash('md5').update(classpath).digest('hex');
        let current = '';
        if (fs.existsSync(hashPath)) current = fs.readFileSync(hashPath, 'utf8').trim();
        if (fs.existsSync(archive) && current === key) return; // già aggiornato
        const { spawn } = require('child_process');
        const child = spawn(javaExec, ['-Xshare:dump', `-XX:SharedArchiveFile=${tmpArchive}`, '-Xmx2G', '-cp', classpath], {
            stdio: 'ignore',
            detached: true
        });
        child.on('exit', () => {
            try {
                if (fs.existsSync(tmpArchive)) {
                    fs.rmSync(archive, { force: true });
                    fs.renameSync(tmpArchive, archive);
                    fs.writeFileSync(hashPath, key);
                    console.log('[CDS] Archivio aggiornato:', archive);
                }
            } catch (e) { console.log('[CDS] refresh error:', e.message); }
        });
        child.unref();
        console.log('[CDS] Rigenerazione archivio in background...');
    } catch (e) {
        console.log('[CDS] dump error:', e.message);
    }
}

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1280,
        height: 800,
        minWidth: 900,
        minHeight: 600,
        frame: false,
        backgroundColor: '#0B1D39',
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true,
            enableRemoteModule: false,
            sandbox: false,
            preload: path.join(__dirname, 'preload.js')
        },
        icon: path.join(__dirname, '..', 'assets', 'icon.png'),
        show: false
    });

    mainWindow.loadFile(path.join(__dirname, '..', 'public', 'index.html'));

    // Show window when ready
    mainWindow.once('ready-to-show', () => {
        mainWindow.show();
    });

    // Open DevTools in development
    if (isDev) {
        mainWindow.webContents.openDevTools();
    }

    mainWindow.on('closed', () => {
        mainWindow = null;
    });
}

// IPC Handlers
ipcMain.handle('window-minimize', () => {
    if (mainWindow && !mainWindow.isDestroyed()) mainWindow.minimize();
});

ipcMain.handle('window-maximize', () => {
    if (!mainWindow || mainWindow.isDestroyed()) return;
    if (mainWindow.isMaximized()) mainWindow.unmaximize();
    else mainWindow.maximize();
});

ipcMain.handle('window-close', () => {
    if (mainWindow && !mainWindow.isDestroyed()) mainWindow.close();
});

ipcMain.handle('get-config', () => {
    return getConfig();
});

ipcMain.handle('save-config', (event, incoming) => {
    try {
        if (!incoming || typeof incoming !== 'object' || Array.isArray(incoming)) {
            throw new Error('Config non valida');
        }
        const current = getConfig();
        const next = {
            ...current,
            settings: { ...current.settings, ...(incoming.settings && typeof incoming.settings === 'object' ? incoming.settings : {}) },
            lastUsername: typeof incoming.lastUsername === 'string' ? incoming.lastUsername : current.lastUsername,
            lastAccountType: incoming.lastAccountType ?? current.lastAccountType,
            lastProfileId: typeof incoming.lastProfileId === 'string' ? incoming.lastProfileId : current.lastProfileId,
            profiles: Array.isArray(incoming.profiles) ? incoming.profiles : current.profiles,
            accounts: Array.isArray(incoming.accounts) ? incoming.accounts : current.accounts
        };
        // Non accettare campi arbitrari top-level
        saveConfig(next);
        return { success: true };
    } catch (e) {
        console.error('save-config error:', e.message);
        return { success: false, error: e.message };
    }
});

ipcMain.handle('get-instances', () => {
    return getInstances();
});

ipcMain.handle('save-instances', (event, instances) => {
    saveInstances(instances);
    return { success: true };
});

ipcMain.handle('create-instance', (event, instanceData) => {
    const instances = getInstances();
    const id = instanceData.id
        ? safeInstanceId(instanceData.id)
        : safeInstanceId(String(instanceData.name || 'profile').toLowerCase().replace(/\s+/g, '-') + '-' + Date.now());

    if (instances.instances.some(i => i.id === id)) {
        instances.activeId = id;
        saveInstances(instances);
        return { success: true, instance: instances.instances.find(i => i.id === id) };
    }

    const newInstance = {
        id,
        name: instanceData.name || id,
        mcVersion: instanceData.mcVersion || '1.21.8',
        fabricLoaderVersion: instanceData.fabricLoaderVersion || instanceData.fabricLoaderOverride || 'latest',
        ramMB: instanceData.ramMB || 4096,
        isFabric: instanceData.isFabric !== false,
        isServerProfile: instanceData.isServerProfile || false,
        iconColor: instanceData.iconColor || '#FCAD14',
        createdAt: new Date().toISOString()
    };

    const instancePath = path.join(APP_DATA_PATH, 'instances', id);
    if (!fs.existsSync(instancePath)) {
        fs.mkdirSync(instancePath, { recursive: true });
        fs.mkdirSync(path.join(instancePath, 'mods'), { recursive: true });
        fs.mkdirSync(path.join(instancePath, 'config'), { recursive: true });
        fs.mkdirSync(path.join(instancePath, 'saves'), { recursive: true });
        fs.mkdirSync(path.join(instancePath, 'resourcepacks'), { recursive: true });
    }

    instances.instances.push(newInstance);
    instances.activeId = id;
    saveInstances(instances);

    return { success: true, instance: newInstance };
});

ipcMain.handle('delete-instance', (event, instanceId) => {
    try {
        const id = safeInstanceId(instanceId);
        const instances = getInstances();
        instances.instances = instances.instances.filter(i => i.id !== id);
        if (instances.activeId === id) {
            instances.activeId = instances.instances.length > 0 ? instances.instances[0].id : null;
        }
        saveInstances(instances);

        const instancePath = path.join(APP_DATA_PATH, 'instances', id);
        const resolved = validatePath(instancePath, [path.join(APP_DATA_PATH, 'instances')]);
        if (fs.existsSync(resolved)) {
            fs.rmSync(resolved, { recursive: true, force: true });
        }
        return { success: true };
    } catch (e) {
        return { success: false, error: e.message };
    }
});

// ===== Import da altri launcher =====
const importer = require(path.join(__dirname, '..', 'lib', 'importer'));

ipcMain.handle('import:detect', async () => {
    try {
        const sources = importer.detectImportSources();
        return { success: true, sources };
    } catch (e) {
        console.error('import:detect', e);
        return { success: false, error: e.message, sources: [] };
    }
});

ipcMain.handle('import:scan-folder', async () => {
    try {
        const result = await dialog.showOpenDialog(mainWindow, {
            title: 'Seleziona cartella gioco (es. .minecraft o profilo)',
            properties: ['openDirectory']
        });
        if (result.canceled || !result.filePaths.length) {
            return { success: true, cancelled: true, source: null };
        }
        const gameDir = result.filePaths[0];
        const source = importer.analyzeGameDir(gameDir, 'Cartella personalizzata', path.basename(gameDir));
        if (!source) {
            return { success: false, error: 'Cartella non valida: nessun dato Minecraft rilevato.', source: null };
        }
        return { success: true, source };
    } catch (e) {
        return { success: false, error: e.message, source: null };
    }
});

ipcMain.handle('import:run', async (event, payload) => {
    let createdId = null;
    try {
        const gameDir = payload && payload.gameDir;
        const categories = Array.isArray(payload?.categories) ? payload.categories : [];
        if (!gameDir || typeof gameDir !== 'string') {
            return { success: false, error: 'gameDir mancante' };
        }
        const resolvedSrc = path.resolve(gameDir);
        if (!fs.existsSync(resolvedSrc) || !fs.statSync(resolvedSrc).isDirectory()) {
            return { success: false, error: 'Cartella sorgente non trovata' };
        }

        // Re-validate it's a plausible game dir
        const analyzed = importer.analyzeGameDir(resolvedSrc, payload.launcher || 'Import', payload.profileName || path.basename(resolvedSrc));
        if (!analyzed) {
            return { success: false, error: 'Sorgente non valida' };
        }

        let instanceId = payload.instanceId ? safeInstanceId(payload.instanceId) : null;
        const config = getConfig();
        if (!config.profiles) config.profiles = [];
        if (!config.settings) config.settings = {};

        if (payload.newProfile || !instanceId) {
            const baseName = String(payload.newProfileName || `Importato da ${analyzed.profileName || 'launcher'}`).trim() || 'Importato';
            const id = safeInstanceId(
                baseName.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') + '-' + Date.now()
            );
            const profile = {
                id,
                name: baseName,
                mcVersion: analyzed.mcVersion || config.settings.mcVersionOverride || '1.21.8',
                ramMB: config.settings.ramMB || 4096,
                isFabric: true,
                fabricLoaderOverride: analyzed.fabricLoaderVersion || config.settings.fabricLoaderOverride || '',
                iconColor: '#FCAD14'
            };
            const createRes = await (async () => {
                // reuse create-instance logic inline
                const instances = getInstances();
                if (instances.instances.some(i => i.id === id)) {
                    return { success: true, instance: instances.instances.find(i => i.id === id) };
                }
                const newInstance = {
                    id,
                    name: profile.name,
                    mcVersion: profile.mcVersion,
                    fabricLoaderVersion: profile.fabricLoaderOverride || 'latest',
                    ramMB: profile.ramMB,
                    isFabric: true,
                    isServerProfile: false,
                    iconColor: profile.iconColor,
                    createdAt: new Date().toISOString()
                };
                const instancePath = path.join(APP_DATA_PATH, 'instances', id);
                fs.mkdirSync(instancePath, { recursive: true });
                for (const sub of ['mods', 'config', 'saves', 'resourcepacks', 'shaderpacks']) {
                    fs.mkdirSync(path.join(instancePath, sub), { recursive: true });
                }
                instances.instances.push(newInstance);
                instances.activeId = id;
                saveInstances(instances);
                return { success: true, instance: newInstance };
            })();

            if (!createRes.success) {
                return { success: false, error: 'Impossibile creare il profilo' };
            }
            createdId = id;
            instanceId = id;
            if (!config.profiles.some(p => p.id === id)) {
                config.profiles.push(profile);
            }
            config.lastProfileId = id;
            saveConfig(config);
        } else {
            const instances = getInstances();
            if (!instances.instances.some(i => i.id === instanceId)) {
                return { success: false, error: 'Profilo destinazione non trovato' };
            }
            instances.activeId = instanceId;
            saveInstances(instances);
            config.lastProfileId = instanceId;
            saveConfig(config);
        }

        const destPath = path.join(APP_DATA_PATH, 'instances', instanceId);
        validatePath(destPath, [path.join(APP_DATA_PATH, 'instances')]);

        const bundledModsDir = path.join(__dirname, '..', 'assets', 'mods');
        const stats = await importer.runImport(resolvedSrc, destPath, categories, { bundledModsDir });

        return {
            success: true,
            instanceId,
            stats,
            profileName: (config.profiles.find(p => p.id === instanceId) || {}).name || instanceId
        };
    } catch (e) {
        console.error('import:run', e);
        if (createdId) {
            try {
                const instances = getInstances();
                instances.instances = instances.instances.filter(i => i.id !== createdId);
                if (instances.activeId === createdId) {
                    instances.activeId = instances.instances[0]?.id || null;
                }
                saveInstances(instances);
                const p = path.join(APP_DATA_PATH, 'instances', createdId);
                if (fs.existsSync(p)) fs.rmSync(p, { recursive: true, force: true });
                const cfg = getConfig();
                if (cfg.profiles) {
                    cfg.profiles = cfg.profiles.filter(x => x.id !== createdId);
                    if (cfg.lastProfileId === createdId) cfg.lastProfileId = cfg.profiles[0]?.id || 'default';
                    saveConfig(cfg);
                }
            } catch (rollbackErr) {
                console.error('import rollback', rollbackErr);
            }
        }
        return { success: false, error: e.message || String(e) };
    }
});

ipcMain.handle('select-java-path', async () => {
    const result = await dialog.showOpenDialog(mainWindow, {
        title: 'Seleziona Java',
        filters: [
            { name: 'Java Executable', extensions: ['exe'] },
            { name: 'All Files', extensions: ['*'] }
        ],
        properties: ['openFile']
    });

    if (!result.canceled && result.filePaths.length > 0) {
        return result.filePaths[0];
    }
    return null;
});

ipcMain.handle('select-minecraft-dir', async () => {
    const result = await dialog.showOpenDialog(mainWindow, {
        title: 'Seleziona Cartella Minecraft',
        properties: ['openDirectory']
    });

    if (!result.canceled && result.filePaths.length > 0) {
        return result.filePaths[0];
    }
    return null;
});

ipcMain.handle('select-mod-file', async () => {
    const result = await dialog.showOpenDialog(mainWindow, {
        title: 'Seleziona Mod',
        filters: [
            { name: 'Mod Files', extensions: ['jar'] },
            { name: 'All Files', extensions: ['*'] }
        ],
        properties: ['openFile', 'multiSelections']
    });

    if (!result.canceled && result.filePaths.length > 0) {
        return result.filePaths;
    }
    return null;
});

ipcMain.handle('get-instance-path', async (event, instanceId) => {
    const id = safeInstanceId(instanceId);
    const instancePath = path.join(APP_DATA_PATH, 'instances', id);
    validatePath(instancePath, [path.join(APP_DATA_PATH, 'instances')]);
    if (!fs.existsSync(instancePath)) {
        fs.mkdirSync(instancePath, { recursive: true });
        fs.mkdirSync(path.join(instancePath, 'mods'), { recursive: true });
        fs.mkdirSync(path.join(instancePath, 'config'), { recursive: true });
        fs.mkdirSync(path.join(instancePath, 'saves'), { recursive: true });
        fs.mkdirSync(path.join(instancePath, 'resourcepacks'), { recursive: true });
    }
    return instancePath;
});

const skinCache = new Map();

ipcMain.handle('get-skin', async (event, { uuid } = {}) => {
    if (!uuid) return { dataUrl: null };
    const cleanUuid = String(uuid).replace(/-/g, '').toLowerCase();
    if (skinCache.has(cleanUuid)) return { dataUrl: skinCache.get(cleanUuid) };
    try {
        const profile = await fetchJson(`https://sessionserver.mojang.com/session/minecraft/profile/${cleanUuid}`, 10000);
        const prop = (profile?.properties || []).find(p => p.name === 'textures');
        if (!prop?.value) return { dataUrl: null };
        const decoded = JSON.parse(Buffer.from(prop.value, 'base64').toString('utf8'));
        const skinUrl = decoded?.textures?.SKIN?.url;
        if (!skinUrl) return { dataUrl: null };
        const res = await fetch(skinUrl, { headers: { 'User-Agent': 'RoleplayClient/1.0.0 (Minecraft launcher)' } });
        if (!res.ok) return { dataUrl: null };
        const buf = Buffer.from(await res.arrayBuffer());
        const dataUrl = `data:image/png;base64,${buf.toString('base64')}`;
        skinCache.set(cleanUuid, dataUrl);
        return { dataUrl };
    } catch (e) {
        console.error('get-skin error:', e.message);
        return { dataUrl: null };
    }
});

ipcMain.handle('open-external', (event, url) => {
    try {
        const u = new URL(String(url || ''));
        if (u.protocol !== 'https:' && u.protocol !== 'http:') {
            throw new Error('Schema non consentito');
        }
        shell.openExternal(u.toString());
    } catch (e) {
        console.error('open-external rifiutato:', e.message);
    }
});

ipcMain.handle('open-path', async (event, targetPath) => {
    try {
        const resolved = validatePath(targetPath, [path.join(APP_DATA_PATH, 'instances')]);
        if (!fs.existsSync(resolved)) fs.mkdirSync(resolved, { recursive: true });
        const err = await shell.openPath(resolved);
        if (err) throw new Error(err);
        return { success: true };
    } catch (e) {
        console.error('open-path error:', e.message);
        return { success: false, error: e.message };
    }
});

ipcMain.handle('get-app-data-path', () => {
    return APP_DATA_PATH;
});

ipcMain.handle('get-launcher-mods-path', () => {
    return path.join(__dirname, '..', 'assets', 'mods');
});

function validatePath(requested, allowedRoots) {
    let resolved = path.resolve(requested);
    try {
        if (fs.existsSync(resolved)) resolved = fs.realpathSync(resolved);
    } catch (_) {}
    for (const root of allowedRoots) {
        let resolvedRoot = path.resolve(root);
        try {
            if (fs.existsSync(resolvedRoot)) resolvedRoot = fs.realpathSync(resolvedRoot);
        } catch (_) {}
        if (resolved === resolvedRoot || resolved.startsWith(resolvedRoot + path.sep)) {
            return resolved;
        }
    }
    throw new Error('Accesso non autorizzato al percorso');
}

function fsAllowedRoots() {
    return [
        path.join(APP_DATA_PATH, 'instances'),
        path.join(APP_DATA_PATH, 'logs'),
        path.join(__dirname, '..', 'assets', 'mods')
    ];
}

ipcMain.handle('read-file', (event, filePath) => {
    try {
        const resolved = validatePath(filePath, fsAllowedRoots());
        // Blocca lettura config/token
        if (resolved.endsWith('config.json') || resolved.includes(`${path.sep}accounts`)) {
            throw new Error('Lettura protetta');
        }
        if (fs.existsSync(resolved)) {
            return fs.readFileSync(resolved, 'utf8');
        }
        return null;
    } catch (e) {
        console.error('read-file error:', e.message);
        return null;
    }
});

ipcMain.handle('write-file', (event, filePath, content) => {
    try {
        const resolved = validatePath(filePath, fsAllowedRoots());
        if (resolved.endsWith('config.json') || resolved.endsWith('instances.json')) {
            throw new Error('Scrittura protetta');
        }
        const dir = path.dirname(resolved);
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, { recursive: true });
        }
        const tmp = resolved + '.tmp';
        fs.writeFileSync(tmp, content);
        fs.renameSync(tmp, resolved);
        return { success: true };
    } catch (e) {
        console.error('write-file error:', e.message);
        return { success: false, error: e.message };
    }
});

ipcMain.handle('list-files', (event, dirPath) => {
    try {
        const resolved = validatePath(dirPath, fsAllowedRoots());
        if (!fs.existsSync(resolved)) {
            return [];
        }
        return fs.readdirSync(resolved);
    } catch (e) {
        console.error('list-files error:', e.message);
        return [];
    }
});

ipcMain.handle('copy-file', (event, source, destination) => {
    try {
        const allowedSrc = [...fsAllowedRoots(), path.join(__dirname, '..', 'assets')];
        const src = validatePath(source, allowedSrc);
        const dst = validatePath(destination, fsAllowedRoots());
        fs.copyFileSync(src, dst);
        return { success: true };
    } catch (error) {
        console.error('copy-file error:', error.message);
        return { success: false, error: error.message };
    }
});

// ===== Mods Management =====

// Icone Modrinth per le mod esterne installate, con cache di 24h.
const MOD_ICONS_TTL = 24 * 60 * 60 * 1000;

function modIconsCachePath() {
    return path.join(APP_DATA_PATH, 'mod-icons.json');
}

function loadModIconsCache() {
    try {
        const data = JSON.parse(fs.readFileSync(modIconsCachePath(), 'utf8'));
        if (data && typeof data.ts === 'number' && data.icons) return data;
    } catch (e) {}
    return { ts: 0, icons: {} };
}

function saveModIconsCache(cache) {
    try {
        fs.mkdirSync(APP_DATA_PATH, { recursive: true });
        fs.writeFileSync(modIconsCachePath(), JSON.stringify(cache));
    } catch (e) {}
}

// Slug candidati per trovare il progetto Modrinth dal nome del file.
function iconCandidates(fileName) {
    const base = fileName.replace(/\.jar$/i, '').toLowerCase();
    const out = new Set();
    for (const s of [stripVersionSuffix(base), base]) {
        if (!s) continue;
        out.add(s.replace(/_/g, '-').replace(/\./g, '-'));
    }
    return [...out];
}

async function attachModrinthIcons(mods) {
    const cache = loadModIconsCache();
    const now = Date.now();
    const fresh = now - cache.ts < MOD_ICONS_TTL;
    const icons = cache.icons;

    const fileCands = new Map();
    const toFetch = new Set();
    for (const m of mods) {
        if (/^roleplayclient|^fabric-api/i.test(m.fileName)) continue;
        const cands = iconCandidates(m.fileName);
        fileCands.set(m.fileName, cands);
        let icon = null;
        for (const c of cands) {
            if (icons[c]) { icon = icons[c]; break; }
        }
        if (icon) { m.iconUrl = icon; continue; }
        if (!fresh) for (const c of cands) toFetch.add(c);
    }

    if (toFetch.size > 0) {
        try {
            const ids = JSON.stringify([...toFetch]);
            const res = await fetchJsonRetry(`https://api.modrinth.com/v2/projects?ids=${encodeURIComponent(ids)}`, 2);
            if (Array.isArray(res)) {
                for (const p of res) {
                    if (p && p.slug && p.icon_url) icons[p.slug] = p.icon_url;
                }
            }
            // Fallback per gli slug non risolti dal batch (max 8 chiamate)
            let attempts = 0;
            for (const c of toFetch) {
                if (attempts >= 8 || icons[c]) continue;
                try {
                    const p = await fetchJsonRetry(`https://api.modrinth.com/v2/project/${encodeURIComponent(c)}`, 1);
                    if (p && p.slug && p.icon_url) icons[p.slug] = p.icon_url;
                } catch (e) {}
                attempts++;
            }
            cache.ts = now;
            saveModIconsCache(cache);
        } catch (e) {
            // offline: si riproverà al prossimo avvio
        }
    }

    for (const m of mods) {
        if (m.iconUrl) continue;
        for (const c of fileCands.get(m.fileName) || []) {
            if (icons[c]) { m.iconUrl = icons[c]; break; }
        }
    }
    return mods;
}

ipcMain.handle('list-installed-mods', async (event, instanceId) => {
    ensureBundledMods(instanceId);
    const modsDir = getModsDir(instanceId);
    if (!fs.existsSync(modsDir)) return [];
    // Migrazione: le mod un tempo disattivate (.jar.disabled) vengono riattivate
    for (const f of fs.readdirSync(modsDir)) {
        if (f.toLowerCase().endsWith('.jar.disabled')) {
            try {
                fs.renameSync(path.join(modsDir, f), path.join(modsDir, f.slice(0, -'.disabled'.length)));
            } catch (e) {}
        }
    }
    const result = [];
    for (const f of fs.readdirSync(modsDir)) {
        if (!f.toLowerCase().endsWith('.jar')) continue;
        result.push({
            id: f.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, ''),
            fileName: f,
            name: friendlyModName(f)
        });
    }
    result.sort((a, b) => a.name.localeCompare(b.name));
    return await attachModrinthIcons(result);
});

ipcMain.handle('delete-mod', async (event, fileName, instanceId) => {
    try {
        const modsDir = getModsDir(instanceId);
        if (!fs.existsSync(modsDir)) return { success: true };
        const base = path.basename(String(fileName || ''));
        if (!base.toLowerCase().endsWith('.jar')) return { success: false, error: 'Solo file .jar' };
        const lower = base.toLowerCase();
        if (lower.includes('roleplayclient') || lower.includes('fabric-api') || lower === 'fabric-api.jar') {
            return { success: false, error: 'Mod protetta' };
        }
        const p = validatePath(path.join(modsDir, base), [modsDir]);
        if (fs.existsSync(p)) fs.rmSync(p, { force: true });
        return { success: true };
    } catch (e) {
        return { success: false, error: e.message };
    }
});

ipcMain.handle('install-mod-files', async (event, filePaths, instanceId) => {
    const modsDir = getModsDir(instanceId);
    if (!fs.existsSync(modsDir)) fs.mkdirSync(modsDir, { recursive: true });
    const installed = [];
    for (const src of filePaths || []) {
        if (typeof src !== 'string' || !src.toLowerCase().endsWith('.jar')) continue;
        // Solo path provenienti dal dialog (assoluti esistenti); rifiuta path strani
        const abs = path.resolve(src);
        if (!fs.existsSync(abs) || !abs.toLowerCase().endsWith('.jar')) continue;
        const dest = path.join(modsDir, path.basename(abs));
        if (fs.existsSync(dest)) fs.rmSync(dest, { force: true });
        fs.copyFileSync(abs, dest);
        installed.push(path.basename(abs));
    }
    return { success: true, installed };
});

// ===== Modrinth =====
ipcMain.handle('modrinth-search', async (event, query, options) => {
    const opts = options || {};
    const mcVersion = opts.mcVersion || '1.21.8';
    const loader = opts.loader || 'fabric';
    const index = opts.index || 'relevance';
    const limit = opts.limit || 24;
    const categories = opts.categories || [];

    // Facets: versione + loader + categorie selezionate (toggle stile Modrinth)
    const facetGroups = [[`versions:${mcVersion}`], [`categories:${loader}`]];
    for (const c of categories) {
        if (c) facetGroups.push([`categories:${c}`]);
    }
    const facets = JSON.stringify(facetGroups);

    // Primary: filter by game version AND loader AND categories via facets
    const url = `https://api.modrinth.com/v2/search?query=${encodeURIComponent(query || '')}&facets=${encodeURIComponent(facets)}&limit=${limit}&index=${index}`;
    try {
        const data = await fetchJsonRetry(url);
        if (data && Array.isArray(data.hits) && data.hits.length) return data.hits;
    } catch (e) {
        console.log('[Modrinth] Version facet failed, using fallback:', e.message);
    }

    // Fallback: search without the version facet, then verify compatibility
    // per project (needed when the search facet for a MC version is broken)
    const fbFacets = JSON.stringify([[`categories:${loader}`], ...facetGroups.slice(2)]);
    const fbUrl = `https://api.modrinth.com/v2/search?query=${encodeURIComponent(query || '')}&facets=${encodeURIComponent(fbFacets)}&limit=${limit}&index=${index}`;
    const fbData = await fetchJsonRetry(fbUrl);
    const hits = (fbData && fbData.hits) || [];
    const results = [];
    for (let i = 0; i < hits.length; i += 8) {
        const batch = hits.slice(i, i + 8);
        const flags = await Promise.all(batch.map(h => projectHasVersion(h.project_id, mcVersion, loader)));
        batch.forEach((h, j) => { if (flags[j]) results.push(h); });
    }
    return results;
});

ipcMain.handle('modrinth-download', async (event, projectId, options) => {
    const opts = options || {};
    const mcVersion = opts.mcVersion || '1.21.8';
    const loader = opts.loader || 'fabric';
    const modsDir = getModsDir(opts.instanceId);
    if (!fs.existsSync(modsDir)) fs.mkdirSync(modsDir, { recursive: true });

    const installedKeys = installedModKeys(modsDir);
    const visited = new Set();

    try {
        // Rimuove le versioni vecchie della mod che stiamo installando
        if (opts.slug) {
            removeOlderVersions(modsDir, opts.slug);
            installedKeys.delete(opts.slug.toLowerCase());
        }

        const installed = await installProjectWithDeps(projectId, mcVersion, loader, modsDir, installedKeys, visited);
        if (!installed.length) {
            return { success: true, installed: [], mainFileName: null, dependencies: [], alreadyInstalled: true };
        }
        const main = installed.find(i => i.projectId === projectId) || installed[0];
        const dependencies = installed.filter(i => i.projectId !== projectId);
        return {
            success: true,
            installed: installed.map(i => i.fileName),
            mainFileName: main.fileName,
            dependencies: dependencies.map(d => d.title)
        };
    } catch (e) {
        console.error('[Modrinth] Install error:', e.message);
        return { success: false, error: e.message };
    }
});

// Fabric Installer
const FabricInstaller = require(path.join(__dirname, '..', 'lib', 'fabric-installer.js'));

// Minecraft Launch Handler - Using MCLC for download and launch
ipcMain.handle('launch-minecraft', async (event, launchData) => {
    console.log('Launching Minecraft…');

    return new Promise(async (resolve) => {
        try {
            const { Client } = require('minecraft-launcher-core');
            const launcher = new Client();
            
            const { instancePath, mcVersion, ramMB, javaPath, jvmArgs, account, isFabric } = launchData;

            // Vincola instancePath sotto APP_DATA/instances
            const instancesRoot = path.join(APP_DATA_PATH, 'instances');
            validatePath(instancePath, [instancesRoot]);
            ensureBundledMods(path.basename(instancePath));
            try { fs.mkdirSync(roleplayClientDir(instancePath), { recursive: true }); } catch (e) {}
            
            // Install Fabric if needed
            let fabricLoaderPath = null;
            if (isFabric !== false) { // Default to Fabric unless explicitly false
                try {
                    console.log('Checking/installing Fabric...');
                    const installer = new FabricInstaller();
                    const fabricResult = await installer.installFabric(mcVersion, instancePath, javaPath, launchData.fabricLoaderOverride);
                    console.log('Fabric result:', fabricResult);
                    
                    // Get the fabric loader JAR path
                    const loaderVersion = fabricResult.loaderVersion;
                    fabricLoaderPath = path.join(instancePath, 'libraries', 'net', 'fabricmc', 'fabric-loader', loaderVersion, `fabric-loader-${loaderVersion}.jar`);
                    
                    if (!fs.existsSync(fabricLoaderPath)) {
                        // Try version dir
                        fabricLoaderPath = path.join(instancePath, 'versions', `fabric-loader-${loaderVersion}-${mcVersion}`, `fabric-loader-${loaderVersion}.jar`);
                    }
                    
                    console.log('Fabric loader path:', fabricLoaderPath);
                    console.log('Fabric loader exists:', fs.existsSync(fabricLoaderPath));
                    
                    // Collect ALL Fabric library JARs for classpath
                    const fabricLibsDir = path.join(instancePath, 'libraries', 'net', 'fabricmc');
                    const findJars = (dir) => {
                        const results = [];
                        if (!fs.existsSync(dir)) return results;
                        const items = fs.readdirSync(dir);
                        for (const item of items) {
                            const fullPath = path.join(dir, item);
                            try {
                                if (fs.statSync(fullPath).isDirectory()) {
                                    results.push(...findJars(fullPath));
                                } else if (item.endsWith('.jar')) {
                                    results.push(fullPath);
                                }
                            } catch(e) {}
                        }
                        return results;
                    };
                    
                    const allFabricJars = findJars(fabricLibsDir);
                    console.log('All Fabric JARs found:', allFabricJars.length);
                    allFabricJars.forEach(j => console.log('  -', j));
                } catch (fabricError) {
                    console.error('Fabric installation error:', fabricError.message);
                    console.log('Continuing without Fabric...');
                }
            }
            
            // Load all mods from the instance mods directory
            const modsDir = path.join(instancePath, 'mods');
            let mods = [];
            if (fs.existsSync(modsDir)) {
                mods = fs.readdirSync(modsDir).filter(f => f.endsWith('.jar')).map(modFile => path.join(modsDir, modFile));
                console.log('Loading mods:', mods);
            }
            
            // Build authorization object
            let authorization;
            console.log('Account type:', account?.type, 'name:', account?.name || account?.profile?.name);
            
            if (account && account.type === 'microsoft') {
                // Microsoft account - try multiple token formats
                console.log('Account keys:', Object.keys(account).filter(k => !/token|profile|encrypted/i.test(k)));
                
                // Format 1: New format with mclcToken (best case)
                if (account.mclcToken && account.mclcToken.access_token) {
                    authorization = account.mclcToken;
                    console.log('Using saved MCLC token for authorization');
                    console.log('Authorization name:', authorization.name);
                    console.log('Authorization uuid:', authorization.uuid);
                
                // Format 2: New format with mcTokenSerialized
                } else if (account.mcTokenSerialized && account.mcTokenSerialized.mcToken) {
                    console.log('Reconstructing authorization from serialized token...');
                    authorization = {
                        access_token: account.mcTokenSerialized.mcToken,
                        client_token: require('crypto').randomUUID(),
                        uuid: account.mcTokenSerialized.profile?.id || account.uuid,
                        name: account.mcTokenSerialized.profile?.name || account.name,
                        meta: {
                            xuid: account.mcTokenSerialized.xuid,
                            type: 'msa',
                            demo: account.mcTokenSerialized.profile?.demo || false
                        },
                        user_properties: {}
                    };
                    console.log('Reconstructed authorization name:', authorization.name);
                
                // Format 3: Old format - mcToken was saved as raw MSMC object
                } else if (account.mcToken && account.mcToken.mcToken) {
                    console.log('Using legacy mcToken.mcToken format (old save)...');
                    authorization = {
                        access_token: account.mcToken.mcToken,
                        client_token: require('crypto').randomUUID(),
                        uuid: account.mcToken.profile?.id || account.uuid,
                        name: account.mcToken.profile?.name || account.name,
                        meta: {
                            xuid: account.mcToken.xuid,
                            type: 'msa',
                            demo: account.mcToken.profile?.demo || false,
                            exp: account.mcToken.exp
                        },
                        user_properties: {}
                    };
                    console.log('Migrated legacy token for:', authorization.name);
                    console.log('WARNING: Token is in old format. User should re-login to save in new format.');
                
                // Format 4: Very old format with accessToken
                } else if (account.accessToken && account.accessToken !== 'offline') {
                    console.log('Using legacy accessToken format');
                    authorization = {
                        access_token: account.accessToken,
                        client_token: account.uuid || require('crypto').randomUUID(),
                        uuid: account.uuid,
                        name: account.name,
                        meta: { type: 'msa' },
                        user_properties: {}
                    };
                
                // No valid token found
                } else {
                    console.log('No valid Microsoft token found, falling back to offline');
                    console.log('Account data:', JSON.stringify({
                        ...account,
                        mcToken: account.mcToken ? 'present' : 'missing',
                        mclcToken: account.mclcToken ? 'present' : 'missing',
                        mcTokenSerialized: account.mcTokenSerialized ? 'present' : 'missing',
                        accessToken: account.accessToken || 'missing'
                    }));
                    authorization = {
                        access_token: 'offline',
                        client_token: require('crypto').randomUUID(),
                        uuid: generateUUID(account.name),
                        name: account.name,
                        user_properties: {}
                    };
                }
            } else if (account && account.name) {
                // Offline account
                authorization = {
                    access_token: 'offline',
                    client_token: require('crypto').randomUUID(),
                    uuid: generateUUID(account.name),
                    name: account.name,
                    user_properties: {}
                };
                console.log('Using offline authorization');
            } else {
                // No account - use offline
                authorization = {
                    access_token: 'offline',
                    client_token: require('crypto').randomUUID(),
                    uuid: generateUUID('Player'),
                    name: 'Player',
                    user_properties: {}
                };
                console.log('Using default offline authorization');
            }
            
            // Build classpath for Fabric if installed
            let customArgs = jvmArgs ? jvmArgs.split(' ') : [];
            let fabricMainClass = null;
            
            if (fabricLoaderPath && fs.existsSync(fabricLoaderPath)) {
                console.log('Adding Fabric to classpath...');
                
                // Find ALL Fabric-related JARs
                const fabricLibsDir = path.join(instancePath, 'libraries', 'net', 'fabricmc');
                const findJars = (dir) => {
                    const results = [];
                    if (!fs.existsSync(dir)) return results;
                    const items = fs.readdirSync(dir);
                    for (const item of items) {
                        const fullPath = path.join(dir, item);
                        try {
                            if (fs.statSync(fullPath).isDirectory()) {
                                results.push(...findJars(fullPath));
                            } else if (item.endsWith('.jar')) {
                                results.push(fullPath);
                            }
                        } catch(e) {}
                    }
                    return results;
                };
                
                const fabricJars = findJars(fabricLibsDir);
                console.log('Fabric JARs for classpath:', fabricJars.length);
                
                // Add Fabric libraries to classpath
                // MCLC builds classpath internally, so we need to use customLaunchArgs
                // But customLaunchArgs are added BEFORE the classpath, not after
                // So we need a different approach - we'll modify the process arguments after launch
                
                fabricMainClass = 'net.fabricmc.loader.launch.knot.KnotClient';
                
                console.log('Fabric main class:', fabricMainClass);
                console.log('Will need to modify classpath after MCLC builds it');
            }
            
            const opts = {
                clientPackage: null,
                authorization: authorization,
                root: instancePath,
                version: {
                    number: mcVersion || '1.21.8',
                    type: 'release'
                },
                memory: {
                    max: `${ramMB || 4096}M`,
                    min: '1024M'
                },
                forge: null,
                fabric: null,
                customArgs: customArgs
            };
            
            console.log('Launch version:', opts.version.number);
            
            // Add Java path if specified
            if (javaPath) {
                opts.javaPath = javaPath;
            }
            
            // Add JVM arguments (with AppCDS for faster restarts)
            opts.customArgs = [...cdsLaunchArgs(instancePath), ...(jvmArgs ? jvmArgs.split(' ') : [])];
            console.log('Launch options:', JSON.stringify({ ...opts, authorization: { ...opts.authorization, access_token: '...' } }, null, 2));
            
            // If Fabric is installed, we need to launch it ourselves
            if (fabricLoaderPath && fs.existsSync(fabricLoaderPath)) {
                console.log('\n=== LAUNCHING WITH FABRIC ===');
                
                // Find Java
                const findJava = () => {
                    const paths = [
                        'java',
                        'javaw',
                        'C:/Program Files/Java/jdk-23/bin/java.exe',
                        'C:/Program Files/Eclipse Adoptium/jdk-23.0.1+9-hotspot/bin/java.exe'
                    ];
                    for (const p of paths) {
                        try {
                            require('child_process').execSync(`"${p}" -version 2>&1`, { encoding: 'utf8', timeout: 5000 });
                            return p;
                        } catch(e) {}
                    }
                    return 'java';
                };
                
                const javaPathFound = javaPath || findJava();
                console.log('Using Java:', javaPathFound);
                
                // Ensure MC JAR is downloaded
                const installer = new FabricInstaller();
                let mcJarPath;
                try {
                    mcJarPath = await installer.ensureMinecraftJar(mcVersion, instancePath);
                    console.log('MC JAR:', mcJarPath);
                } catch (mcError) {
                    console.error('Failed to get MC JAR:', mcError.message);
                    resolve({ success: false, error: 'Failed to download Minecraft JAR: ' + mcError.message });
                    return;
                }
                
                // Ensure the default Minecraft pack (sounds, textures, fonts) is available
                let defaultPack = null;
                try {
                    defaultPack = await ensureDefaultPack(instancePath, mcVersion, (p) => {
                        event.sender.send('launch-progress', p);
                    });
                } catch (packError) {
                    console.error('Default pack:', packError.message);
                }
                
                // Find ALL JARs in libraries
                const libsDir = path.join(instancePath, 'libraries');
                const findAllJars = (dir) => {
                    const results = [];
                    if (!fs.existsSync(dir)) return results;
                    const items = fs.readdirSync(dir);
                    for (const item of items) {
                        const fullPath = path.join(dir, item);
                        try {
                            if (fs.statSync(fullPath).isDirectory()) {
                                results.push(...findAllJars(fullPath));
                            } else if (item.endsWith('.jar')) {
                                results.push(fullPath);
                            }
                        } catch(e) {}
                    }
                    return results;
                };
                
                // Get MC libs (exclude fabricmc and mixin, keep asm but deduplicated)
                const toPosix = (p) => p.split(path.sep).join('/');
                const mcLibsRaw = findAllJars(libsDir).filter(j => {
                    const n = toPosix(j);
                    // Exclude Fabric libs (added separately below)
                    if (n.includes('/fabricmc/')) return false;
                    // Exclude mixin - Fabric bundles its own
                    if (n.includes('sponge-mixin') || n.includes('/mixin/')) return false;
                    return true;
                });

                // Deduplicate libraries keeping the highest version of each artifact.
                // MC 1.21.8 ships asm 9.6 but Fabric loader 0.19.x requires asm 9.10.1:
                // putting both on the classpath makes Fabric refuse to start
                // ("duplicate ASM classes found on classpath").
                const versionParts = (v) => String(v).split(/[^0-9]+/).filter(Boolean).map(Number);
                const cmpVersions = (a, b) => {
                    const A = versionParts(a), B = versionParts(b);
                    for (let i = 0; i < Math.max(A.length, B.length); i++) {
                        const d = (A[i] || 0) - (B[i] || 0);
                        if (d !== 0) return d;
                    }
                    return 0;
                };
                const byArtifact = new Map();
                for (const jar of mcLibsRaw) {
                    const n = toPosix(jar).replace(/\.jar$/i, '');
                    const parts = n.split('/');
                    const file = parts[parts.length - 1];
                    const version = parts[parts.length - 2];
                    const artifact = parts[parts.length - 3];
                    // Keep natives/classifiers as separate keys so they aren't dropped
                    let classifier = file.startsWith(`${artifact}-${version}`) ? file.slice((`${artifact}-${version}`).length) : '';
                    if (classifier.startsWith('-')) classifier = classifier.slice(1);
                    const key = `${parts.slice(0, -3).join('/')}/${artifact}${classifier ? '/' + classifier : ''}`;
                    const existing = byArtifact.get(key);
                    if (!existing || cmpVersions(version, existing.version) > 0) {
                        byArtifact.set(key, { version, jar });
                    }
                }
                const mcLibs = [...byArtifact.values()].map(v => v.jar);

                const asmJars = mcLibs.filter(j => toPosix(j).includes('/ow2/asm/'));
                console.log('MC libs raw:', mcLibsRaw.length, '-> deduped:', mcLibs.length);
                console.log('ASM jars on classpath:', asmJars.length);
                asmJars.forEach(j => console.log('  -', j));

                // Get Fabric libs
                const fabricLibsDir = path.join(instancePath, 'libraries', 'net', 'fabricmc');
                const fabricJars = findAllJars(fabricLibsDir);
                
                console.log('MC libs:', mcLibs.length);
                console.log('Fabric libs:', fabricJars.length);
                
                // Build classpath: MC jar first, then MC libs, then Fabric
                const classpath = [mcJarPath, ...mcLibs, ...fabricJars].join(path.delimiter);
                
                // Build JVM args
                const allJvmArgs = [
                    `-Xmx${ramMB || 4096}M`,
                    '-Xms1024M',
                    ...cdsLaunchArgs(instancePath),
                    `-Djava.library.path=${instancePath}`,
                    `-Dfabric.classPath=${libsDir}`,
                    ...(jvmArgs ? jvmArgs.split(' ') : []),
                    '-cp', classpath,
                    'net.fabricmc.loader.launch.knot.KnotClient'
                ];
                
                // Build game args
                const gameArgs = [
                    '--username', authorization.name,
                    '--version', mcVersion,
                    '--gameDir', instancePath,
                    '--assetsDir', defaultPack ? defaultPack.assetsDir : path.join(instancePath, 'assets'),
                    '--assetIndex', defaultPack ? defaultPack.assetIndexId : mcVersion,
                    '--uuid', authorization.uuid,
                    '--accessToken', authorization.access_token,
                    '--clientId', authorization.client_token || require('crypto').randomUUID(),
                    '--xuid', authorization.meta?.xuid || '',
                    '--userType', 'msa',
                    '--versionType', 'release'
                ];
                
                const allArgs = [...allJvmArgs, ...gameArgs];
                
                console.log('Main class: net.fabricmc.loader.launch.knot.KnotClient');
                console.log('Classpath entries:', classpath.split(path.delimiter).length);
                
                // Spawn the process
                const { spawn } = require('child_process');
                const mcProcess = spawn(javaPathFound, allArgs, {
                    cwd: instancePath,
                    stdio: ['pipe', 'pipe', 'pipe']
                });

                // Rigenera l'archivio CDS in background se la classpath è cambiata
                ensureCdsArchive(instancePath, classpath, javaPathFound);

                // Risolvi subito allo spawn (UX: toast "avviato" non a chiusura)
                resolve({ success: true, message: 'Minecraft avviato!', started: true });
                
                mcProcess.stdout.on('data', (data) => {
                    const msg = data.toString();
                    console.log('Data:', msg.trim());
                });
                
                mcProcess.stderr.on('data', (data) => {
                    console.log('Error:', data.toString().trim());
                });
                
                mcProcess.on('close', (code) => {
                    console.log('Close:', code);
                    if (consumeRestartSignal(instancePath)) {
                        console.log('Restart signal: riavvio del gioco in corso...');
                        if (mainWindow && !mainWindow.isDestroyed()) {
                            mainWindow.webContents.send('minecraft-restart-requested');
                        }
                    }
                    if (mainWindow && !mainWindow.isDestroyed()) {
                        mainWindow.webContents.send('minecraft-closed', { code });
                    }
                });
                
                mcProcess.on('error', (err) => {
                    console.error('Process error:', err);
                    if (mainWindow && !mainWindow.isDestroyed()) {
                        mainWindow.webContents.send('minecraft-closed', { code: -1, error: err.message });
                    }
                });
                
                return; // Don't use MCLC for Fabric
            }
            
            // Vanilla Minecraft - use MCLC
            console.log('\n=== LAUNCHING VANILLA ===');
            
            // Setup event listeners before launch
            launcher.on('debug', (e) => console.log('Debug:', e));
            launcher.on('data', (e) => console.log('Data:', e));
            launcher.on('error', (e) => console.log('Error:', e));
            launcher.on('close', (e) => {
                console.log('Close:', e);
                if (consumeRestartSignal(instancePath)) {
                    console.log('Restart signal: riavvio del gioco in corso...');
                    resolve({ success: true, restartRequested: true });
                    return;
                }
                resolve({ success: true, message: 'Minecraft avviato con successo!' });
            });
            launcher.on('download', (e) => console.log('Downloaded:', e));
            launcher.on('progress', (e) => {
                if (e.type !== 'assets' || e.task === e.total) {
                    console.log('Progress:', e);
                }
            });
            
            // Launch Minecraft
            const mc = await launcher.launch(opts);
            
            console.log('Minecraft process started:', mc ? 'yes' : 'no');
            
            if (!mc) {
                resolve({ success: false, error: 'Failed to start Minecraft process' });
            }
            
        } catch (error) {
            console.error('Launch error:', error);
            resolve({ success: false, error: error.message });
        }
    });
});

// Helper function to generate UUID from username
function generateUUID(username) {
    const crypto = require('crypto');
    const hash = crypto.createHash('md5').update(username).digest('hex');
    return `${hash.slice(0,8)}-${hash.slice(8,12)}-4${hash.slice(12,16)}-${hash.slice(16,20)}-${hash.slice(20,32)}`;
}

// Microsoft Authentication using msmc
ipcMain.handle('microsoft-login', async () => {
    console.log('Starting Microsoft login...');
    try {
        const { Auth } = require('msmc');
        console.log('MSMC loaded successfully');
        
        // Create a new Auth manager with 'select_account' prompt
        const authManager = new Auth('select_account');
        console.log('Auth manager created');
        
        // Launch using the 'electron' gui framework
        const xboxManager = await authManager.launch('electron');
        console.log('Xbox manager created:', xboxManager ? 'yes' : 'no');
        
        if (xboxManager) {
            // Try to get Minecraft token
            console.log('Getting Minecraft token...');
            const token = await xboxManager.getMinecraft();
            console.log('Token object type:', typeof token);
            console.log('Token has mclc:', typeof token?.mclc);
            console.log('Token has getToken:', typeof token?.getToken);
            
            if (token && typeof token.mclc === 'function') {
                // Use msmc's built-in serialization to get full token data
                // getToken(true) includes refresh token metadata
                const serializedToken = token.getToken(true);
                console.log('Serialized token keys:', Object.keys(serializedToken));
                console.log('Serialized token presente:', !!serializedToken?.mcToken);
                console.log('Serialized token profile:', JSON.stringify(serializedToken.profile));
                console.log('Serialized token has refresh:', !!serializedToken.refresh);
                
                // Also get the mclc format for immediate use
                const mclcToken = token.mclc(true); // true = include refresh metadata
                console.log('MCLC token keys:', Object.keys(mclcToken));
                
                return {
                    success: true,
                    account: {
                        name: serializedToken.profile.name,
                        uuid: serializedToken.profile.id,
                        type: 'microsoft',
                        // Save the full serialized token for later restoration
                        mcTokenSerialized: serializedToken,
                        // Save mclc format for direct use
                        mclcToken: mclcToken,
                        // Save profile for display
                        profile: serializedToken.profile
                    }
                };
            } else {
                console.error('Token object is invalid or missing mclc method');
                console.error('Token:', token);
            }
        }
        
        console.log('Login failed - no valid result');
        return { success: false, error: 'Login fallito' };
    } catch (error) {
        console.error('Microsoft login error:', error.message);
        console.error('Full error:', error);
        return { success: false, error: error.message };
    }
});

ipcMain.handle('microsoft-refresh', async (event, accountData) => {
    try {
        const { Auth, fromMclcToken } = require('msmc');
        
        console.log('Attempting Microsoft token refresh...');
        console.log('Account data keys:', Object.keys(accountData || {}));
        
        const authManager = new Auth('select_account');
        
        // Try to refresh using the saved mclc token with refresh metadata
        if (accountData && accountData.mclcToken && accountData.mclcToken.meta?.refresh) {
            console.log('Refresh token found, attempting refresh...');
            
            try {
                // Use fromMclcToken to restore the Minecraft token object
                const mcToken = await fromMclcToken(authManager, accountData.mclcToken, true);
                
                if (mcToken && typeof mcToken.mclc === 'function') {
                    // Successfully refreshed! Get new tokens
                    const newSerialized = mcToken.getToken(true);
                    const newMclc = mcToken.mclc(true);
                    
                    console.log('Token refreshed successfully!');
                    console.log('New profile:', newSerialized.profile?.name);
                    
                    return {
                        success: true,
                        account: {
                            name: newSerialized.profile.name,
                            uuid: newSerialized.profile.id,
                            type: 'microsoft',
                            mcTokenSerialized: newSerialized,
                            mclcToken: newMclc,
                            profile: newSerialized.profile
                        }
                    };
                }
            } catch (refreshError) {
                console.error('Token refresh failed:', refreshError.message);
                // Fall through to re-login
            }
        }
        
        // If refresh failed or no refresh token, need to re-login
        console.log('Cannot refresh, need to re-login');
        return { success: false, error: 'Token scaduto, effettua nuovamente il login' };
    } catch (error) {
        console.error('Microsoft refresh error:', error);
        return { success: false, error: error.message };
    }
});

// App lifecycle
app.whenReady().then(() => {
    ensureAppDataPath();
    createWindow();

    app.on('activate', () => {
        if (BrowserWindow.getAllWindows().length === 0) {
            createWindow();
        }
    });
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        app.quit();
    }
});
