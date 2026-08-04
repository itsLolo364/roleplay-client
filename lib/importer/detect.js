'use strict';

const fs = require('fs');
const path = require('path');
const os = require('os');

/**
 * Detection sorgenti import (parity CharlieLauncher + config/).
 */

// Radice dei dati dei launcher per piattaforma. Su Linux la maggior parte dei
// launcher (e lo stesso .minecraft) vive direttamente in $HOME.
function appData(...parts) {
    const home = os.homedir();
    if (process.platform === 'win32') return path.join(home, 'AppData', 'Roaming', ...parts);
    if (process.platform === 'darwin') return path.join(home, 'Library', 'Application Support', ...parts);
    return path.join(home, ...parts);
}

function home(...parts) {
    return path.join(os.homedir(), ...parts);
}

function existsDir(p) {
    try {
        return p && fs.existsSync(p) && fs.statSync(p).isDirectory();
    } catch {
        return false;
    }
}

function listSubdirs(dir) {
    if (!existsDir(dir)) return [];
    try {
        return fs.readdirSync(dir, { withFileTypes: true })
            .filter(d => d.isDirectory())
            .map(d => d.name);
    } catch {
        return [];
    }
}

function countEntries(dir, predicate) {
    if (!existsDir(dir)) return 0;
    try {
        return fs.readdirSync(dir).filter(predicate).length;
    } catch {
        return 0;
    }
}

function countConfigFiles(configDir) {
    if (!existsDir(configDir)) return 0;
    let n = 0;
    const walk = (d) => {
        let entries;
        try { entries = fs.readdirSync(d, { withFileTypes: true }); } catch { return; }
        for (const e of entries) {
            const p = path.join(d, e.name);
            if (e.isDirectory()) walk(p);
            else n += 1;
        }
    };
    walk(configDir);
    return n;
}

function readJsonSafe(file) {
    try {
        return JSON.parse(fs.readFileSync(file, 'utf8'));
    } catch {
        return null;
    }
}

function mmcVersions(instanceDir) {
    const pack = readJsonSafe(path.join(instanceDir, 'mmc-pack.json'));
    let mcVersion = null;
    let fabricLoaderVersion = null;
    if (pack && Array.isArray(pack.components)) {
        for (const c of pack.components) {
            if (c.uid === 'net.minecraft') mcVersion = c.version || mcVersion;
            if (c.uid === 'net.fabricmc.fabric-loader') fabricLoaderVersion = c.version || fabricLoaderVersion;
        }
    }
    return { mcVersion, fabricLoaderVersion };
}

function resolveMmcGameDir(instanceDir) {
    const a = path.join(instanceDir, '.minecraft');
    const b = path.join(instanceDir, 'minecraft');
    if (existsDir(a)) return a;
    if (existsDir(b)) return b;
    return instanceDir;
}

/**
 * @returns {null|{launcher, profileName, gameDir, hasOptions, hasServers, hasConfig, modCount, resourcePackCount, shaderPackCount, configFileCount, mcVersion, fabricLoaderVersion}}
 */
function analyzeGameDir(gameDir, launcher, profileName, meta = {}) {
    if (!existsDir(gameDir)) return null;

    const optionsPath = path.join(gameDir, 'options.txt');
    const serversPath = path.join(gameDir, 'servers.dat');
    const modsDir = path.join(gameDir, 'mods');
    const rpDir = path.join(gameDir, 'resourcepacks');
    const spDir = path.join(gameDir, 'shaderpacks');
    const configDir = path.join(gameDir, 'config');

    const hasOptions = fs.existsSync(optionsPath);
    const hasServers = fs.existsSync(serversPath);
    const modCount = countEntries(modsDir, f => f.toLowerCase().endsWith('.jar'));
    const resourcePackCount = countEntries(rpDir, () => true);
    const shaderPackCount = countEntries(spDir, () => true);
    const configFileCount = countConfigFiles(configDir);
    const hasConfig = configFileCount > 0;

    if (!hasOptions && !hasServers && modCount === 0 && resourcePackCount === 0 && shaderPackCount === 0 && !hasConfig) {
        return null;
    }

    return {
        launcher: launcher || 'Cartella',
        profileName: profileName || path.basename(gameDir),
        gameDir: path.resolve(gameDir),
        hasOptions,
        hasServers,
        hasConfig,
        modCount,
        resourcePackCount,
        shaderPackCount,
        configFileCount,
        mcVersion: meta.mcVersion || null,
        fabricLoaderVersion: meta.fabricLoaderVersion || null
    };
}

