'use strict';

const fs = require('fs');
const fsp = require('fs').promises;
const path = require('path');

const INJECTED_CLIENT_IDS = new Set(['feather']);
const PROTECTED_MOD_FILES = new Set(['roleplayclient.jar']);
const OPTION_FILES = ['options.txt', 'optionsof.txt', 'optionsshaders.txt'];

function readModId(jarPath) {
    try {
        const AdmZip = require('adm-zip');
        const zip = new AdmZip(jarPath);
        const entry = zip.getEntry('fabric.mod.json');
        if (entry) {
            const meta = JSON.parse(zip.readAsText(entry));
            if (meta && typeof meta.id === 'string') return meta.id.toLowerCase();
        }
    } catch {
        // ignore
    }
    return null;
}

function isFabricJar(jarPath) {
    return !!readModId(jarPath);
}

function isInjectedClient(modId) {
    return modId && INJECTED_CLIENT_IDS.has(modId.toLowerCase());
}

async function pathInside(root, candidate) {
    const resolvedRoot = path.resolve(root);
    const resolved = path.resolve(candidate);
    const rel = path.relative(resolvedRoot, resolved);
    return rel === '' || (!rel.startsWith('..') && !path.isAbsolute(rel));
}

async function safeStat(p) {
    try {
        return await fsp.lstat(p);
    } catch {
        return null;
    }
}

async function ensureDir(dir) {
    await fsp.mkdir(dir, { recursive: true });
}

async function backupThenCopyFile(src, dest) {
    await ensureDir(path.dirname(dest));
    if (fs.existsSync(dest)) {
        const bak = dest + '.bak';
        if (fs.existsSync(bak)) fs.unlinkSync(bak);
        await fsp.copyFile(dest, bak).catch(() => {});
    }
    await fsp.copyFile(src, dest);
}

async function copyDirMerge(srcDir, destDir, stats) {
    const st = await safeStat(srcDir);
    if (!st || !st.isDirectory()) return;
    if (st.isSymbolicLink()) return;

    await ensureDir(destDir);
    const entries = await fsp.readdir(srcDir, { withFileTypes: true });
    for (const e of entries) {
        const s = path.join(srcDir, e.name);
        const d = path.join(destDir, e.name);
        const lst = await safeStat(s);
        if (!lst) continue;
        if (lst.isSymbolicLink()) continue;
        if (lst.isDirectory()) {
            await copyDirMerge(s, d, stats);
        } else if (lst.isFile()) {
            if (fs.existsSync(d)) {
                await fsp.copyFile(d, d + '.bak').catch(() => {});
                stats.configOverwritten = (stats.configOverwritten || 0) + 1;
            } else {
                stats.configCopied = (stats.configCopied || 0) + 1;
            }
            await ensureDir(path.dirname(d));
            await fsp.copyFile(s, d);
        }
    }
}

async function copyPacks(srcDir, destDir, stats, key) {
    const st = await safeStat(srcDir);
    if (!st || !st.isDirectory()) return;
    await ensureDir(destDir);
    const entries = await fsp.readdir(srcDir, { withFileTypes: true });
    for (const e of entries) {
        const s = path.join(srcDir, e.name);
        const d = path.join(destDir, e.name);
        if (fs.existsSync(d)) {
            stats[key + 'Skipped'] = (stats[key + 'Skipped'] || 0) + 1;
            continue;
        }
        const lst = await safeStat(s);
        if (!lst || lst.isSymbolicLink()) continue;
        if (lst.isDirectory()) {
            await fsp.cp(s, d, { recursive: true, dereference: false });
        } else {
            await fsp.copyFile(s, d);
        }
        stats[key + 'Copied'] = (stats[key + 'Copied'] || 0) + 1;
    }
}

