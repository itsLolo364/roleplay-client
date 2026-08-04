#!/usr/bin/env node
'use strict';

/*
 * Publish Roleplay Client — commit + tag + release GitHub.
 *
 * Chiede in sequenza le informazioni (messaggio del commit, versione della
 * release, file con il contenuto delle note, ...) e poi esegue automaticamente:
 * bump versioni, build mod, commit, tag, push e creazione della release.
 *
 * Uso:  npm run publish
 */

const { execSync } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');
const readline = require('readline');

const ROOT = path.resolve(__dirname, '..');
const MOD_DIR = path.join(ROOT, 'loloclient-mod');

const RESET = '\x1b[0m';
const BOLD = '\x1b[1m';
const DIM = '\x1b[2m';
const CYAN = '\x1b[36m';
const GREEN = '\x1b[32m';
const RED = '\x1b[31m';

const rl = readline.createInterface({ input: process.stdin, output: process.stdout });

/* ---------- Helpers di input ---------- */
/* Nota: si consumano gli eventi 'line' con una coda invece di rl.question():
   funziona sia in TTY interattivo sia con input reindirizzato (pipe). */

const lineQueue = [];
let lineWaiter = null;

rl.on('line', (line) => {
    if (lineWaiter) {
        const waiter = lineWaiter;
        lineWaiter = null;
        waiter(line);
    } else {
        lineQueue.push(line);
    }
});

function nextLine() {
    return new Promise((resolve) => {
        if (lineQueue.length > 0) resolve(lineQueue.shift());
        else lineWaiter = resolve;
    });
}

async function ask(prompt, { def, required } = {}) {
    for (;;) {
        const suffix = def !== undefined ? ` ${DIM}[${def}]${RESET}` : '';
        process.stdout.write(`${CYAN}?${RESET} ${prompt}${suffix}: `);
        const answer = (await nextLine()).trim();
        let value = answer === '' && def !== undefined ? def : answer;
        if (value === '' && required) {
            console.log(`${RED}Campo obbligatorio.${RESET}`);
            continue;
        }
        return value;
    }
}

async function askYesNo(prompt, def = false) {
    const suffix = def ? ' (Y/n)' : ' (y/N)';
    process.stdout.write(`${CYAN}?${RESET} ${prompt}${suffix}: `);
    const value = (await nextLine()).trim().toLowerCase();
    if (value === 'y' || value === 'yes') return true;
    if (value === 'n' || value === 'no') return false;
    return def;
}

/* ---------- Helpers di sistema ---------- */

function sh(cmd, opts = {}) {
    console.log(`${DIM}$ ${cmd}${RESET}`);
    return execSync(cmd, { stdio: 'inherit', cwd: opts.cwd || ROOT });
}

function shOut(cmd, opts = {}) {
    try {
        return execSync(cmd, { stdio: ['ignore', 'pipe', 'ignore'], cwd: opts.cwd || ROOT, encoding: 'utf8' }).toString().trim();
    } catch {
        return '';
    }
}

/* ---------- Versioni ---------- */

function lastTagVersion() {
    const tags = shOut('git tag --sort=-v:refname').split('\n').filter(Boolean);
    for (const tag of tags) {
        const match = tag.match(/^v?(\d+)\.(\d+)\.(\d+)/);
        if (match) return { version: `${match[1]}.${match[2]}.${match[3]}`, tag };
    }
    return null;
}

function bumpPatch(version) {
    const match = version.match(/^(\d+)\.(\d+)\.(\d+)$/);
    if (!match) return version;
    return `${match[1]}.${match[2]}.${Number(match[3]) + 1}`;
}

function packageVersion() {
    try {
        return JSON.parse(fs.readFileSync(path.join(ROOT, 'package.json'), 'utf8')).version;
    } catch {
        return '1.0.0';
    }
}

function bumpPackageJson(version) {
    const file = path.join(ROOT, 'package.json');
    const data = JSON.parse(fs.readFileSync(file, 'utf8'));
    data.version = version;
    fs.writeFileSync(file, JSON.stringify(data, null, 2) + '\n');
    console.log(`  package.json -> ${version}`);
}

function bumpModVersion(version) {
    const file = path.join(MOD_DIR, 'gradle.properties');
    let content = fs.readFileSync(file, 'utf8');
    if (/^mod_version=.*$/m.test(content)) {
        content = content.replace(/^mod_version=.*$/m, `mod_version=${version}`);
    } else {
        content += `\nmod_version=${version}\n`;
    }
    fs.writeFileSync(file, content);
    console.log(`  gradle.properties mod_version -> ${version}`);
}

/* ---------- Build mod ---------- */