function vanillaLabel() {
    const labels = ['Minecraft Launcher'];
    if (existsDir(appData('.feather')) || existsDir(home('.feather'))) labels.push('Feather');
    if (existsDir(home('.lunarclient'))) labels.push('Lunar');
    if (existsDir(appData('Badlion Client')) || existsDir(home('.badlion'))) labels.push('Badlion');
    if (existsDir(home('.tlauncher')) || existsDir(appData('.tlauncher'))) labels.push('TLauncher');
    if (existsDir(appData('dawn-craft')) || existsDir(home('.dawn'))) labels.push('Dawn');
    return labels.join(' / ');
}

function detectImportSources() {
    const out = [];
    const seen = new Set();

    const push = (candidate) => {
        if (!candidate) return;
        const key = candidate.gameDir.toLowerCase();
        if (seen.has(key)) return;
        seen.add(key);
        out.push(candidate);
    };

    // Vanilla .minecraft
    const userHome = os.homedir();
    let mc;
    if (process.platform === 'darwin') {
        mc = path.join(userHome, 'Library', 'Application Support', 'minecraft');
    } else if (process.platform === 'win32') {
        mc = path.join(userHome, 'AppData', 'Roaming', '.minecraft');
    } else {
        mc = path.join(userHome, '.minecraft');
    }
    push(analyzeGameDir(mc, vanillaLabel(), '.minecraft'));

    // Prism / PolyMC
    for (const launcher of ['PrismLauncher', 'PolyMC']) {
        const root = appData(launcher, 'instances');
        for (const id of listSubdirs(root)) {
            const inst = path.join(root, id);
            const gameDir = resolveMmcGameDir(inst);
            const meta = mmcVersions(inst);
            push(analyzeGameDir(gameDir, launcher === 'PrismLauncher' ? 'Prism Launcher' : 'PolyMC', id, meta));
        }
    }

    // GDLauncher
    const gdRoot = appData('gdlauncher_next', 'instances');
    for (const id of listSubdirs(gdRoot)) {
        const inst = path.join(gdRoot, id);
        const cfg = readJsonSafe(path.join(inst, 'config.json')) || {};
        const loader = cfg.loader || {};
        push(analyzeGameDir(inst, 'GDLauncher', id, {
            mcVersion: loader.mcVersion || cfg.mcVersion || null,
            fabricLoaderVersion: loader.loaderVersion || null
        }));
    }

    // Modrinth App
    for (const rootName of ['ModrinthApp', 'com.modrinth.theseus']) {
        const root = appData(rootName, 'profiles');
        for (const id of listSubdirs(root)) {
            const inst = path.join(root, id);
            const profile = readJsonSafe(path.join(inst, 'profile.json')) || {};
            const metadata = profile.metadata || profile;
            push(analyzeGameDir(inst, 'Modrinth App', profile.name || id, {
                mcVersion: metadata.game_version || metadata.gameVersion || null,
                fabricLoaderVersion: (metadata.loader === 'fabric' ? (metadata.loader_version || metadata.loaderVersion) : null) || null
            }));
        }
    }

    // CurseForge
    const cfRoots = [
        home('curseforge', 'minecraft', 'Instances'),
        home('Documents', 'curseforge', 'minecraft', 'Instances')
    ];
    for (const root of cfRoots) {
        for (const id of listSubdirs(root)) {
            const inst = path.join(root, id);
            const mi = readJsonSafe(path.join(inst, 'minecraftinstance.json')) || {};
            const base = mi.baseModLoader || {};
            push(analyzeGameDir(inst, 'CurseForge', mi.name || id, {
                mcVersion: base.minecraftVersion || mi.gameVersion || null,
                fabricLoaderVersion: (String(base.name || '').toLowerCase().includes('fabric') ? base.forgeVersion : null) || null
            }));
        }
    }

    return out;
}

module.exports = {
    detectImportSources,
    analyzeGameDir,
    appData,
    home
};