async function importMods(srcMods, destMods, bundledModsDir, stats) {
    await ensureDir(destMods);
    const existingIds = new Set();
    const existingNames = new Set();

    if (fs.existsSync(destMods)) {
        for (const f of await fsp.readdir(destMods)) {
            if (!f.toLowerCase().endsWith('.jar')) continue;
            existingNames.add(f.toLowerCase());
            const id = readModId(path.join(destMods, f));
            if (id) existingIds.add(id);
        }
    }
    if (bundledModsDir && fs.existsSync(bundledModsDir)) {
        for (const f of await fsp.readdir(bundledModsDir)) {
            if (!f.toLowerCase().endsWith('.jar')) continue;
            existingNames.add(f.toLowerCase());
            const id = readModId(path.join(bundledModsDir, f));
            if (id) existingIds.add(id);
        }
    }

    if (!fs.existsSync(srcMods)) return;
    for (const f of await fsp.readdir(srcMods)) {
        if (!f.toLowerCase().endsWith('.jar')) continue;
        const lower = f.toLowerCase();
        if (PROTECTED_MOD_FILES.has(lower) || lower.startsWith('fabric-api')) {
            stats.modsSkipped = (stats.modsSkipped || 0) + 1;
            continue;
        }
        const src = path.join(srcMods, f);
        const lst = await safeStat(src);
        if (!lst || !lst.isFile() || lst.isSymbolicLink()) continue;

        const modId = readModId(src);
        if (!modId) {
            stats.modsSkippedNonFabric = (stats.modsSkippedNonFabric || 0) + 1;
            continue;
        }
        if (isInjectedClient(modId)) {
            stats.modsSkipped = (stats.modsSkipped || 0) + 1;
            continue;
        }
        if (existingIds.has(modId) || existingNames.has(lower)) {
            stats.modsSkipped = (stats.modsSkipped || 0) + 1;
            continue;
        }

        await fsp.copyFile(src, path.join(destMods, f));
        existingIds.add(modId);
        existingNames.add(lower);
        stats.modsCopied = (stats.modsCopied || 0) + 1;
    }
}

/**
 * @param {string} srcGameDir
 * @param {string} destGameDir  instance root
 * @param {string[]} categories
 * @param {{ bundledModsDir?: string }} opts
 */
async function runImport(srcGameDir, destGameDir, categories, opts = {}) {
    const src = path.resolve(srcGameDir);
    const dest = path.resolve(destGameDir);
    if (!fs.existsSync(src) || !fs.statSync(src).isDirectory()) {
        throw new Error('Cartella sorgente non valida');
    }
    await ensureDir(dest);

    const cats = new Set(categories || []);
    const stats = {
        optionsCopied: 0,
        serversCopied: 0,
        modsCopied: 0,
        modsSkipped: 0,
        modsSkippedNonFabric: 0,
        configCopied: 0,
        configOverwritten: 0,
        resourcepacksCopied: 0,
        resourcepacksSkipped: 0,
        shaderpacksCopied: 0,
        shaderpacksSkipped: 0
    };

    if (cats.has('options')) {
        for (const name of OPTION_FILES) {
            const s = path.join(src, name);
            if (!fs.existsSync(s) || !fs.statSync(s).isFile()) continue;
            await backupThenCopyFile(s, path.join(dest, name));
            stats.optionsCopied += 1;
        }
    }

    if (cats.has('servers')) {
        const s = path.join(src, 'servers.dat');
        if (fs.existsSync(s) && fs.statSync(s).isFile()) {
            await backupThenCopyFile(s, path.join(dest, 'servers.dat'));
            stats.serversCopied = 1;
        }
    }

    if (cats.has('mods')) {
        await importMods(path.join(src, 'mods'), path.join(dest, 'mods'), opts.bundledModsDir, stats);
    }

    if (cats.has('config')) {
        await copyDirMerge(path.join(src, 'config'), path.join(dest, 'config'), stats);
    }

    if (cats.has('resourcepacks')) {
        await copyPacks(path.join(src, 'resourcepacks'), path.join(dest, 'resourcepacks'), stats, 'resourcepacks');
    }

    if (cats.has('shaderpacks')) {
        await copyPacks(path.join(src, 'shaderpacks'), path.join(dest, 'shaderpacks'), stats, 'shaderpacks');
    }

    return stats;
}

module.exports = {
    runImport,
    readModId,
    isFabricJar,
    OPTION_FILES
};