function findGradle() {
    const candidates = [
        'gradle',
        'gradle.exe',
        path.join(os.homedir(), 'gradle', 'bin', 'gradle'),
        '/mnt/c/gradle-9.2.1/bin/gradle',
        '/mnt/c/ProgramData/chocolatey/bin/gradle.exe'
    ];
    for (const candidate of candidates) {
        try {
            execSync(`${candidate} --version`, { stdio: 'ignore' });
            return candidate;
        } catch {
            // prova il successivo
        }
    }
    return null;
}

function buildMod() {
    const gradle = findGradle();
    if (!gradle) {
        console.log(`${RED}!${RESET} gradle non trovato: salto la build della mod.`);
        return false;
    }
    console.log(`${BOLD}→${RESET} Build della mod (${gradle})...`);
    sh(`${gradle} build -x test`, { cwd: MOD_DIR });
    const libsDir = path.join(MOD_DIR, 'build', 'libs');
    const jars = fs.readdirSync(libsDir)
        .filter((f) => /^roleplayclient-.+\.jar$/.test(f) && !f.endsWith('-sources.jar'))
        .sort();
    if (jars.length === 0) {
        console.log(`${RED}!${RESET} JAR della mod non trovato in build/libs.`);
        return false;
    }
    const jar = jars[jars.length - 1];
    fs.copyFileSync(path.join(libsDir, jar), path.join(ROOT, 'assets', 'mods', 'roleplayclient.jar'));
    console.log(`  JAR aggiornato: ${jar} -> assets/mods/roleplayclient.jar`);
    return true;
}

/* ---------- Build asset multi-piattaforma ---------- */

function powershellCommand() {
    return process.platform === 'win32'
        ? 'powershell -NoProfile -ExecutionPolicy Bypass'
        : 'powershell.exe -NoProfile -ExecutionPolicy Bypass';
}

function globDist(re) {
    try {
        return fs.readdirSync(path.join(ROOT, 'dist'))
            .filter((f) => re.test(f))
            .map((f) => path.join('dist', f));
    } catch {
        return [];
    }
}

function buildLinuxAssets() {
    console.log(`${BOLD}→${RESET} Build asset Linux (.deb + AppImage)...`);
    sh('bash scripts/package-linux.sh --skip-mod');
    return globDist(/\.(deb|AppImage)$/);
}

function buildWindowsAssets() {
    const ps = powershellCommand();
    console.log(`${BOLD}→${RESET} Build asset Windows (installer NSIS)...`);
    sh(`${ps} -File scripts/package-installer.ps1 -SkipMod -SkipBlockers`);
    return globDist(/Setup-.*\.exe$/);
}

/**
 * Builda gli asset per Linux e Windows. Ogni piattaforma è best-effort:
 * se i tool non sono disponibili (o la build fallisce) viene loggato un
 * warning e si prosegue con l'altra piattaforma.
 */
function buildPlatformAssets() {
    const artifacts = { linux: [], windows: [] };
    try {
        artifacts.linux = buildLinuxAssets();
    } catch (err) {
        console.log(`${RED}✗${RESET} Build Linux fallita: ${(err && err.message) || err}`);
    }
    try {
        artifacts.windows = buildWindowsAssets();
    } catch (err) {
        console.log(`${RED}✗${RESET} Build Windows fallita: ${(err && err.message) || err}`);
    }
    return artifacts;
}

/* ---------- Flusso principale ---------- */

async function main() {
    console.log(`\n${BOLD}Publish Roleplay Client${RESET}`);
    console.log(`${DIM}Commit + tag + release GitHub. Interrompi con Ctrl+C in qualsiasi momento.${RESET}\n`);

    const branch = shOut('git rev-parse --abbrev-ref HEAD') || 'main';
    const last = lastTagVersion();
    const defaultVersion = bumpPatch((last && last.version) || packageVersion());

    const commitMsg = await ask('Messaggio del commit', { required: true });
    const version = await ask('Versione della release', { def: defaultVersion });
    const tag = await ask('Tag git', { def: 'v' + version.replace(/^v/, '') });

    const bump = await askYesNo('Aggiorno le versioni (package.json, gradle.properties mod_version)?', true);
    const notesFile = await ask('File con il contenuto della release (release notes)', { def: 'RELEASE_NOTES.md' });
    if (!fs.existsSync(path.join(ROOT, notesFile))) {
        console.log(`${RED}!${RESET} File non trovato: ${notesFile}`);
        rl.close();
        process.exit(1);
    }
    const build = await askYesNo('Rigenero la mod (gradle build) e aggiorno assets/mods/roleplayclient.jar?', true);
    const buildAssets = await askYesNo('Buildo automaticamente gli asset Linux e Windows (dist/)?', true);

    const push = await askYesNo(`Push su origin/${branch}?`, false);
    const ghAvailable = shOut('gh --version') !== '';
    const release = ghAvailable && (await askYesNo('Creo la release su GitHub?', false));
    let manualAssets = '';
    let attachBuilt = false;
    if (release && buildAssets) {
        attachBuilt = await askYesNo('Allego alla release gli asset appena generati?', true);
        if (!attachBuilt) {
            manualAssets = await ask('Asset da allegare alla release (percorsi separati da spazio, vuoto = nessuno)');
        }
    } else if (release) {
        manualAssets = await ask('Asset da allegare alla release (percorsi separati da spazio, vuoto = nessuno)');
    }

    /* Riepilogo */
    console.log(`\n${DIM}— Riepilogo —${RESET}`);
    console.log(`  Commit     : ${commitMsg}`);
    console.log(`  Versione   : ${version}`);
    console.log(`  Tag        : ${tag}`);
    console.log(`  Bump vers. : ${bump ? 'sì' : 'no'}`);
    console.log(`  Note       : ${notesFile}`);
    console.log(`  Build mod  : ${build ? 'sì' : 'no'}`);
    console.log(`  Build asset: ${buildAssets ? 'Linux + Windows (dist/)' : 'no'}`);
    console.log(`  Push       : ${push ? `origin/${branch}` : 'no'}`);
    if (ghAvailable) {
        console.log(`  Release GH : ${release ? 'sì' : 'no'}${release ? (attachBuilt ? '  (asset: generati)' : (manualAssets ? `  (asset: ${manualAssets})` : '')) : ''}`);
    } else {
        console.log(`  Release GH : ${RED}gh non trovato${RESET} (salta)`);
    }

    const proceed = await askYesNo('\nProcedo con il publish?', false);
    if (!proceed) {
        console.log('Annullato.');
        rl.close();
        process.exit(0);
    }

    const tmpMsg = path.join(os.tmpdir(), `rc-publish-${process.pid}.txt`);
    let builtArtifacts = [];
    try {
        if (bump) {
            console.log(`${BOLD}→${RESET} Aggiorno le versioni...`);
            bumpPackageJson(version);
            bumpModVersion(version);
        }

        if (build) {
            console.log(`${BOLD}→${RESET} Build della mod...`);
            buildMod();
        }

        if (buildAssets) {
            console.log(`${BOLD}→${RESET} Build degli asset per Linux e Windows...`);
            const results = buildPlatformAssets();
            builtArtifacts = [...results.linux, ...results.windows];
            if (builtArtifacts.length > 0) {
                console.log(`${DIM}  Asset generati:${RESET}`);
                builtArtifacts.forEach((a) => console.log(`    - ${a}`));
            } else {
                console.log(`${RED}!${RESET} Nessun asset generato in dist/.`);
            }
        }

        const status = shOut('git status --porcelain');
        if (status) {
            console.log(`${BOLD}→${RESET} Commit...`);
            fs.writeFileSync(tmpMsg, commitMsg + '\n');
            sh('git add -A');
            sh(`git commit -F ${JSON.stringify(tmpMsg)}`);
        } else {
            console.log(`${DIM}Nessuna modifica da committare.${RESET}`);
        }

        if (shOut(`git tag --list ${JSON.stringify(tag)}`)) {
            console.log(`${DIM}Tag ${tag} già presente: salto la creazione.${RESET}`);
        } else {
            console.log(`${BOLD}→${RESET} Creo il tag ${tag}...`);
            sh(`git tag -a ${tag} -F ${JSON.stringify(tmpMsg)}`);
        }

        if (push) {
            console.log(`${BOLD}→${RESET} Push...`);
            sh(`git push origin ${branch}`);
            sh(`git push origin ${tag}`);
        }

        if (release) {
            console.log(`${BOLD}→${RESET} Release su GitHub...`);
            const assetsArg = attachBuilt ? builtArtifacts.join(' ') : manualAssets;
            const cmd = ['gh', 'release', 'create', tag,
                '--title', JSON.stringify(`Release ${version}`),
                '--notes-file', JSON.stringify(notesFile)];
            assetsArg.split(/\s+/).filter(Boolean).forEach((a) => cmd.push(a));
            sh(cmd.join(' '));
        } else if (ghAvailable) {
            console.log(`${DIM}Nessuna release GitHub creata.${RESET}`);
        } else {
            console.log(`${DIM}gh non disponibile: crea la release a mano con:${RESET}`);
            console.log(`${DIM}  gh release create ${tag} --title "Release ${version}" --notes-file ${notesFile}${RESET}`);
        }

        console.log(`\n${GREEN}✓ Publish completato.${RESET}`);
    } catch (err) {
        console.log(`\n${RED}✗ Publish fallito: ${(err && err.message) || err}${RESET}`);
        process.exitCode = 1;
    } finally {
        try { fs.unlinkSync(tmpMsg); } catch { /* non critico */ }
        rl.close();
    }
}

main();
